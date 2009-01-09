/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 8, 2009
 */
class Parent {
    private String myPrivateProperty="secret"

    public final void foo() {
        [1, 2, 3].each {
            println myPrivateProperty
        }
    }
}

class Child extends Parent {}

new Child().foo()

