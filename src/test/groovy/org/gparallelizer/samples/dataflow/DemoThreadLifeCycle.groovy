package org.gparallelizer.samples.dataflow

import static org.gparallelizer.dataflow.DataFlow.*

final def thread = thread {

}

thread.metaClass {
    afterStart = {->
        println "Started"
    }
}