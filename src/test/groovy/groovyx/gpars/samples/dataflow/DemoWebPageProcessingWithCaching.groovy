// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

import groovyx.gpars.dataflow.DataFlowStream
import static groovyx.gpars.dataflow.DataFlow.operator
import static groovyx.gpars.dataflow.DataFlow.prioritySelector
import static groovyx.gpars.dataflow.DataFlow.selector
import static groovyx.gpars.dataflow.DataFlow.splitter
import static groovyx.gpars.dataflow.DataFlow.task

/**
 * Builds a network of dataflow operators, which will in turn complete provided urls, download them, search for the words
 * 'groovy' and 'scala' in them and returning reports telling, which site refers to which of the two languages.
 *
 * Uses advanced techniques to speed-up information retrieval using speculation and confirmation technique,
 * described by Greg Barish in his paper (see http://www.jroller.com/vaclav/entry/speculate_on_information_in_parallel)
 *
 * @author Vaclav Pech
 * Date 23rd Sep 2010
 */

final DataFlowStream urlsRequests = new DataFlowStream()
final DataFlowStream urls = new DataFlowStream()
final DataFlowStream urlsForSpeculation = new DataFlowStream()
final DataFlowStream pages = new DataFlowStream()
final DataFlowStream pagesForGroovy = new DataFlowStream()
final DataFlowStream pagesForScala = new DataFlowStream()
final DataFlowStream resultsFromGroovy = new DataFlowStream()
final DataFlowStream resultsFromScala = new DataFlowStream()
final DataFlowStream unconfirmedReports = new DataFlowStream()
final DataFlowStream confirmations = new DataFlowStream()
final DataFlowStream reports = new DataFlowStream()
final DataFlowStream contentForCache = new DataFlowStream()

//todo polish
//todo resolve confirmation on real data
//todo test prioritySelector based on selector
def long counter = 0L
def urlResolver = operator(inputs: [urlsRequests], outputs: [urls, urlsForSpeculation]) {
    bindAllOutputs([id: counter++, url: "http://www.${it}.com"])
}

def downloader = operator(inputs: [urls], outputs: [pages, confirmations, contentForCache]) {
    def content = it.url.toURL().text
    it.content = content
    bindAllOutputs it
}

def cache = ['http://www.jetbrains.com': 'groovy scala kfjhskfhsk']

def speculator = selector(inputs: [urlsForSpeculation, contentForCache], outputs: [pages]) {msg, index ->
    if (index == 0) {
        def content = cache[msg.url]
        if (content) bindAllOutputs([id: msg.id, url: msg.url, speculation: true, content: content])
    } else {
        cache[msg.url] = msg.content
    }
}

def splitter = splitter(pages, [pagesForGroovy, pagesForScala])

def groovyScanner = operator(inputs: [pagesForGroovy], outputs: [resultsFromGroovy]) {
    def foundWord = it.content.toLowerCase().contains('groovy') ? 'groovy' : ''
    bindOutput([id: it.id, url: it.url, foundWord: foundWord, speculation: it.speculation])
}

def scalaScanner = operator(inputs: [pagesForScala], outputs: [resultsFromScala]) {
    def foundWord = it.content.toLowerCase().contains('scala') ? 'scala' : ''
    bindOutput([id: it.id, url: it.url, foundWord: foundWord, speculation: it.speculation])
}

def reporter = operator(inputs: [resultsFromGroovy, resultsFromScala], outputs: [unconfirmedReports]) {g, s ->
    assert g.url == s.url
    assert g.id == s.id
    assert g.speculation == s.speculation
    def words = [g.foundWord, s.foundWord].findAll {it}
    def result
    switch (words.size()) {
        case 2:
            result = "${g.foundWord} and ${s.foundWord}"
            break
        case 1:
            result = words[0]
            break
        default:
            result = 'No interesting words'
    }
    bindOutput([id: g.id, url: g.url, content: g.content, speculation: g.speculation, result: ("$result found at ${g.url}" + (g.speculation ? ' based on speculation' : ''))])
}

def unconfirmedSpeculativeReports = [:]
def deliveredConfirmations = [:]
def processedIds = new HashSet()

def confirm = prioritySelector(inputs: [confirmations, unconfirmedReports], outputs: [reports]) {msg ->
    if (msg.id in processedIds) return
    if (msg.result != null) {  //differentiate between inputs serialized by the priority select
        final boolean isSpeculation = msg.speculation
        if (isSpeculation) {
            final def confirmation = deliveredConfirmations[msg.id]
            if (confirmation) {
                if (compareSpeculationWithRealContent(confirmation, msg)) {
                    bindOutput msg.result
                    processedIds << msg.id
                }
                deliveredConfirmations.remove(msg.id)
            } else {
                unconfirmedSpeculativeReports[msg.id] = msg
            }
        }
        else {
            bindOutput msg.result
            processedIds << msg.id
            unconfirmedSpeculativeReports.remove(msg.id)
            deliveredConfirmations.remove(msg.id)
        }
    } else {
        final Object speculativeReport = unconfirmedSpeculativeReports[msg.id]
        if (speculativeReport) {
            if (compareSpeculationWithRealContent(speculativeReport, msg)) {
                bindOutput speculativeReport.result
                processedIds << msg.id
            }
            unconfirmedSpeculativeReports.remove(msg.id)
        } else {
            deliveredConfirmations[msg.id] = msg
        }
    }
}

private boolean compareSpeculationWithRealContent(msg1, msg2) {
    //todo enable
//    return msg1?.content?.size() == msg2?.content?.size()
    true
}

task {
    for (;;) {
        println reports.val
    }
}

['dzone', 'infoq', 'jetbrains', 'oracle'].each {
    urlsRequests << it
}

sleep 10000
println 'Cache ' + cache.keySet()

['dzone', 'infoq', 'jetbrains', 'oracle'].each {
    urlsRequests << it
}
