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
import org.jcsp.awt.*
import org.jcsp.groovy.*
import java.awt.*

class Gallery implements CSProcess {
    def ActiveCanvas targetCanvas
    def ChannelInput hitsFromGallery
    def ChannelInput possiblesFromGallery
    def ChannelOutput mouseEvent

    def canvasSize = 450

    void run() {
        def root = new ActiveClosingFrame("Hand-Eye Co-ordination Test")
        def mainFrame = root.getActiveFrame()
        def m1 = new Label("You Have Hit")
        def m2 = new Label("Out Of")
        def hitLabel = new ActiveLabel(hitsFromGallery)
        def possLabel = new ActiveLabel(possiblesFromGallery)
        m1.setAlignment(Label.CENTER)
        m2.setAlignment(Label.CENTER)
        hitLabel.setAlignment(Label.CENTER)
        possLabel.setAlignment(Label.CENTER)
        m1.setFont(new Font("sans-serif", Font.BOLD, 14))
        m2.setFont(new Font("sans-serif", Font.BOLD, 14))
        hitLabel.setFont(new Font("sans-serif", Font.BOLD, 20))
        possLabel.setFont(new Font("sans-serif", Font.BOLD, 20))
        def message = new Container()
        message.setLayout(new GridLayout(1, 4))
        message.add(m1)
        message.add(hitLabel)
        message.add(m2)
        message.add(possLabel)
        targetCanvas.addMouseEventChannel(mouseEvent)
        mainFrame.setLayout(new BorderLayout())
        targetCanvas.setSize(canvasSize, canvasSize)
        mainFrame.add(targetCanvas, BorderLayout.CENTER)
        mainFrame.add(message, BorderLayout.SOUTH)
        mainFrame.pack()
        mainFrame.setVisible(true)
        def network = [root, targetCanvas, hitLabel, possLabel]
        new PAR(network).run()
    }

}