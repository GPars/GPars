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
