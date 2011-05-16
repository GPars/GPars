// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.activeobject;


import groovyx.gpars.MessagingRunnable;
import groovyx.gpars.actor.Actors;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.group.PGroup;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Arrays;
import java.util.Collection;

/**
 * Backs active objects and invokes all object's active methods.
 *
 * @author Vaclav Pech
 */
@SuppressWarnings({"CallToStringEquals"})
public final class InternalActor extends DynamicDispatchActor {
    private static final long serialVersionUID = 6700367864074699984L;
    public static final String METHOD_NAME_PREFIX = "activeObject_";
    private static final Object[] No_ARGS = new Object[0];

    /**
     * A DataflowVariable is expected back
     * @param args The method parameters
     * @return A Promise for the real result
     */
    DataflowVariable<Object> submit(final Object... args) {
        final DataflowVariable<Object> result = new DataflowVariable<Object>();
        if (this.currentThread == Thread.currentThread()) result.bind(handleCurrentMessage(args));
        else sendAndContinue(args, new MessagingRunnable<Object>() {
            @Override
            protected void doRun(final Object argument) {
                result.bind(argument);
            }
        });
        return result;
    }

    /**
     * A response is expected back
     * @param args The method parameters
     * @return The result of the internal method as returned when run in the internal actor's context
     * @throws InterruptedException If the current thread gets interrupted while waiting for the internal actor to respond
     */
    Object submitAndWait(final Object... args) throws InterruptedException {
        if (this.currentThread == Thread.currentThread()) return handleCurrentMessage(args);
        else return sendAndWait(args);
    }

    /**
     * Handles incoming messages
     * @param msg The message representing the requested method call
     */
    public void onMessage(final Object msg) {
        replyIfExists(handleCurrentMessage(msg));
    }

    @SuppressWarnings({"unchecked"})
    private static Object handleCurrentMessage(final Object msg) {
        try {
            final Object[] params;
            if (msg instanceof Collection) params = ((Collection<Object>)msg).toArray(new Object[((Collection<Object>)msg).size()]);
            else params = (Object[]) msg;
            final Object target = params[0];
            final String methodName = (String) params[1];
            final Object[] args = params.length > 2 ? Arrays.copyOfRange(params, 2, params.length) : No_ARGS;
            return InvokerHelper.invokeMethod(target, METHOD_NAME_PREFIX + methodName, args);
        } catch (Throwable all) {
            return all;
        }
    }

    public static InternalActor create(final Object groupId) {
        final PGroup group;
        if ("".equals(groupId)) group = Actors.defaultActorPGroup;
        else {
            group = ActiveObjectRegistry.getInstance().findGroupById((String) groupId);
        }
        if (group == null) throw new IllegalArgumentException("Cannot find a PGroup " + groupId + " in the ActiveObjectRegistry. Please make sure you register the group prior to instantiating ActiveObjects.");
        final InternalActor internalActor = new InternalActor();
        internalActor.setParallelGroup(group);
        internalActor.start();
        return internalActor;
    }
}
