//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars.actor.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovyx.gpars.actor.AbstractPooledActor;
import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Arrays;

/**
 * Utility class to implement AbstractPooledActor backed by any Runnable (including Closure)
 *
 * @author Alex Tkachman, Vaclav Pech
 */
public class RunnableBackedPooledActor extends AbstractPooledActor {
    private static final long serialVersionUID = 8992135845484038961L;

    private Runnable action;

    public RunnableBackedPooledActor() {
    }

    public RunnableBackedPooledActor(final Runnable handler) {
        setAction(handler);
    }

    final void setAction(final Runnable handler) {
        if (handler == null) {
            action = null;
        } else {
            if (handler instanceof Closure) {
                final Closure cloned = (Closure) ((Closure) handler).clone();
                if (cloned.getOwner() == cloned.getDelegate()) {
                    // otherwise someone else already took care for setting delegate for the closure
                    cloned.setDelegate(this);
                    cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
                } else {
                    cloned.setDelegate(new RunnableBackedPooledActorDelegate(cloned.getDelegate(), this));
                }
                action = cloned;
            } else {
                action = handler;
            }
        }
    }

    @Override protected void act() {
        if (action != null) {
            if (action instanceof Closure) {
                GroovyCategorySupport.use(Arrays.<Class>asList(ReplyCategory.class), (Closure) action);
            } else {
                action.run();
            }
        }
    }

    private static final class RunnableBackedPooledActorDelegate extends GroovyObjectSupport {
        private final Object first;
        private final Object second;

        RunnableBackedPooledActorDelegate(final Object first, final Object second) {
            this.first = first;
            this.second = second;
        }

        @Override public Object invokeMethod(final String name, final Object args) {
            try {
                return InvokerHelper.invokeMethod(first, name, args);
            }
            catch (MissingMethodException mme) {
                return InvokerHelper.invokeMethod(second, name, args);
            }
        }

        @Override public Object getProperty(final String propertyName) {
            try {
                return InvokerHelper.getProperty(first, propertyName);
            }
            catch (MissingPropertyException mpe) {
                return InvokerHelper.getProperty(second, propertyName);
            }
        }

        @Override public void setProperty(final String propertyName, final Object newValue) {
            try {
                InvokerHelper.setProperty(first, propertyName, newValue);
            }
            catch (MissingPropertyException mpe) {
                InvokerHelper.setProperty(second, propertyName, newValue);
            }
        }
    }
}
