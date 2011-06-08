// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.groups

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.BlockingActor
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CountDownLatch

public class PGroupTest extends GroovyTestCase {
    public void testDefaultGroupDaemon() {
        volatile boolean daemon = false;
        final CountDownLatch latch = new CountDownLatch(1)

        def actor = Actors.actor {
            daemon = Thread.currentThread().isDaemon()
            latch.countDown()
        }

        assertEquals Actors.defaultActorPGroup, actor.parallelGroup
        latch.await()
        assert daemon
    }

    public void testGroupDaemonFlag() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final DefaultPGroup daemonGroup = new DefaultPGroup()
        final PGroup nonDaemonGroup = new NonDaemonPGroup()

        def actor1 = daemonGroup.actor {
            daemon = Thread.currentThread().isDaemon()
            latch1.countDown()
        }

        assertEquals daemonGroup, actor1.parallelGroup
        latch1.await()
        assert daemon

        def actor2 = nonDaemonGroup.actor {
            daemon = Thread.currentThread().isDaemon()
            latch2.countDown()
        }

        assertEquals nonDaemonGroup, actor2.parallelGroup
        latch2.await()
        assertFalse daemon

        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testGroupsWithActorInheritance() {
        def daemon = new DataflowVariable();

        final DefaultPGroup daemonGroup = new DefaultPGroup()
        final PGroup nonDaemonGroup = new NonDaemonPGroup()

        final InheritanceGroupTestActor actor1 = new InheritanceGroupTestActor(daemonGroup, daemon)
        actor1.start()

        assertEquals daemonGroup, actor1.parallelGroup
        assert daemon.val

        daemon = new DataflowVariable()
        final InheritanceGroupTestActor actor2 = new InheritanceGroupTestActor(nonDaemonGroup, daemon)
        actor2.start()

        assertEquals nonDaemonGroup, actor2.parallelGroup
        assertFalse daemon.val
        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testValidGroupReset() {
        final DefaultPGroup daemonGroup = new DefaultPGroup()
        final PGroup nonDaemonGroup = new NonDaemonPGroup()
        final GroupTestActor actor = new GroupTestActor(daemonGroup)

        assertEquals daemonGroup, actor.parallelGroup
        actor.parallelGroup = nonDaemonGroup
        assertEquals nonDaemonGroup, actor.parallelGroup

        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testInvalidGroupReset() {
        final DefaultPGroup daemonGroup = new DefaultPGroup()
        final PGroup nonDaemonGroup = new NonDaemonPGroup()
        final GroupTestActor actor = new GroupTestActor(daemonGroup)
        actor.start()
        assertEquals daemonGroup, actor.parallelGroup
        shouldFail(IllegalStateException) {
            actor.parallelGroup = nonDaemonGroup
        }
        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testDifferentPools() {
        final DefaultPGroup daemonGroup1 = new DefaultPGroup()
        final DefaultPGroup daemonGroup2 = new DefaultPGroup(3)
        final NonDaemonPGroup nonDaemonGroup1 = new NonDaemonPGroup()
        final NonDaemonPGroup nonDaemonGroup2 = new NonDaemonPGroup(3)
        final DefaultPGroup defaultGroup = Actors.defaultActorPGroup

        assert daemonGroup1.threadPool != daemonGroup2.threadPool
        assert daemonGroup1.threadPool != nonDaemonGroup1.threadPool
        assert daemonGroup1.threadPool != defaultGroup.threadPool

        assert nonDaemonGroup1.threadPool != daemonGroup2.threadPool
        assert nonDaemonGroup1.threadPool != nonDaemonGroup2.threadPool
        assert nonDaemonGroup1.threadPool != defaultGroup.threadPool
        daemonGroup1.shutdown()
        daemonGroup2.shutdown()
        nonDaemonGroup1.shutdown()
        nonDaemonGroup2.shutdown()
    }
}

class GroupTestActor extends BlockingActor {

    def GroupTestActor(PGroup group) {
        parallelGroup = group
    }

    protected void act() {
    }
}

class InheritanceGroupTestActor extends BlockingActor {

    private def daemon

    def InheritanceGroupTestActor(PGroup group, daemon) {
        parallelGroup = group
        this.daemon = daemon
    }

    @Override
    protected void act() {
        daemon << Thread.currentThread().isDaemon()
    }
}