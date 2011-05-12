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

import groovyx.gpars.dataflow.DataflowVariable
import java.util.concurrent.Executors

final many = 1..(100)

List dfs = many.collect { new DataflowVariable() }
def result = new DataflowVariable()

def scheduler = Executors.newFixedThreadPool(20)

scheduler.execute { result << dfs.sum { it.val } }

dfs.each {df ->
    scheduler.execute { df << 1 }
}

assert many.size() == result.val

scheduler.shutdown()
