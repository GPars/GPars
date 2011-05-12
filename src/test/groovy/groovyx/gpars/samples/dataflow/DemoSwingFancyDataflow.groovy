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
import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.Dataflows
import java.awt.Color
import java.awt.GridLayout
import javax.swing.JFrame

/**
 * Visually shows the effect of having code run in parallel. Multiple functions are calculated at the same time while their
 * results are accumulated sequentially as the results become available.
 *
 * @author Vaclav Pech, Dierk Koenig
 */

def values = [1, 2, 3, 4, 5, 6, 7, 8, 9]
final Dataflows df = new Dataflows()
final SwingBuilder builder = new SwingBuilder()

builder.build {
    final JFrame frame = builder.frame(title: 'Demo', defaultCloseOperation: JFrame.EXIT_ON_CLOSE, visible: true) {
        panel(layout: new GridLayout(values.size(), 2)) {
            values.eachWithIndex {value, index ->
                button('Undefined', opaque: true, id: 'x' + index)
                button('Accumulated summary not known', opaque: true, id: 'y' + index)
            }
        }
    }
    frame.pack()
}

Dataflow.task {
    int sum = 0
    values.eachWithIndex {value, index ->
        builder.edt {
            builder."y$index".text = 'Waiting'
            builder."y$index".background = Color.red
        }
        def loadedValue = df."$index"
        builder.edt {
            builder."y$index".text = 'Processing ' + loadedValue
            builder."y$index".background = Color.blue
        }
        sleep 2000
        sum += loadedValue
        builder.edt {
            builder."y$index".text = 'Accumulated summary = ' + sum
            builder."y$index".background = Color.green
        }
    }
}

values.eachWithIndex {value, index ->
    df."$index" {newValue ->
        builder.edt {
            builder."x$index".text = newValue
            builder."x$index".background = Color.green
        }
    }
}

random = new Random()

GParsPool.withPool(3) {
    values.eachWithIndexParallel {value, index ->
        builder.edt {
            builder."x$index".text = 'Calculating'
            builder."x$index".background = Color.blue
        }

        df."$index" = func(value)
    }
}

private def func(value) {
    sleep random.nextInt(9000 + value * 1000)
    factorial value
}

def factorial(value) {
    (1..value).toList().inject(1) {partial, num -> partial * num}
}
