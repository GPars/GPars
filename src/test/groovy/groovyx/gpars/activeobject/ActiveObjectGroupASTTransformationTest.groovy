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

package groovyx.gpars.activeobject

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup

public class ActiveObjectGroupASTTransformationTest extends GroovyTestCase {

    public void testActorGroupMatchesDefault() {
        final actor = new MyWrapper().internalActiveObjectActor
        assert actor.parallelGroup == Actors.defaultActorPGroup
    }

    public void testActorGroupMatches() {
        final DefaultPGroup group = new DefaultPGroup()
        ActiveObjectRegistry.instance.register("group1", group)
        final actor = new MyGroupWrapper().internalActiveObjectActor
        assert actor.parallelGroup == group
    }

    public void testInvalidActorGroupReported() {
        shouldFail(IllegalArgumentException) {
            new MyInvalidGroupWrapper()
        }
    }
}

@ActiveObject("group1")
class MyGroupWrapper {
    def result = new DataflowVariable()

    @ActiveMethod
    public void foo(value) {
        result << Thread.currentThread()
    }
}

@ActiveObject("invalidGroup")
class MyInvalidGroupWrapper {
    def result = new DataflowVariable()

    @ActiveMethod
    public void foo(value) {
        result << Thread.currentThread()
    }
}