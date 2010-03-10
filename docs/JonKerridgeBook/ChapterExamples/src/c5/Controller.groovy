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

package c5

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Controller implements CSProcess {
    def long testInterval = 5000
    def long computeInterval = 700
    def int addition = 1
    def ChannelInput factor
    def ChannelOutput suspend
    def ChannelOutput injector

    void run() {
        def currentFactor = 0
        def timer = new CSTimer()
        def timeout = timer.read()                   // get current time
        while (true) {
            timeout = timeout + testInterval           // set the timeout
            timer.after(timeout)                    // wait for the timeout
            suspend.write(0)                          // suspend signal to ScaleInt; value irrelevant
            currentFactor = factor.read()              // get current scaling from ScaleInt
            currentFactor = currentFactor + addition   // compute new factor
            timer.sleep(computeInterval)               // to simulate computational time
            injector.write(currentFactor)          // send new scale factor to Scale
        }
    }
}
