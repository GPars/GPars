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

package groovyx.gpars.samples.collections

import groovy.swing.SwingBuilder
import groovyx.gpars.GParsPool

import javax.swing.*

/**
 * @author Vaclav Pech
 */

class FileProcessor {
    private static final Random random = new Random()
    public static final MAX_WORDS = 10000

    String name
    JSlider slider

    public void perform() {
        final int wordCount = random.nextInt(MAX_WORDS)
        for (counter in (1..wordCount)) {
            if (counter % 100 == 0) {
                SwingUtilities.invokeLater { (slider.value = (int) (counter / 100)) }
//                Thread.sleep 100
                1.upto(5000) { new Random().nextInt(30) }
            }
        }
    }
}

def processors = (1..10).collect {
    final JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, (int) (FileProcessor.MAX_WORDS / 100), 0)
    new FileProcessor(name: 'File' + it, slider: slider)
}

final JFrame frame = new SwingBuilder().frame(title: 'GParsPool Demo', defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
    vbox() {
        panel {
            vbox() {
                processors.each { processor ->
                    panel {
                        hbox {
                            label(text: bind(source: processor.slider, sourceProperty: 'value', converter: {
                                "Current value: $it"
                            }))
                            def currentPanel = panel()
                            currentPanel.add processor.slider
                        }
                    }
                }
            }
        }
        panel {
            vbox {
                panel {
                    hbox {
                        buttonGroup().with {
                            add radioButton(text: 'Single threaded', id: 'singleThreaded', selected: true)
                            add radioButton(text: 'Multi threaded', id: 'multiThreaded')
                        }
                    }
                }
                panel {
                    hbox() {
                        button(text: 'Start', actionPerformed: {
                            doOutside {
                                if (singleThreaded.isSelected()) {
                                    processors.each {
                                        it.perform()
                                    }
                                } else {
                                    GParsPool.withPool {
                                        processors.eachParallel {
                                            it.perform()
                                        }
                                    }
                                }
                            }
                        })
                        button(text: 'Clear', actionPerformed: {
                            processors.each { it.slider.value = 0 }
                        })
                    }
                }
            }
        }
    }
}

frame.visible = true
frame.pack()
