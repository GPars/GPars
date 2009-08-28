package org.gparallelizer.dataflow;

/**
 * A message representing a value to set on the DataFlowVariable.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
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
