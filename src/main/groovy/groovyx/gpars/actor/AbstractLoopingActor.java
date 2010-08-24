// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package groovyx.gpars.actor;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.util.AsyncMessagingCore;

/**
 * Wraps all actors that repeatedly loop through incoming messages and hold no implicit state between subsequent messages.
 *
 * @author Vaclav Pech
 *         Date: Aug 23, 2010
 */
public abstract class AbstractLoopingActor extends Actor {

    private final AsyncMessagingCore core;

    protected AbstractLoopingActor(final Closure code, final Closure errorHandler, final boolean fair) {
        this.core = new AsyncMessagingCore(parallelGroup.getThreadPool(), fair) {
            @Override
            protected void registerError(final Exception e) {
                errorHandler.call(e);
            }

            @Override
            protected void handleMessage(final Object message) {
                runEnhancedWithReplies((ActorMessage) message, code);
            }
        };
    }

    @Override
    public Actor start() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Actor stop() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Actor terminate() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isActive() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isActorThread() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected boolean hasBeenStopped() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageStream send(final Object message) {
        core.store(createActorMessage(message));
        return this;
    }
}
