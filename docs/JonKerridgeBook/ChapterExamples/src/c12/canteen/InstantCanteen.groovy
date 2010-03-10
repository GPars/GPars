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

package c12.canteen

import org.jcsp.lang.*
import org.jcsp.groovy.*

class InstantCanteen implements CSProcess {

    def ChannelInput service
    def ChannelOutput deliver
    def ChannelInput supply
    def ChannelOutput toConsole

    def void run() {

        def canteenAlt = new ALT([supply, service])

        def SUPPLY = 0
        def SERVICE = 1

        def tim = new CSTimer()
        def chickens = 0

        toConsole.write("Canteen : starting ... \n")
        while (true) {
            switch (canteenAlt.fairSelect()) {
                case SUPPLY:
                    def value = supply.read()
                    toConsole.write("Chickens on the way ...\n")
                    tim.after(tim.read() + 3000)
                    chickens = chickens + value
                    toConsole.write("${chickens} chickens now available ...\n")
                    supply.read()
                    break
                case SERVICE:
                    def id = service.read()
                    if (chickens > 0) {
                        chickens = chickens - 1
                        toConsole.write("chicken ready for Philosoper ${id} ... ${chickens} chickens left \n")
                        deliver.write(1)
                    }
                    else {
                        toConsole.write(" NO chickens left ... \n")
                        deliver.write(0)
                    }
                    break
            }
        }
    }
}
