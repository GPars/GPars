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

import groovyx.gpars.GParsPoolUtil
import org.codehaus.groovy.GroovyBugError
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import groovyx.gpars.AsyncFun
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.MethodNode
import static groovyx.gpars.util.ASTUtils.addError

/**
 *
 * This transformation turns field initialExpressions into method calls to {@link groovyx.gpars.GParsPoolUtil#asyncFun(groovy.lang.Closure, boolean)}.
 *
 * @see groovyx.gpars.GParsPoolUtil
 * @author Vladimir Orany
 * @author Hamlet D'Arcy
 * @author Dinko Srkoč
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class AsyncFunASTTransformation implements ASTTransformation {
    private static final MY_TYPE = ClassHelper.make(AsyncFun)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes)

        AnnotatedNode fieldNode = (AnnotatedNode) nodes[1]
        AnnotationNode annotation = (AnnotationNode) nodes[0]
        if (!MY_TYPE == annotation.classNode || !(fieldNode in FieldNode)) return

        Expression classExpression
        if (annotation.members.value instanceof ClassExpression) {
            classExpression = annotation.members.value
        } else {
            classExpression = new ClassExpression(ClassHelper.make(GParsPoolUtil))
        }

        validatePoolClass(classExpression, fieldNode, source)
        Expression initExpression = fieldNode.initialValueExpression
        def blocking = new ConstantExpression(memberHasValue(annotation, 'blocking', true))

        Expression newInitExpression = new StaticMethodCallExpression(
                classExpression.type,
                'asyncFun',
                new ArgumentListExpression(initExpression, blocking))
        fieldNode.initialValueExpression = newInitExpression
    }

    private validatePoolClass(Expression classExpression, AnnotatedNode fieldNode, SourceUnit source) {
        Parameter[] parameters = [new Parameter(ClassHelper.CLOSURE_TYPE, 'a1'),
                new Parameter(ClassHelper.boolean_TYPE, 'a2')]
        MethodNode asyncFunMethod = classExpression.type.getMethod('asyncFun', parameters)
        if (!asyncFunMethod || !asyncFunMethod.isStatic())
            addError("Supplied pool class has no static asyncFun(Closure, boolean) method", fieldNode, source)
    }

    private init(ASTNode[] nodes) {
        if (nodes.length != 2 || !(nodes[0] in AnnotationNode) || !(nodes[1] in AnnotatedNode)) {
            throw new GroovyBugError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(nodes))
        }
    }

    private memberHasValue(AnnotationNode node, String name, Object value) {
        Expression member = node.getMember(name)
        member && member in ConstantExpression && member.value == value
    }
}
