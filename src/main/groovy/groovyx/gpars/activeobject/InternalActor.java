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

import groovyx.gpars.actor.DynamicDispatchActor;

/**
 * @author Vaclav Pech
 */
public class InternalActor extends DynamicDispatchActor {
    private static final long serialVersionUID = 6700367864074699984L;

    public void onMessage(final Object msg) {
        System.out.println("Received a message " + msg);
    }

    public static InternalActor create(final Object param) {
        final InternalActor internalActor = new InternalActor();
        internalActor.start();
        return internalActor;
    }
}
