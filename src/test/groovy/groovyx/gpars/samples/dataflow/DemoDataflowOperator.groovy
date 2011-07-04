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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue

/**
 * Uses dataflow operators to download websites, cache their contents and check, whether they talk about particular technologies.
 *
 * @author Vaclav Pech
 * Date: Nov 16, 2009
 */

/**
 * Feed for URLs to retrieve from the cache
 */
def urlRequests = new DataflowQueue()

/**
 * Feed for URLs to download since they are not cached
 */
def downloadRequests = new DataflowQueue()

/**
 * Feed for site contents
 */
def sites = new DataflowQueue()

/**
 * DOWNLOADER
 *
 * Downloads received urls passing downloaded content to the output
 */
def downloader = Dataflow.operator(inputs: [downloadRequests], outputs: [urlRequests]) {request ->

    println "[Downloader] Downloading ${request.site}"
    def content = request.site.toURL().text
    println "[Downloader] Downloaded"
    bindOutput 0, [site: request.site, word: request.word, content: content]
}

cache = [:]
pendingDownloads = [:]

/**
 * CACHE
 *
 * Caches sites' contents. Accepts requests for url content, outputs the content. Outputs requests for download
 * if the site is not in cache yet.
 */
def cache = Dataflow.operator(inputs: [urlRequests], outputs: [downloadRequests, sites]) {request ->

    if (request.content) {
        println "[Cache] Caching ${request.site}"
        cache[request.site] = request.content
        bindOutput 1, request
        def downloads = pendingDownloads[request.site]
        if (downloads != null) {
            for (downloadRequest in downloads) {
                println "[Cache] Waking up"
                bindOutput 1, [site: downloadRequest.site, word: downloadRequest.word, content: request.content]
            }
            pendingDownloads.remove(request.site)
        }
    } else {
        println "[Cache] Retrieving ${request.site}"
        def content = cache[request.site]
        if (content) {
            println "[Cache] Found in cache"
            bindOutput 1, [site: request.site, word: request.word, content: content]
        } else {
            def downloads = pendingDownloads[request.site]
            if (downloads != null) {
                println "[Cache] Awaiting download"
                downloads << request
            } else {
                pendingDownloads[request.site] = []
                println "[Cache] Asking for download"
                bindOutput 0, request
            }
        }
    }
}

/**
 * Feed for the results
 */
def results = new DataflowQueue()

/**
 * FINDER
 *
 * Accepts sites' content searching for requested word in them. Sends the result to the results stream.
 */
def finder = Dataflow.operator(inputs: [sites], outputs: [results]) {request ->
    println "[Finder] Searching for ${request.word} in ${request.site}."
    def result = request.content.toUpperCase().contains(request.word.toUpperCase())
    results << "${result ? '' : 'No '}${request.word} in ${request.site}."
}

urlRequests << [site: 'http://www.dzone.com', word: 'groovy']
urlRequests << [site: 'http://www.dzone.com', word: 'java']
urlRequests << [site: 'http://www.infoq.com', word: 'groovy']
urlRequests << [site: 'http://www.infoq.com', word: 'java']
urlRequests << [site: 'http://www.dzone.com', word: 'scala']
urlRequests << [site: 'http://www.infoq.com', word: 'scala']
urlRequests << [site: 'http://www.theserverside.com', word: 'scala']
urlRequests << [site: 'http://www.theserverside.com', word: 'java']
urlRequests << [site: 'http://www.theserverside.com', word: 'groovy']

Thread.start {
    1.upto 9, {
        //There are multiple ways to get hold of the output feeds
        println "============================================================= Result: " + results.val
//        println "============================================================= Result: " + finder.output.val
        //        println "============================================================= Result: " + finder.outputs[0].val
    }
    finder.terminate()
    cache.terminate()
    downloader.terminate()
}