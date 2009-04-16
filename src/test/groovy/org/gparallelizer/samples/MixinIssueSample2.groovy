/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 13, 2009
 */
class SampleA {
    private void foo() {
        println 'Original foo ' + receive('')
    }

    private String bar() {
        "Bar"
    }

    protected Object receive() {
        return "Message " + bar()
    }

    protected Object receive(Object param) {
        receive() + param
    }

    public void perform() {
        foo()
        foo()
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
a.perform()

final SampleB b = new SampleB()
b.perform()

