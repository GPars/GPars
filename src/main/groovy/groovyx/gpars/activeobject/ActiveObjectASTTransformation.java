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
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GroovyClassVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
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

        final String actorFieldName = lookupLogFieldName(activeObjectAnnotation);

        if (!(targetClass instanceof ClassNode))
            throw new GroovyBugError("Class annotation " + activeObjectAnnotation.getClassNode().getName() + " annotated no Class, this must not happen.");

        final ClassNode classNode = (ClassNode) targetClass;

        final GroovyClassVisitor transformer = new MyClassCodeExpressionTransformer(source, actorFieldName);
        transformer.visitClass(classNode);
    }

    private static String lookupLogFieldName(final AnnotationNode logAnnotation) {
        final Expression member = logAnnotation.getMember("value");
        if (member != null && member.getText() != null) {
            return member.getText();
        } else {
            return "internalActiveObjectActor";
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
            if (exp instanceof MethodCallExpression) {
                return transformMethodCallExpression(exp);
            }
            return super.transform(exp);
        }

        @Override
        public void visitClass(final ClassNode node) {
            final FieldNode actorField = node.getField(actorFieldName);
            if (actorField != null) {
                this.addError("Class annotated with Log annotation cannot have log field declared", actorField);
            } else {
                actorNode = addActorFieldToClass(node, actorFieldName);
            }

            final Iterable<MethodNode> copyOfMethods = new ArrayList<MethodNode>(node.getMethods());
            for (final MethodNode method : copyOfMethods) {
                if (method.isStatic()) continue;
                final List<AnnotationNode> annotations = method.getAnnotations(new ClassNode(ActiveMethod.class));
                if (annotations.isEmpty()) continue;
                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA " + method);

                addActiveMethod(actorNode, node, method);
            }
            super.visitClass(node);
        }

        private static void addActiveMethod(final FieldNode actorNode, final ClassNode owner, final MethodNode original) {
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

            original.setCode(new ExpressionStatement(
                    new MethodCallExpression(
                            new VariableExpression(actorNode), "submit", args)
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

        private Expression transformMethodCallExpression(final Expression exp) {
            final MethodCallExpression mce = (MethodCallExpression) exp;
            if (!(mce.getObjectExpression() instanceof VariableExpression)) {
                return exp;
            }
            final VariableExpression variableExpression = (VariableExpression) mce.getObjectExpression();
            if (!variableExpression.getName().equals(actorFieldName)
                    || !(variableExpression.getAccessedVariable() instanceof DynamicVariable)) {
                return exp;
            }
            final String methodName = mce.getMethodAsString();
            if (methodName == null) return exp;
            if (usesSimpleMethodArgumentsOnly(mce)) return exp;

            variableExpression.setAccessedVariable(actorNode);

            if (!isLoggingMethod(methodName)) return exp;

            return wrapLoggingMethodCall(variableExpression, methodName, exp);
        }

        private static boolean usesSimpleMethodArgumentsOnly(final MethodCallExpression mce) {
            final Expression arguments = mce.getArguments();
            if (arguments instanceof TupleExpression) {
                final TupleExpression tuple = (TupleExpression) arguments;
                for (final Expression exp : tuple.getExpressions()) {
                    if (!isSimpleExpression(exp)) return false;
                }
                return true;
            }
            return !isSimpleExpression(arguments);
        }

        private static boolean isSimpleExpression(final Expression exp) {
            if (exp instanceof ConstantExpression) return true;
            return exp instanceof VariableExpression;
        }

        private static FieldNode addActorFieldToClass(final ClassNode classNode, final String logFieldName) {
            //todo make the field non-static
            //todo pass in the group
            return classNode.addField(logFieldName,
                    Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE,
                    new ClassNode(InternalActor.class),
//                    new ClassNode("groovyx.gpars.activeobject.InternalActor", Opcodes.ACC_PUBLIC, new ClassNode(Object.class)),
                    new MethodCallExpression(
                            new ClassExpression(new ClassNode(InternalActor.class)),
                            "create",
                            new ClassExpression(classNode)));
        }

        private static boolean isLoggingMethod(final String methodName) {
            return methodName.matches("fatal|error|warn|info|debug|trace");
        }

        private static Expression wrapLoggingMethodCall(final Expression logVariable, final String methodName, final Expression originalExpression) {
            final MethodCallExpression condition = new MethodCallExpression(
                    logVariable,
                    "is" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1, methodName.length()) + "Enabled",
                    ArgumentListExpression.EMPTY_ARGUMENTS);

            return new TernaryExpression(
                    new BooleanExpression(condition),
                    originalExpression,
                    ConstantExpression.NULL);
        }
    }
}


