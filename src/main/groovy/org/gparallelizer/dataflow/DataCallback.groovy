package org.gparallelizer.dataflow

import org.gparallelizer.MessageStream
   //todo update doc
/**
 *
 * A helper class enabling the 'whenBound()' functionality of a DataFlowVariable.
 * An actor that waits asynchronously on the DFV to be bound. Once the DFV is bound,
 * upon receiving the message the actor runs the supplied closure / code with the DFV value as a parameter.
 * 
 * @author Vaclav Pech, Alex Tkachman
 * Date: Sep 13, 2009
 */
final class DataCallback extends MessageStream {
    private final Closure code

    /**
     * @param code The closure to run
     * @param df The DFV to wait for
     */
    DataCallback(final Closure code) {
        this.code = code
    }

    /**
     * @param code The code to run. An object responding to 'perform(Object value)' is expected
     * @param df The DFV to wait for
     */
    DataCallback(final Object code) {
        this.code = {code.perform(it)}
    }

    /**
     * Sends a message back to the DataCallback.
     * Will schedule processing the internal closure with the thread pool
     */
    @Override
    public MessageStream send(Object message) {
      DataFlowActor.DATA_FLOW_GROUP.threadPool.execute{
        code.call message
      };
      return this;
    }
}