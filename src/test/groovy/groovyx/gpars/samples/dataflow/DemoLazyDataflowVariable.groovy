// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

import groovyx.gpars.dataflow.LazyDataflowVariable

Closure<String> download = { url ->
    println "Downloading"
    url.toURL().text
}

def pageContent = new LazyDataflowVariable(download.curry("http://gpars.codehaus.org"))

println "No-one has asked for the value just yet. Bound = ${pageContent.bound}"
sleep 1000
println "Now going to ask for a value"
println pageContent.get().size()
println "Repetitive requests will receive the already calculated value. No additional downloading."
println pageContent.get().size()