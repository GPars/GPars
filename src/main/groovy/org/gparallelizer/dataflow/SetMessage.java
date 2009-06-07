package org.gparallelizer.dataflow;

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 5, 2009
 * Time: 2:04:42 PM
 * To change this template use File | Settings | File Templates.
 */
final class SetMessage<T> extends DataFlowMessage {
    private final T value;

    SetMessage(final T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
