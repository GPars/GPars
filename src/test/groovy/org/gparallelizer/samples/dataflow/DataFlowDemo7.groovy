package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowStream
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread

DataFlowActor.DATA_FLOW_GROUP.resize 10

def buffer = new DataFlowStream()

final def urls = [
        'http://www.dzone.com',
        'http://www.jroller.com',
        'http://www.theserverside.com'
]

thread {
    for(url in urls) {
        println 'AAAAAAAAAAAAAAAAAAA'
        final def site = new DataFlowVariable()
        buffer << site
        site << url.toURL().text
        println 'CCCCCCCCCCCCCCCCCc'
    }
}

thread {
    int count = 0
    0.upto(urls.size()-1) {
        def content = ~buffer
        println 'BBBBBBBBBBBBBBBBBBBBBbb'
        if (content.contains('groovy')) count++
    }
    println "Number of Groovy sites today: $count"
    System.exit 0
}

System.in.read()