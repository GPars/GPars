package org.gparallelizer.dataflow;

/**
 * Represents a state of a DataFlowVariable.
 *
 * @author Vaclav Pech
 * Date: Jun 4, 2009
 */
@SuppressWarnings({"RefusedBequest"})
public enum DataFlowState {
    UNASSIGNED {
        @Override public boolean canBeAssigned() {
            return true;
        }},
    ASSIGNED,
    STOPPED;

    public boolean canBeAssigned() {
        return false;
    }
}
