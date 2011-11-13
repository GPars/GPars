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

package groovyx.gpars.samples.actors.visual

import groovy.swing.SwingBuilder
import groovy.transform.Immutable
import groovyx.gpars.actor.Actors
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * @author Vaclav Pech
 */

@Immutable final class WordFindRequest {
    String site
    String word
}

@Immutable final class WordFindResponse {
    String content
    @Delegate WordFindRequest request
}

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

def wordFinder

def contentDownloader = Actors.reactor {
    contentDownloaderArea.report "Downloading ${it.site}"
//    def content = it.site.toURL().text
    def content = defaultSites[it.site]
    sleep 2000
    if (it.site.contains('info')) sleep 10000

    contentDownloaderArea.report "Downloaded"
    new WordFindResponse(content, it)
}

def contentCache = Actors.actor {
    def cache = [:]
    def pendingDownloads = [:]

    loop {
        react {
            switch (it) {
                case WordFindRequest:
                    contentCacheArea.report "Retrieving ${it.site}"
                    sleep 2000
                    def content = cache[it.site]
                    if (content) {
                        contentCacheArea.report "Found in cache"
                        reply new WordFindResponse(content, it)
                    } else {
                        def downloads = pendingDownloads[it.site]
                        if (downloads!=null) {
                            contentCacheArea.report "Awaiting download"
                            downloads << it
                        } else {
                            pendingDownloads[it.site] = []
                            contentCacheArea.report "Asking for download"
                            contentDownloader << it
                        }
                    }
                    break
                case WordFindResponse:
                    contentCacheArea.report "Caching ${it.request.site}"
                    sleep 2000
                    cache[it.request.site] = it.content
                    wordFinder << it
                    def downloads = pendingDownloads[it.site]
                    if (downloads!=null) {
                        for (downloadRequest in downloads) {
                            contentCacheArea.report "Waking up"
                            wordFinder << new WordFindResponse(it.content, downloadRequest)
                        }
                        pendingDownloads.remove(it.site)
                    }
            }
        }
    }
}

wordFinder = Actors.actor {
    loop {
        react {
            switch (it) {
                case Map:
                    wordFinderArea.report "${it.site} requested"
                    sleep 2000
                    contentCache << new WordFindRequest(it.site, it.word)
                    break
                case WordFindResponse:
                    def result = it.content.toUpperCase().contains(it.word.toUpperCase())
                    wordFinderArea.report "${result ? '' : 'No '}${it.request.word} in ${it.request.site}."
            }
        }
    }
}

def createActorPanel = {actorName ->
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
    createActorPanel.delegate = delegate
    vbox {
        hbox {
            wordFinderArea = createActorPanel('Finder')
            contentCacheArea = createActorPanel('Cache')
            contentDownloaderArea = createActorPanel('Downloader')
        }
        vbox {
            hbox {
                textField(id: 'siteName', text: 'http://www.dzone.com')
                button(text: 'Send', actionPerformed: {
                    wordFinder << [site: siteName.text, word: 'GROOVY']
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

