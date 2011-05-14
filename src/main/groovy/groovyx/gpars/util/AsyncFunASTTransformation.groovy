// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.util

import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.GroovyBugError
import static groovyx.gpars.util.ASTUtils.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import groovyx.gpars.GParsPoolUtil
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.ClassHelper
import groovyx.gpars.AsyncFun

/**
 *
 * This transformation turns field initialExpressions into method calls to {@link groovyx.gpars.GParsPoolUtil#asyncFun(groovy.lang.Closure, boolean)}.
 *
 * @see groovyx.gpars.GParsPoolUtil
 * @author Vladimir Orany
 * @author Hamlet D'Arcy
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class AsyncFunASTTransformation implements ASTTransformation {
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {

        if (nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            addError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(nodes));
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode node = (AnnotationNode) nodes[0];

        if (parent instanceof FieldNode) {


            final Expression classExpression
            if (node.members.value instanceof ClassExpression) {
                classExpression = node.members.value
            } else {
                classExpression = new ClassExpression(ClassHelper.make(GParsPoolUtil))
            }

            Expression initExpression = parent.initialValueExpression
            MethodCallExpression newInitExpression = new MethodCallExpression(
                    classExpression,
                    new ConstantExpression('asyncFun'),
                    new ArgumentListExpression(initExpression))
            parent.initialValueExpression = newInitExpression
        }
    }
}
