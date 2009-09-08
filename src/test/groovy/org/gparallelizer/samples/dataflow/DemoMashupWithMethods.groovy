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

import static org.gparallelizer.Asynchronizer.*
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start


/**
 * A simple mashup sample, downloads content of three websites and checks how many of them refer to Groovy.
 */
final List urls = ['http://www.dzone.com', 'http://www.jroller.com', 'http://www.theserverside.com']

start {
    def pages = urls.collect { downloadPage(it) }
    doAsync {
        println "Number of Groovy sites today: " +
                (pages.findAllAsync {
                    it.val.toUpperCase().contains 'GROOVY'
                }).size()
    }
    System.exit 0
}

def downloadPage(def url) {
    def page = new DataFlowVariable()
    start {
        println "Started downloading from $url"
        page << url.toURL().text
        println "Done downloading from $url"
    }
    return page
}
