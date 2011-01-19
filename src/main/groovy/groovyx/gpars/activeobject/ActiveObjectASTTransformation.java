// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.activeobject;

import groovyjarjarasm.asm.Opcodes;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GroovyClassVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vaclav Pech
 *         <p/>
 *         Inspired by org.codehaus.groovy.transform.LogASTTransformation
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ActiveObjectASTTransformation implements ASTTransformation {
    @Override
    public void visit(final ASTNode[] nodes, final SourceUnit source) {
        if (nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            addError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(nodes), nodes[0], source);
        }

        final AnnotatedNode targetClass = (AnnotatedNode) nodes[1];
        final AnnotationNode activeObjectAnnotation = (AnnotationNode) nodes[0];

        final String actorFieldName = lookupActorFieldName(activeObjectAnnotation);

        if (!(targetClass instanceof ClassNode))
            throw new GroovyBugError("Class annotation " + activeObjectAnnotation.getClassNode().getName() + " annotated no Class, this must not happen.");

        final ClassNode classNode = (ClassNode) targetClass;

        final GroovyClassVisitor transformer = new MyClassCodeExpressionTransformer(source, actorFieldName);
        transformer.visitClass(classNode);
    }

    private static String lookupActorFieldName(final AnnotationNode logAnnotation) {
        final Expression member = logAnnotation.getMember("value");
        if (member != null && member.getText() != null) {
            return member.getText();
        } else {
            return ActiveObject.INTERNAL_ACTIVE_OBJECT_ACTOR;
        }
    }

    public static void addError(final String msg, final ASTNode expr, final SourceUnit source) {
        final int line = expr.getLineNumber();
        final int col = expr.getColumnNumber();
        source.getErrorCollector().addErrorAndContinue(
                new SyntaxErrorMessage(new SyntaxException(msg + '\n', line, col), source)
        );
    }

    @SuppressWarnings({"StringToUpperCaseOrToLowerCaseWithoutLocale", "CallToStringEquals"})
    private static class MyClassCodeExpressionTransformer extends ClassCodeExpressionTransformer {
        private FieldNode actorNode;
        private final SourceUnit source;
        private final String actorFieldName;

        private MyClassCodeExpressionTransformer(final SourceUnit source, final String actorFieldName) {
            this.source = source;
            this.actorFieldName = actorFieldName;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return source;
        }

        @Override
        public Expression transform(final Expression exp) {
            if (exp == null) return null;
            return super.transform(exp);
        }

        @Override
        public void visitClass(final ClassNode node) {

            final FieldNode actorField = node.getField(actorFieldName);
            if (actorField != null) {
                if (actorField.getType().getName().contains("groovyx.gpars.activeobject.InternalActor")) {
                    actorNode = actorField;
                } else
                    this.addError("Active Object cannot have a field named " + actorFieldName + " declared", actorField);
            } else {
                actorNode = addActorFieldToClass(node, actorFieldName);
            }

            final Iterable<MethodNode> copyOfMethods = new ArrayList<MethodNode>(node.getMethods());
            for (final MethodNode method : copyOfMethods) {
                final List<AnnotationNode> annotations = method.getAnnotations(new ClassNode(ActiveMethod.class));
                if (annotations.isEmpty()) continue;
                if (method.isStatic()) this.addError("Static methods cannot be active", method);

                addActiveMethod(actorNode, node, method);
            }
            super.visitClass(node);
        }

        private void addActiveMethod(final FieldNode actorNode, final ClassNode owner, final MethodNode original) {
            if ((original.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0) return;

            final ArgumentListExpression args = new ArgumentListExpression();
            final Parameter[] params = original.getParameters();
            final Parameter[] newParams = new Parameter[params.length];

            args.addExpression(new VariableExpression("this"));
            args.addExpression(new ConstantExpression(original.getName()));

            for (int i = 0; i < newParams.length; i++) {
                final Parameter newParam = new Parameter(nonGeneric(params[i].getType()), params[i].getName());
                newParam.setInitialExpression(params[i].getInitialExpression());
                newParams[i] = newParam;
                args.addExpression(new VariableExpression(newParam));
            }
            final MethodNode newMethod = owner.addMethod(InternalActor.METHOD_NAME_PREFIX + original.getName(),
                    Opcodes.ACC_FINAL & Opcodes.ACC_PRIVATE,
                    nonGeneric(original.getReturnType()),
                    newParams,
                    original.getExceptions(),
                    original.getCode());
            newMethod.setGenericsTypes(original.getGenericsTypes());

            final String submitMethodName = original.getReturnType().isDerivedFrom(new ClassNode(Void.class)) ? "submit" : "submitAndWait";
            original.setCode(new ExpressionStatement(
                    new MethodCallExpression(
                            new VariableExpression(actorNode), submitMethodName, args)
            ));
        }

        private static ClassNode nonGeneric(final ClassNode type) {
            if (type.isUsingGenerics()) {
                final ClassNode nonGen = ClassHelper.makeWithoutCaching(type.getName());
                nonGen.setRedirect(type);
                nonGen.setGenericsTypes(null);
                nonGen.setUsingGenerics(false);
                return nonGen;
            } else {
                return type;
            }
        }

        private static FieldNode addActorFieldToClass(final ClassNode classNode, final String logFieldName) {
            final ArgumentListExpression args = new ArgumentListExpression();
            args.addExpression(new ConstantExpression(""));

            return classNode.addField(logFieldName,
                    Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT | Opcodes.ACC_PROTECTED,
                    new ClassNode(InternalActor.class),
                    new MethodCallExpression(
                            new ClassExpression(new ClassNode(InternalActor.class)),
                            "create",
                            args));
        }
    }
}


