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

package c14

import org.jcsp.lang.*
import org.jcsp.groovy.*
import java.awt.Point
import java.awt.event.*

class MouseBufferPreCon implements CSProcess {
    def ChannelInput mouseEvent
    def ChannelInput getClick
    def ChannelOutput sendPoint

    void run() {
        def mouseBufferAlt = new ALT([getClick, mouseEvent])
        def preCon = new boolean[2]
        def EVENT = 1
        def GET = 0
        preCon[EVENT] = true
        preCon[GET] = false
        def point
        while (true) {
            switch (mouseBufferAlt.select(preCon)) {
                case GET:
                    getClick.read()
                    sendPoint.write(point)
                    preCon[GET] = false
                    break
                case EVENT:
                    def mEvent = mouseEvent.read()
                    if (mEvent.getID() == MouseEvent.MOUSE_PRESSED) {
                        preCon[GET] = true
                        point = mEvent.getPoint()
                    }
                    break
            }
        }
    }
}