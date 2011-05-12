// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataflowQueue
import static groovyx.gpars.dataflow.Dataflow.operator
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Builds a network of dataflow operators, which will in turn complete provided urls, download them, search for the words
 * 'groovy' and 'scala' in them and returning reports telling, which site refers to which of the two languages.
 *
 * You might consider checking out the speculative web page processing demo, which uses advanced techniques
 * to speed-up information retrieval using speculation and confirmation technique,
 * described by Greg Barish in his paper (see http://www.jroller.com/vaclav/entry/speculate_on_information_in_parallel)
 *
 * @author Vaclav Pech
 * Date 22nd Sep 2010
 */

final DataflowQueue urlsRequests = new DataflowQueue()
final DataflowQueue urls = new DataflowQueue()
final DataflowQueue pagesForGroovy = new DataflowQueue()
final DataflowQueue pagesForScala = new DataflowQueue()
final DataflowQueue resultsFromGroovy = new DataflowQueue()
final DataflowQueue resultsFromScala = new DataflowQueue()
final DataflowQueue reports = new DataflowQueue()

def urlResolver = operator(urlsRequests, urls) {
    bindOutput([url: "http://www.${it}.com"])
}

def downloader = operator(inputs: [urls], outputs: [pagesForGroovy, pagesForScala]) {
    def content = it.url.toURL().text
    it.content = content
    bindAllOutputsAtomically it
}

def groovyScanner = operator(pagesForGroovy, resultsFromGroovy) {
    def foundWord = it.content.toLowerCase().contains('groovy') ? 'groovy' : ''
    bindOutput([url: it.url, foundWord: foundWord])
}
def scalaScanner = operator(pagesForScala, resultsFromScala) {
    def foundWord = it.content.toLowerCase().contains('scala') ? 'scala' : ''
    bindOutput([url: it.url, foundWord: foundWord])
}

def reporter = operator(inputs: [resultsFromGroovy, resultsFromScala], outputs: [reports]) {g, s ->
    assert g.url == s.url
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
    bindOutput "$result found at ${g.url}"
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