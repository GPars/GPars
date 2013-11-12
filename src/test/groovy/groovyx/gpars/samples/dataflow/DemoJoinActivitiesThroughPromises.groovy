// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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

import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Demonstrates the thenForkAndJoin() capability of promises. The thenForkAndJoin() method triggers multiple 'then' handlers,
 * once a promise they wait for has been bound. The method returns a promise eventually containing a list of results of all the parallel 'then' handlers.
 * in a list to the next
 */
task {
    2
}.thenForkAndJoin({ it ** 2 }, { it ** 3 }, { it ** 4 }, { it ** 5 }).then({ println it }).join()