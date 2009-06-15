package org.gparallelizer.dataflow;

/**
 * A message representing a request for value of a DataFlowVariable, once available. The value should be replied back
 * as an instance of the Set message class.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
final class Get<T> extends DataFlowMessage { }
