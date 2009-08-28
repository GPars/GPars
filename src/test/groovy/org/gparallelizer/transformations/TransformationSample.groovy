package org.gparallelizer.transformations

import org.gparallelizer.transformations.Asynchronous

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