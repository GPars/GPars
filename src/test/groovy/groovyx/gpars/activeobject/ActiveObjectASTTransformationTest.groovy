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

class ActiveObjectASTTransformationTest extends GroovyTestCase {
    //todo return values
    //todo test actor field name
        //todo test group, messages, uniqueness
        // todo test static methods, inheritance of active methods, correctness
    //todo report if no methods are active
    //todo return a DFV

            //todo make the field non-static
            //todo pass in the group
    //todo finish all methods before exit


    public void testActorIsActive() {
        final actor = new MyWrapper().internalActiveObjectActor
        assert actor.active
    }

    public void testActorUniqueness() {
        final actor1 = new MyWrapper().internalActiveObjectActor
        final actor2 = new MyWrapper().internalActiveObjectActor
        assert actor1.active
        assert actor2.active
        assert actor1.is(actor2)
    }
}
@ActiveObject
class MyWrapper {}
