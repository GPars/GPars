/*
 * Copyright 2005-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.gpars.javademo.javademo;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.scheduler.DefaultPool;
import org.junit.Test;


public class PipelineStatelessActorTest {
    @Test
    public void testActor() throws InterruptedException {
        final StatefulDynamicDispatchActor writer = new DownloadStatefulDynamicDispatchActor();
        final StatefulDynamicDispatchActor indexer = new IndexStatefulDynamicDispatchActor();
        final StatefulDynamicDispatchActor downloader = new WriteStatefulDynamicDispatchActor();

        downloader.follower = indexer;
        indexer.follower = writer;

        final DefaultPGroup group = new DefaultPGroup(new DefaultPool(false, 4));

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
        while (i < 1000000) {
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

abstract class StatefulDynamicDispatchActor extends DynamicDispatchActor {
    Actor follower;

    abstract String handleMessage(String message);

    void onMessage(final String message) {
        if (follower != null) follower.send(handleMessage(message));
    }

    void onMessage(final StopMessage message) {
        if (follower != null) follower.send(message);
        terminate();
    }
}

final class DownloadStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    String handleMessage(final String message) {
        return message.replaceFirst("Requested ", "Downloaded ");
    }
}

final class IndexStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    String handleMessage(final String message) {
        return message.replaceFirst("Downloaded ", "Indexed ");
    }
}

final class WriteStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    String handleMessage(final String message) {
        return message.replaceFirst("Indexed ", "Wrote ");
    }
}

class StopMessage {
}
