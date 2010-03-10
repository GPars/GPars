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

package c11

import org.jcsp.groovy.*
import org.jcsp.lang.*
import phw.util.*
import org.jcsp.util.*
import org.jcsp.awt.*

class ParticleInterface implements CSProcess {
    def ChannelInput inChannel
    def ChannelOutput outChannel
    def int canvasSize = 100
    def int particles
    def int centre
    def int initialTemp

    void run() {
        def dList = new DisplayList()
        def particleCanvas = new ActiveCanvas()
        particleCanvas.setPaintable(dList)
        def tempConfig = Channel.createOne2One()
        def pauseConfig = Channel.createOne2One()
        def uiEvents = Channel.createAny2One(new OverWriteOldestBuffer(5))
        def network = [new ParticleManager(fromParticles: inChannel,
                toParticles: outChannel,
                toUI: dList,
                fromUIButtons: uiEvents.in(),
                toUIPause: pauseConfig.out(),
                toUILabel: tempConfig.out(),
                CANVASSIZE: canvasSize,
                PARTICLES: particles,
                CENTRE: centre,
                START_TEMP: initialTemp),
                new UserInterface(particleCanvas: particleCanvas,
                        canvasSize: canvasSize,
                        tempValueConfig: tempConfig.in(),
                        pauseButtonConfig: pauseConfig.in(),
                        buttonEvent: uiEvents.out())
        ]
        new PAR(network).run()
    }
}
   