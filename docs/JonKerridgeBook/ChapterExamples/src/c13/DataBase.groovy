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

class DataBase implements CSProcess {
    def inChannels
    def outChannels
    def int readers
    def int writers

    void run() {
        println "DataBase has started"
        def crewDataBase = new CrewMap()
        for (i in 0..<10) {
            crewDataBase.put(i, 100 + i)
        }
        for (i in 0..<10) {
            println "DB: Location ${i} contains ${crewDataBase.get(i)} "
        }
        def processList = []
        for (i in 0..<readers) {
            processList.putAt(i, new ReadClerk(cin: inChannels[i],
                    cout: outChannels[i],
                    data: crewDataBase))
        }
        for (i in 0..<writers) {
            processList.putAt((i + readers), new WriteClerk(cin: inChannels[i + readers],
                    cout: outChannels[i + readers],
                    data: crewDataBase))
        }
        new PAR(processList).run()
    }
}

    
      