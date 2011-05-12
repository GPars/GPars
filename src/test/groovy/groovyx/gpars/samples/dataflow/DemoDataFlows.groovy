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
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Demonstrates the use of the Dataflows class to exchange values among threads or tasks.
 *
 * @author Vaclav Pech, Dierk Koenig
 */

final df = new Dataflows()

task { df.result = df.x + df.y }

task { df.x = 10 }

task { df.y = 5 }

assert 15 == df.result

task { //noinspection GroovyAssignmentCanBeOperatorAssignment
    df[0] = df[2] + df[1]
}

task { df[1] = 10 }

task { df[2] = 5 }

assert 15 == df[0]
