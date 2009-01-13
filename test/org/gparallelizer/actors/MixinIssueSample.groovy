/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 7, 2009
 */
class A {

    def final foo() {
        bar()
    }

    private final String bar() {
        return "Bar"
    }
}

class B extends A {}

class C {}

C.metaClass {
    mixin B
}

def c= new C()
c.foo()

