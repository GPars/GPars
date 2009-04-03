/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 13, 2009
 */
class SampleA {
    public void foo() {
        println 'Original foo ' + receive('')
    }

    protected Object receive() {
        return "Message"
    }

    protected Object receive(Object param) {
        receive() + param
    }
}

class SampleB {}

SampleB.metaClass {
    mixin SampleA

    foo = {->
        println 'New foo ' + receive('')
    }
}

final SampleA a = new SampleA()
a.foo()

final SampleB b = new SampleB()
b.foo()

