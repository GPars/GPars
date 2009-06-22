package org.gparallelizer.transformations

import org.gparallelizer.actors.transformations.Asynchronous

class A {
    @Asynchronous public String foo(String value) {
        return "Foo $value"
    }
}

@Asynchronous(waitForResult = false) def baz() {
    return 'baz'    
}

println new A().foo('bar')

println baz()