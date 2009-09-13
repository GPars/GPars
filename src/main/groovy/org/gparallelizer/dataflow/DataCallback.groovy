package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * A helper class enabling the 'whenBound()' functionality of a DataFlowVariable.
 * An actor that waits asynchronously on the DFV to be bound. Once the DFV is bound,
 * upon receiving the message the actor runs the supplied closure / code with the DFV value as a parameter.
 * 
 * @author Vaclav Pech
 * Date: Sep 13, 2009
 */
final class DataCallback extends AbstractPooledActor {
    private Closure code
    private DataFlowVariable df

    /**
     * @param code The closure to run
     * @param df The DFV to wait for
     */
    DataCallback(final Closure code, final DataFlowVariable df) {
        this.code = code
        this.df = df
    }

    /**
     * @param code The code to run. An object responding to 'perform(Object value)' is expected
     * @param df The DFV to wait for
     */
    DataCallback(final Object code, final DataFlowVariable df) {
        this.code = {code.perform(it)}
        this.df = df
    }

    @Override protected void act() {
        df.getValAsync(this)
        react {
            code.call(it)
        }
    }
}