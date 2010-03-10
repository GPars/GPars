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

class Read implements CSProcess {

    def ChannelOutput r2db
    def ChannelInput db2r
    def int id
    def ChannelOutput toConsole

    void run() {
        def timer = new CSTimer()
        toConsole.write("Reader has started \n")
        for (i in 0..<10) {
            def d = new DataObject(pid: id)
            d.location = i
            r2db.write(d)
            d = db2r.read()
            toConsole.write("Location " + d.location + " has value " + d.value + "\n")
            timer.sleep(100)
        }
        toConsole.write("Reader has finished \n")
    }
}
