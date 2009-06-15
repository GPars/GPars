package org.gparallelizer.actors.transformations;

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE) @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD}) @org.codehaus.groovy.transform.GroovyASTTransformationClass({"org.gparallelizer.transformations.AsynchronousASTTransformation"}) public @interface Asynchronous {

    boolean waitForResult() default true;
}
