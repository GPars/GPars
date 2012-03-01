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

/**
 * Measures time to access a web service repeatedly sequentially and concurrently.
 */

//@Grab(group='org.codehaus.groovy.modules', module='groovyws', version='0.5.2')
//import groovyx.net.ws.WSClient
//
//proxy = new WSClient("http://www.w3schools.com/webservices/tempconvert.asmx?WSDL", this.class.classLoader)
//proxy.initialize()
//
//result = proxy.CelsiusToFahrenheit(0)
//println "You are probably freezing at ${result} degrees Farhenheit"
//
//final temperatures = [-20, -10, 0, 10, 20, 30]
//
//final t1 = System.currentTimeMillis()
//temperatures.collect {[it, proxy.CelsiusToFahrenheit(it)]}.each {
//    println "${it[0]} Celsius is ${it[1]} Fahrenheit"
//}
//println "Sequential Time: ${System.currentTimeMillis() - t1}"
//
//final t2 = System.currentTimeMillis()
//groovyx.gpars.GParsPool.withPool {
//    temperatures.collectParallel {[it, proxy.CelsiusToFahrenheit(it)]}.each {
//        println "${it[0]} Celsius is ${it[1]} Fahrenheit"
//    }
//}
println "Concurrent Time: ${System.currentTimeMillis() - t2}"