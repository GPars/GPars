package groovyx.gpars.samples.actors

import groovy.swing.SwingBuilder
import groovyx.gpars.actor.Actors
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JTextArea
import javax.swing.SwingUtilities

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
    Thread.sleep 2000

    contentDownloaderArea.report "Downloaded"
    new WordFindResponse(content, it)
}.start()

def contentCache = Actors.actor {
    def cache = [:]
    def pendingDownloads = [:]

    loop {
        react {
            switch (it) {
                case WordFindRequest:
                    contentCacheArea.report "Retrieving ${it.site}"
                    Thread.sleep 2000
                    def content = cache[it.site]
                    if (content) {
                        contentCacheArea.report "Found in cache"
                        it.reply new WordFindResponse(content, it)
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
                    Thread.sleep 2000
                    cache[it.request.site] = it.content
                    wordFinder << it
                    def downloads = pendingDownloads[it.site]
                    if (downloads!=null) {
                        for (downloadRequest in downloads) {
                            contentCacheArea.report "Waking up"
                            //todo output, dummy, it x msg 
                            wordFinder << new WordFindResponse(it.content, downloadRequest)
                        }
                        pendingDownloads.remove(it.site)
                    }
            }
        }
    }
}.start()

wordFinder = Actors.actor {
    loop {
        react {
            switch (it) {
                case Map:
                    wordFinderArea.report "${it.site} requested"
                    Thread.sleep 2000
                    contentCache << new WordFindRequest(it.site, it.word)
                    break
                case WordFindResponse:
                    def result = it.content.toUpperCase().contains(it.word.toUpperCase())
                    wordFinderArea.report "${result ? '' : 'No '}${it.request.word} in ${it.request.site}."
            }
        }
    }
}.start()

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

