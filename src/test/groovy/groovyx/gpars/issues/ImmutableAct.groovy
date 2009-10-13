package groovyx.gpars.issues

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Sep 21, 2009
 * Time: 10:56:05 PM
 * To change this template use File | Settings | File Templates.
 */

class FailingA {
    protected void act() {
        throw new UnsupportedOperationException()
    }
}

//@Immutable
final class FailingB extends FailingA {
    public void act() {

    }
}

final def b = new FailingB()
