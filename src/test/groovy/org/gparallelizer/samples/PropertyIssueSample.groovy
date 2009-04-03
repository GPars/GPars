/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 8, 2009
 */
class Parent {
    private String myPrivateProperty="secret"

    public void foo() {
        Thread.start {
            println myPrivateProperty
        }
    }
}

class Child extends Parent {

}

final Parent item = new Child()
item.foo()

