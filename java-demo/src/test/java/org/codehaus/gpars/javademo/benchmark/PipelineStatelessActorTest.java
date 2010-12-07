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

package org.codehaus.gpars.javademo.benchmark;

import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.DefaultPool;
import org.junit.Test;

/**
 * @author Jiri Mares, Vaclav Pech
 */

@SuppressWarnings({"MagicNumber"})
public class PipelineStatelessActorTest {
    @Test
    public void testActor() throws InterruptedException {
        final StatefulDynamicDispatchActor writer = new DownloadStatefulDynamicDispatchActor();
        final StatefulDynamicDispatchActor indexer = new IndexStatefulDynamicDispatchActor();
        final StatefulDynamicDispatchActor downloader = new WriteStatefulDynamicDispatchActor();

        downloader.follower = indexer;
        indexer.follower = writer;

        final PGroup group = new DefaultPGroup(new DefaultPool(false, 4));

        writer.setParallelGroup(group);
        indexer.setParallelGroup(group);
        downloader.setParallelGroup(group);
        downloader.follower = indexer;
        indexer.follower = writer;
        writer.silentStart();
        indexer.silentStart();
        downloader.silentStart();

        final long t1 = System.currentTimeMillis();

        long i = 0;
        while (i < 1000000L) {
            downloader.send("Requested " + i);
            i++;
        }

        downloader.send(new StopMessage());
        downloader.join();
        indexer.join();
        writer.join();
        final long t2 = System.currentTimeMillis();

        System.out.println(t2 - t1);
        group.shutdown();
    }

}

class StopMessage {
}