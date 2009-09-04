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
