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

package c20

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Queue implements CSProcess {

    def ChannelInput fromElement
    def ChannelInput fromPrompter
    def ChannelOutput toStateManager
    def ChannelOutput toPrompter
    def int slots

    void run() {
        def qAlt = new ALT([fromElement, fromPrompter])
        def preCon = new boolean[2]
        def ELEMENT = 0
        def PROMPT = 1
        preCon[ELEMENT] = true
        preCon[PROMPT] = false
        def data = []
        def counter = 0
        def front = 0
        def rear = 0
        while (true) {
            def index = qAlt.priSelect(preCon)
            switch (index) {
                case ELEMENT:
                    data[front] = fromElement.read()
                    front = (front + 1) % slots
                    counter = counter + 1
                    toStateManager.write(counter)
                    break
                case PROMPT:
                    fromPrompter.read()
                    toPrompter.write(data[rear])
                    rear = (rear + 1) % slots
                    counter = counter - 1
                    toStateManager.write(counter)
                    break
            }
            preCon[ELEMENT] = (counter < slots)
            preCon[PROMPT] = (counter > 0)
        }
    }

}