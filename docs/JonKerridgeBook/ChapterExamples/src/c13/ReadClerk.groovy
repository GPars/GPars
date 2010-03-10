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

package c13

import org.jcsp.lang.*
import org.jcsp.groovy.*

class ReadClerk implements CSProcess {
    def ChannelInput cin
    def ChannelOutput cout
    def CrewMap data

    void run() {
        println "ReadClerk has started "
        while (true) {
            def d = new DataObject()
            d = cin.read()
            d.value = data.get(d.location)
            println "RC: Reader ${d.pid} has read ${d.value} from ${d.location}"
            cout.write(d)
        }
    }
}

  