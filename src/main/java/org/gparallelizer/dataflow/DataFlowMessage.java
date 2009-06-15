package org.gparallelizer.dataflow;

/**
 * The parent message for messages used internally to implement Dataflow Concurrency.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
class DataFlowMessage {

    /**
     * The message to send to dataflow actors to terminate them.
     */
    static final DataFlowMessage EXIT = new DataFlowMessage();
}
