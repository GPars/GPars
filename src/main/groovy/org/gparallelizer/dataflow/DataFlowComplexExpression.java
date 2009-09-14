package org.gparallelizer.dataflow;

import org.gparallelizer.MessageStream;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Tkachman
 */
public abstract class DataFlowComplexExpression<T> extends DataFlowExpression<T> implements Runnable {
    protected Object[] args;

    public DataFlowComplexExpression(final Object... elements) {
        this.args = elements;
    }

    protected void init() {
        HashSet<DataFlowVariable> variables = new HashSet<DataFlowVariable>();
        collectVariables(variables);

        if (variables.isEmpty()) {
            DataFlowActor.DATA_FLOW_GROUP.getThreadPool().execute(this);
        }
        else {
            final AtomicInteger count = new AtomicInteger(variables.size());
            MessageStream listener = new MessageStream() {
                public MessageStream send(Object message) {
                    if (count.decrementAndGet() == 0) {
                        DataFlowActor.DATA_FLOW_GROUP.getThreadPool().execute(DataFlowComplexExpression.this);
                    }
                    return this;
                }
            };

            for (DataFlowVariable variable : variables) {
                variable.getValAsync (listener);
            }
        }
    }

    protected abstract T evaluate() throws InterruptedException;

    protected void collectVariables(Set<DataFlowVariable> collection) {
        if (state.get() != S_INITIALIZED) {
            for (int i = 0; i != args.length; ++i) {
                Object element = args[i];
                if (element instanceof DataFlowExpression) {
                    DataFlowExpression dfe = (DataFlowExpression) element;
                    dfe.collectVariables(collection);
                }
            }
        }
    }

    public void run() {
        try {
            doBind(evaluate());
        } catch (InterruptedException e) {
        }
    }
}
