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
import org.jcsp.awt.*
import java.awt.*

class UserInterface implements CSProcess {
    def ActiveCanvas particleCanvas
    def int canvasSize
    def ChannelInput tempValueConfig
    def ChannelInput pauseButtonConfig
    def ChannelOutput buttonEvent

    void run() {
        def root = new ActiveClosingFrame("Brownian Motion Particle System")
        def mainFrame = root.getActiveFrame()
        def tempLabel = new Label("Temperature")
        def tempValue = new ActiveLabel(tempValueConfig)
        tempValue.setAlignment(Label.CENTER)
        def upButton = new ActiveButton(null, buttonEvent, "Up")
        def downButton = new ActiveButton(null, buttonEvent, "Down")
        def pauseButton = new ActiveButton(pauseButtonConfig, buttonEvent, "START")
        def tempContainer = new Container()
        tempContainer.setLayout(new GridLayout(1, 5))
        tempContainer.add(pauseButton)
        tempContainer.add(tempLabel)
        tempContainer.add(upButton)
        tempContainer.add(tempValue)
        tempContainer.add(downButton)
        mainFrame.setLayout(new BorderLayout())
        particleCanvas.setSize(canvasSize, canvasSize)
        mainFrame.add(particleCanvas, BorderLayout.CENTER)
        mainFrame.add(tempContainer, BorderLayout.SOUTH)
        mainFrame.pack()
        mainFrame.setVisible(true)
        def network = [root, particleCanvas, tempValue, upButton, downButton, pauseButton]
        new PAR(network).run()
    }
}

    
    