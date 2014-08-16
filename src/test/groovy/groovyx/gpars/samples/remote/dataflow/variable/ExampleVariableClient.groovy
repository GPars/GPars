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

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9009

println "Example: DataflowVariable"

def remoteDataflows = RemoteDataflows.create()

def var1Promise = remoteDataflows.getVariable HOST, PORT, "variabledemo-var-GPars"
def var2Promise = remoteDataflows.getVariable HOST, PORT, "variabledemo-var-Dataflows"

def var1 = var1Promise.get()
def var2 = var2Promise.get()

println "${var1.val} supports ${var2.val}"
