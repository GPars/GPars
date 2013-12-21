// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable

import static groovyx.gpars.dataflow.Dataflow.task

/**
 * A producer-consumer demo using the DataflowQueue class. Producer downloads web content from a list of urls,
 * the consumer then counts number of sites referring Groovy. 
 */

def buffer = new DataflowQueue()

final def urls = [
        'http://www.dzone.com',
        'http://www.jroller.com',
        'http://www.theserverside.com'
]

task {
    for (url in urls) {
        final def site = new DataflowVariable()
        buffer << site
        try {
            site << url.toURL().text
        } catch (all) {
            site << 'Error downloading page'
        }
    }
}

task {
    int count = 0
    0.upto(urls.size() - 1) {
        def content = buffer.val
        if (content.contains('groovy')) count++
    }
    println "Number of Groovy sites today: $count"
}.join()
