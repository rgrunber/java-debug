/*******************************************************************************
* Copyright (c) 2022 Microsoft Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Gayan Perera (gayanper@gmail.com) - initial API and implementation
*******************************************************************************/
package com.microsoft.java.debug;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;

public class LambdaExpressionLocator extends ASTVisitor {
    private CompilationUnit compilationUnit;
    private int line;
    private int column;
    private boolean found;

    private IMethodBinding lambdaMethodBinding;
    private LambdaExpression lambdaExpression;

    public LambdaExpressionLocator(CompilationUnit compilationUnit, int line, int column) {
        this.compilationUnit = compilationUnit;
        this.line = line;
        this.column = column;
    }

    @Override
    public boolean visit(LambdaExpression node) {
        if (column > -1) {
            int startPosition = node.getStartPosition();
            int breakOffset = this.compilationUnit.getPosition(line, column);
            // lambda on same line:
            // list.stream().map(i -> i + 1);
            //
            // lambda on multiple lines:
            // list.stream().map(user
            // -> user.isSystem() ? new SystemUser(user) : new EndUser(user));

            // Since the debugger supports BreakpointLocations Request to hint user
            // about possible inline breakpoint locations, we will only support
            // inline breakpoints added where a lambda expression begins.
            if (breakOffset == startPosition) {
                this.lambdaMethodBinding = node.resolveMethodBinding();
                this.found = true;
                this.lambdaExpression = node;
                return false;
            }
        }
        return super.visit(node);
    }

    /**
     * Returns <code>true</code> if a lambda is found at given location.
     */
    public boolean isFound() {
        return found;
    }

    /**
     * Returns the signature of lambda method otherwise return null.
     */
    public String getMethodSignature() {
        if (!this.found) {
            return null;
        }
        return BindingUtils.toSignature(this.lambdaMethodBinding, getMethodName());
    }

    /**
     * Returns the name of lambda method otherwise return null.
     */
    public String getMethodName() {
        if (!this.found) {
            return null;
        }
        return BindingUtils.getMethodName(lambdaMethodBinding, true);
    }

    /**
     * Returns the name of the type which the lambda method is found.
     */
    public String getFullyQualifiedTypeName() {
        if (this.found) {
            ASTNode parent = lambdaExpression.getParent();
            while (parent != null) {
                if (parent instanceof AbstractTypeDeclaration) {
                    AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) parent;
                    return declaration.resolveBinding().getBinaryName();
                }
                parent = parent.getParent();
            }
        }
        return null;
    }
}
