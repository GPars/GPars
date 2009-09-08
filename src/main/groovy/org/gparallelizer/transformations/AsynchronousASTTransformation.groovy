//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.transformations

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement

@org.codehaus.groovy.transform.GroovyASTTransformation (phase = org.codehaus.groovy.control.CompilePhase.CANONICALIZATION)
public class AsynchronousASTTransformation implements org.codehaus.groovy.transform.ASTTransformation {

    //, org.objectweb.asm.Opcodes 

    public AsynchronousASTTransformation() { }

    public void visit(final org.codehaus.groovy.ast.ASTNode[] astNodes, final org.codehaus.groovy.control.SourceUnit sourceUnit) {
        MethodNode method = astNodes[-1] as MethodNode  //todo make sure this refers to the correct node
        Statement startMessage = createClosureAst("Starting $method.name")
        Statement endMessage = createClosureAst("Ending $method.name")

        List existingStatements = method.code.statements
        existingStatements.add(0, startMessage)
        existingStatements.add(endMessage)
        //todo wrap in block, pass to an external method
        //todo add threading
        //todo ensure exceptions are properly propagated to the Future.get() method
    }

    private Statement createClosureAst(String message) {
        return new ExpressionStatement(
                new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                                new ConstantExpression(message)
                        )
                )
        )
    }

    private Statement createClosureAst2(List<Statement> methodBody) {
//        def builder = new AstBuilder()
//        List<ASTNode> statements = builder.buildAST {
//            phase = CONVERSION
//            returnScriptBodyOnly = true
//            source = """ println "Hello World" """
//        }
    }

}

//new VariableExpression("this"),
//new ConstantExpression("println"),
//new ArgumentListExpression(
//    new ConstantExpression(message)
//)
