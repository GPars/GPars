package org.gparallelizer.issues

class A3 {

    public int counter = 0


}

class B3 extends A3 {
    public void foo() {
        counter = 1
    }
}

class C3 {}

C3.metaClass {
    mixin B3
}

def c= new C3()
c.foo()

