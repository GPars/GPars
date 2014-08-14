// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
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

package groovyx.gpars.samples.remote.dataflow.variable

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9009

println "Example: DataflowVariable"

def remoteDataflows = RemoteDataflows.create()
remoteDataflows.startServer HOST, PORT

def var1 = new DataflowVariable()
def var2 = new DataflowVariable()

remoteDataflows.publish var1, "variabledemo-var-GPars"
remoteDataflows.publish var2, "variabledemo-var-Dataflows"

println "Before bind"
sleep 10000
var1 << "GPars"
var2 << "Remote Dataflows"
println "After bind"
