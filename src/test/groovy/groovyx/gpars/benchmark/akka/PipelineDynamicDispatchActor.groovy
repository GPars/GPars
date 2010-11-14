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

package groovyx.gpars.benchmark.akka

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DynamicDispatchActor

/**
 * @author Jiri Mares, Vaclav Pech
 */
abstract class StatefulDynamicDispatchActor extends DynamicDispatchActor {
    Actor follower

    abstract String handleMessage(String message)

    void onMessage(String message) {
        follower?.send(handleMessage(message))
    }

    void onMessage(StopMessage message) {
        follower?.send(message)
        terminate()
    }

}
final class DownloadStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    String handleMessage(String message) {
        message.replaceFirst("Requested ", "Downloaded ")
    }
}

final class IndexStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    String handleMessage(String message) {
        message.replaceFirst("Downloaded ", "Indexed ")
    }
}

final class WriteStatefulDynamicDispatchActor extends StatefulDynamicDispatchActor {
    String handleMessage(String message) {
        message.replaceFirst("Indexed ", "Wrote ")
    }
}

new PipelineBenchmark(
        writer: new WriteStatefulDynamicDispatchActor(),
        indexer: new IndexStatefulDynamicDispatchActor(),
        downloader: new DownloadStatefulDynamicDispatchActor()
).run()
