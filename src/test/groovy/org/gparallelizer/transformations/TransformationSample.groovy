package org.gparallelizer.transformations

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