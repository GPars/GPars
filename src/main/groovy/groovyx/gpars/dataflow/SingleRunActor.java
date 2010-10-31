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

package groovyx.gpars.dataflow;

import groovy.lang.Closure;
import groovyx.gpars.actor.AbstractPooledActor;

/**
 * An actor representing a dataflow thread. Runs the supplied block of code inside the act() actor method once.
 *
 * @author Vaclav Pech, Dierk Koenig
 *         Date: Jun 5, 2009
 */
public final class SingleRunActor extends AbstractPooledActor {
    private static final long serialVersionUID = 516126583515361939L;

    /**
     * Sets the default Dataflow Concurrency actor group on the actor.
     */
    public SingleRunActor() {
        this.parallelGroup = DataFlow.DATA_FLOW_GROUP;
    }

    private Closure body;

    @Override
    protected void act() {
        body.setDelegate(this);
        body.call();
    }

    public void setBody(final Closure body) {
        this.body = body;
    }
}

