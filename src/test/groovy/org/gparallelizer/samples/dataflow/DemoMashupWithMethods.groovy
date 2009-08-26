package org.gparallelizer.samples.dataflow

import static org.gparallelizer.Asynchronizer.*
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread


/**
 * A simple mashup sample, downloads content of three websites and checks how many of them refer to Groovy.
 */
final List urls = ['http://www.dzone.com', 'http://www.jroller.com', 'http://www.theserverside.com']

thread {
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
    thread {
        println "Started downloading from $url"
        page << url.toURL().text
        println "Done downloading from $url"
    }
    return page
}
