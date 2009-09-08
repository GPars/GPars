//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowStream
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * A producer-consumer demo using the DataFlowStream class. Producer downloads web content from a list of urls,
 * the concumenr then counts number of sites refering Groovy. 
 */

def buffer = new DataFlowStream()

final def urls = [
        'http://www.dzone.com',
        'http://www.jroller.com',
        'http://www.theserverside.com'
]

start {
    for(url in urls) {
        final def site = new DataFlowVariable()
        buffer << site
        site << url.toURL().text
    }
}

start {
    int count = 0
    0.upto(urls.size()-1) {
        def content = buffer.val
        if (content.contains('groovy')) count++
    }
    println "Number of Groovy sites today: $count"
    System.exit 0
}
