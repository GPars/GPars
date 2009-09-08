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

def dzone = new DataFlowVariable()
def jroller = new DataFlowVariable()
def theserverside = new DataFlowVariable()

start {
    println 'Started downloading from DZone'
    dzone << 'http://www.dzone.com'.toURL().text
    println 'Done downloading from DZone'
}

start {
    println 'Started downloading from JRoller'
    jroller << 'http://www.jroller.com'.toURL().text
    println 'Done downloading from JRoller'
}

start {
    println 'Started downloading from TheServerSide'
    theserverside << 'http://www.theserverside.com'.toURL().text
    println 'Done downloading from TheServerSide'
}

start {
    doAsync {
        println "Number of Groovy sites today: " +
                ([dzone, jroller, theserverside].findAllAsync {
                    it.val.toUpperCase().contains 'GROOVY'
                }).size()
    }
    System.exit 0
}
