// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
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

package c17.test

import org.jcsp.lang.*

class GenerateNumbers implements CSProcess {
    def delay = 1000
    def iterations = 20
    def ChannelOutput outChannel

    def generatedList = []

    void run() {
        println "Numbers started"
        def timer = new CSTimer()
        for (i in 1..iterations) {
            //println "Generated ${i}"
            outChannel.write(i)
            generatedList << i
            timer.sleep(delay)
        }
        println "Numbers finished"
        //println"Generated: ${generatedList}"
    }
}