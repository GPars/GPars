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


import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.PGroup

/**
 * Backs active objects and invokes all object's active methods.
 *
 * @author Vaclav Pech
 */
public final class InternalActor extends DynamicDispatchActor {
    private static final long serialVersionUID = 6700367864074699984L;
    public static final String METHOD_NAME_PREFIX = "activeObject_";

    /**
     * A DataFlowVariable is expected back
     * @param args The method parameters
     */
    DataFlowVariable submit(Object... args) {
        def result = new DataFlowVariable()
        if (this.currentThread == Thread.currentThread()) result << handleCurrentMessage(args)
        else sendAndContinue(args) {result << it}
        return result
    }

    /**
     * A response is expected back
     * @param args The method parameters
     */
    Object submitAndWait(Object... args) {
        if (this.currentThread == Thread.currentThread()) return handleCurrentMessage(args)
        else return sendAndWait(args);
    }

    /**
     * Handles incoming messages
     * @param msg The message representing the requested method call
     */
    public void onMessage(final Object msg) {
        def result
        try {
            result = handleCurrentMessage(msg)
        } finally {
            replyIfExists(result)
        }
    }

    private Object handleCurrentMessage(final Object msg) {
        try {
            Object target = msg[0]
            String methodName = msg[1]
            Object[] args = msg.size() > 2 ? msg[2..-1] : new Object[0]
            return target."${METHOD_NAME_PREFIX + methodName}"(* args)
        } catch (all) {
            return all
        }
    }

    public static InternalActor create(final Object groupId) {
        PGroup group
        if ('' == groupId) group = Actors.defaultActorPGroup
        else {
            group = ActiveObjectRegistry.instance.findGroupById(groupId)
        }
        if (group == null) throw new IllegalArgumentException("Cannot find a PGroup " + groupId + " in the ActiveObjectRegistry. Please make sure you register the group prior to instantiating ActiveObjects.")
        final InternalActor internalActor = new InternalActor();
        internalActor.parallelGroup = group;
        internalActor.start();
        return internalActor;
    }
}
