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

package groovyx.gpars.samples.collections

import groovy.swing.SwingBuilder
import javax.swing.JFrame

/**
 * A simple mashup sample, downloads content of three websites and checks how many of them refer to Groovy.
 */
final List urls = [
        'http://www.dzone.com',
        'http://www.jroller.com',
        'http://www.theserverside.com',
        'http://www.jetbrains.com',
        'http://www.infoq.com',
        'http://groovy.codehaus.org',
        'http://griffon.codehaus.org',
        'http://grails.codehaus.org']

final def frame = new SwingBuilder().frame(title: 'Demo', defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
    vbox() {
        panel() {
            gridLayout()
            label 'Sites:'
        }
        scrollPane() {
            textArea(columns: 80, rows: 20, id: 'result')
        }
        panel() {
            gridLayout()
            label 'Log:'
        }
        scrollPane() {
            textArea(columns: 80, rows: 25, id: 'logMessages')
        }
        hbox {
            button('Search for Groovy', mnemonic: 'S', actionPerformed: {
                doOutside {
                    urls.each {url ->
                        edt {logMessages.text += "Started downloading from $url \n"}
                        def content = url.toURL().text
                        edt {logMessages.text += "Done downloading from $url \n"}
                        if (content.toUpperCase().contains('GROOVY'))
                            edt {
                                result.text += "A groovy site found: ${url} \n"
                            }
                    }
                }
            })
        }
    }
}

frame.visible = true
frame.pack()

