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

import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.group.DefaultPGroup

/**
 * Shows a method calculating project duration and cost estimate done concurrently using dataflow tasks and variables.
 *
 * @author Vaclav Pech
 */
def log(text) {println text}

public Map calculateProjectDuration(int numOfEntities) {
    def df = new Dataflows()
    final def group = new DefaultPGroup(4)

    try {
        group.with {
            task {
                log 'Calculating total project estimate'
                df.durationEstimate = [df.dbaEstimate, df.uiEstimate].max() + 1
            }
            task {
                log 'Calculating db admin time'
                df.dbaEstimate = (numOfEntities * 3 / 20)
            }
            task {
                log 'Calculating UI designer time'
                df.uiEstimate = ((5 + numOfEntities) * 3 / 5)
            }
            task {
                log 'Calculating the cost'
                df.costEstimate = 500 + numOfEntities * 9
            }
        }
    } finally {
        group.shutdown()
    }

    return [cost: df.costEstimate, duration: df.durationEstimate]
}

println calculateProjectDuration(10)