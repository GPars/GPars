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

package groovyx.gpars.benchmark.akka

/**
 * @author Jiri Mares, Vaclav Pech
 */
abstract class PipelineHandler {
    abstract String handleMessage(String message);

    void handle(actor, follower) {
        actor.react {message ->
            switch (message) {
                case String:
                    follower?.send(handleMessage(message))
                    handle(actor, follower)
                    break
                case StopMessage.instance:
                    follower?.send(message)
            }
        }
    }
}

class DownloadHandler extends PipelineHandler {
    String handleMessage(String message) {
        message.replaceFirst('Requested ', 'Downloaded ')
    }
}

class IndexHandler extends PipelineHandler {
    String handleMessage(String message) {
        message.replaceFirst('Downloaded ', 'Indexed ')
    }
}

class WriteHandler extends PipelineHandler {
    String handleMessage(String message) {
        message.replaceFirst('Indexed ', 'Wrote ')
    }
}

perform()  //warmup
println perform()

private long perform() {
    def benchmark = new PipelineBenchmark()
    def pgroup = benchmark.create()

    benchmark.downloader = pgroup.actor {
        new DownloadHandler().handle(delegate,
                benchmark.indexer = pgroup.actor {
                    new IndexHandler().handle(delegate,
                            benchmark.writer = pgroup.actor {
                                new WriteHandler().handle(delegate,
                                        null)
                            })
                })
    }

    benchmark.prepare(pgroup, false)
    final result = benchmark.benchmark()
    benchmark.shutdown(pgroup)
    result
}




