package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.*
import org.gparallelizer.dataflow.DataFlowStream
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowVariable

/**
 * A simple mashup sample, downloads content of three websites and checks how many of them refer to Groovy.
 */

def dzone = new DataFlowVariable()
def jroller = new DataFlowVariable()
def theserverside = new DataFlowVariable()

thread {
    dzone << 'http://www.dzone.com'.toURL().text
}

thread {
    jroller << 'http://www.jroller.com'.toURL().text
}

thread {
    theserverside << 'http://www.theserverside.com'.toURL().text
}

thread {
    println 'Number of Groovy sites today: ' + ([dzone(), jroller(), theserverside()].findAll {it.contains 'groovy'}).size()
    System.exit 0
}

System.in.read()