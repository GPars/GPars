// GPars (formerly GParallelizer)
//
// Copyright © 2008-9  The original author or authors
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

/*
Visual demo of DataFlows that need to processed in a given order -
like for appending - while retrieving the several parts concurrently.
@author Vaclav Pech
@author Dierk Koenig
*/

package groovyx.gpars.samples

import groovy.swing.SwingBuilder
import groovyx.gpars.Parallelizer
import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlows
import java.awt.Color
import static javax.swing.BorderFactory.*
import static javax.swing.WindowConstants.EXIT_ON_CLOSE

def rand = new Random()
def values = (1..5).collect { 1 + rand.nextInt(15) }

final DataFlows retrieved = new DataFlows()
def bars = []
def labels = []

final SwingBuilder builder = new SwingBuilder()
builder.build {
    def frame = builder.frame(title: 'Demo', defaultCloseOperation: EXIT_ON_CLOSE, visible: true, location: [80, 80]) {
        panel(border: createEmptyBorder(10, 10, 10, 10)) {
            gridLayout rows: values.size(), columns: 2, hgap: 10, vgap: 10
            values.eachWithIndex {value, index ->
                bars[index] = progressBar(string: value, minimum: 0, maximum: value, stringPainted: true)
                labels[index] = label()
            }
        }
    }
    frame.pack()
}

def update = { view, text, color ->
    builder.edt {
        view.text = text
        view.background = color
    }
}

DataFlow.task {
    def result = ''
    values.eachWithIndex { value, index ->
        def view = labels[index]
        update view, 'Waiting', Color.red
        def part = retrieved[index]
        update view, 'Appending ' + part, Color.blue
        sleep 1000
        result += part
        update view, result, Color.green
    }
}

Parallelizer.doParallel() {
    values.eachWithIndexParallel { value, index ->
        for (progress in 1..value) {
            sleep 1000
            builder.edt { bars[index].value = progress }
        }
        retrieved[index] = value + " "
    }
}