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

import groovy.swing.SwingBuilder
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * @author Vaclav Pech
 */

def defaultSites = [
        'http://www.dzone.com': 'Java Groovy',
        'http://www.infoq.com': 'Java Scala',
        'http://www.groovy.org': 'Java Groovy'
]

def contentDownloaderArea
def contentCacheArea
def wordFinderArea

JTextArea.metaClass {
    report = {text ->
        def currentArea = delegate
        SwingUtilities.invokeLater {
            currentArea.text += text
            contentDownloaderArea.text += "\n"
            contentCacheArea.text += "\n"
            wordFinderArea.text += "\n"
        }
    }
}

def urlRequests = new DataflowQueue()
def downloadRequests = new DataflowQueue()
def sites = new DataflowQueue()

/**
 * Downloads received urls passing downloaded content to the output
 */
Dataflow.operator(inputs: [downloadRequests], outputs: [urlRequests]) {request ->

    contentDownloaderArea.report "Downloading ${request.site}"
//    def content = request.site.toURL().text
    def content = defaultSites[request.site]
    Thread.sleep 2000
    if (request.site.contains('info')) Thread.sleep 10000

    contentDownloaderArea.report "Downloaded"
    bindOutput 0, [site: request.site, content: content]
}

cache = [:]
pendingDownloads = [:]

/**
 * Caches sites' contents. Accepts requests for url content, outputs the content. Outputs requests for download
 * if the site is not in cache yet.
 */
Dataflow.operator(inputs: [urlRequests], outputs: [downloadRequests, sites]) {request ->

    if (request.content) {
        contentCacheArea.report "Caching ${request.site}"
        Thread.sleep 2000
        cache[request.site] = request.content
        bindOutput 1, request
        def downloads = pendingDownloads[request.site]
        if (downloads != null) {
            for (downloadRequest in downloads) {
                contentCacheArea.report "Waking up"
                bindOutput 1, [site: downloadRequest.site, content: request.content]
            }
            pendingDownloads.remove(request.site)
        }
    } else {
        contentCacheArea.report "Retrieving ${request.site}"
        Thread.sleep 2000
        def content = cache[request.site]
        if (content) {
            contentCacheArea.report "Found in cache"
            bindOutput 1, [site: request.site, content: content]
        } else {
            def downloads = pendingDownloads[request.site]
            if (downloads != null) {
                contentCacheArea.report "Awaiting download"
                downloads << request
            } else {
                pendingDownloads[request.site] = []
                contentCacheArea.report "Asking for download"
                bindOutput 0, request
            }
        }
    }
}

/**
 * Accepts sites' content searching for the word Groovy in them, printing results to the UI.
 */
Dataflow.operator(inputs: [sites], outputs: []) {request ->
    def result = request.content.toUpperCase().contains('GROOVY')
    wordFinderArea.report "${result ? '' : 'No '}Groovy in ${request.site}."
}

def createOperatorPanel = {actorName ->
    def area = null
    panel {
        vbox() {
            label(actorName)
            scrollPane() {
                area = textArea(columns: 20, rows: 30)
                Font currentFont = area.font
                area.setFont new Font(currentFont.name, currentFont.style, 18)
            }
        }
    }
    return area
}

final JFrame frame = new SwingBuilder().frame(title: 'Actors', defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
    createOperatorPanel.delegate = delegate
    vbox {
        hbox {
            contentCacheArea = createOperatorPanel('Cache')
            contentDownloaderArea = createOperatorPanel('Downloader')
            wordFinderArea = createOperatorPanel('Finder')
        }
        vbox {
            hbox {
                textField(id: 'siteName', text: 'http://www.dzone.com')
                button(text: 'Send', actionPerformed: {
                    doOutside {
                        Thread.sleep 2000
                        urlRequests << [site: siteName.text, word: 'GROOVY']
                    }
                })
                button(text: 'Clear', actionPerformed: {
                    contentDownloaderArea.text = ''
                    contentCacheArea.text = ''
                    wordFinderArea.text = ''
                })
            }
        }
    }

}

frame.visible = true
frame.pack()

