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

import org.jcsp.awt.*
import java.awt.*

class ParticleManager implements CSProcess {
    def ChannelInput fromParticles
    def ChannelOutput toParticles
    def DisplayList toUI
    def int CANVAS_SIZE
    def int PARTICLES
    def int CENTRE
    def int START_TEMP
    def ChannelInput fromUIButtons
    def ChannelOutput toUILabel
    def ChannelOutput toUIPause

    void run() {
        def colourList = [Color.BLUE, Color.GREEN,
                Color.RED, Color.MAGENTA,
                Color.CYAN, Color.YELLOW]

        def temperature = START_TEMP

        GraphicsCommand[] particleGraphics = new GraphicsCommand[1 + (PARTICLES * 2)]

        particleGraphics[0] = new GraphicsCommand.ClearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE)

        GraphicsCommand[] initialGraphic = new GraphicsCommand[2]

        initialGraphic[0] = new GraphicsCommand.SetColor(Color.BLACK)
        initialGraphic[1] = new GraphicsCommand.FillOval(CENTRE, CENTRE, 10, 10)

        for (i in 0..<PARTICLES) {
            def p = (i * 2) + 1
            for (j in 0..<2) {
                particleGraphics[p + j] = initialGraphic[j]
            }
        }

        toUI.set(particleGraphics)
        GraphicsCommand[] positionGraphic = new GraphicsCommand[2]
        positionGraphic =
            [new GraphicsCommand.SetColor(Color.WHITE),
                    new GraphicsCommand.FillOval(CENTRE, CENTRE, 10, 10)
            ]

        def pmAlt = new ALT([fromUIButtons, fromParticles])

        def initTemp = " " + temperature + " "
        toUILabel.write(initTemp)

        def direction = fromUIButtons.read()
        while (direction != "START") {
            direction = fromUIButtons.read()
        }
        toUIPause.write("PAUSE")

        while (true) {
            def index = pmAlt.priSelect()
            if (index == 0) {        // dealing with a button event
                direction = fromUIButtons.read()
                if (direction == "PAUSE") {
                    toUIPause.write("RESTART")
                    direction = fromUIButtons.read()
                    while (direction != "RESTART") {
                        direction = fromUIButtons.read()
                    }
                    toUIPause.write("PAUSE")
                }
                else {
                    if ((direction == "Up") && (temperature < 50)) {
                        temperature = temperature + 5
                        def s = "+" + temperature + "+"
                        toUILabel.write(s)
                    }
                    else {
                        if ((direction == "Down") && (temperature > 10)) {
                            temperature = temperature - 5
                            def s = "-" + temperature + "-"
                            toUILabel.write(s)
                        }
                        else {
                        }
                    }
                }
            }
            else {    // index is 1 particle movement
                def p = (Position) fromParticles.read()
                // make sure particle stays within bounds of canvas by bouncing off the boundary of the canvas
                if (p.lx > CANVAS_SIZE) { p.lx = (2 * CANVAS_SIZE) - p.lx }
                if (p.ly > CANVAS_SIZE) { p.ly = (2 * CANVAS_SIZE) - p.ly }
                if (p.lx < 0) { p.lx = 0 - p.lx }
                if (p.ly < 0) { p.ly = 0 - p.ly }
                // now change positionGraphic
                positionGraphic[0] = new GraphicsCommand.SetColor(colourList.getAt(p.id % 6))
                positionGraphic[1] = new GraphicsCommand.FillOval(p.lx, p.ly, 10, 10)
                // now modify the DisplayList toUI
                toUI.change(positionGraphic, 1 + (p.id * 2))
                // modify px, py
                p.px = p.lx
                p.py = p.ly
                p.temperature = temperature
                toParticles.write(p)
            } // index test
        } // while
    } // run
}                   