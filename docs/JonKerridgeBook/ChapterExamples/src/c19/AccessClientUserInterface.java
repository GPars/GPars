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

package c19;

import org.jcsp.awt.ActiveButton;
import org.jcsp.awt.ActiveClosingFrame;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.lang.Parallel;

import java.awt.*;


/**
 * @author JM Kerridge
 */
public class AccessClientUserInterface implements CSProcess {
    private ChannelOutput buttonEvent;

    /**
     * @param buttonEvent
     */
    public AccessClientUserInterface(ChannelOutput buttonEvent) {
        this.buttonEvent = buttonEvent;
    }

    public void run() {
        final ActiveClosingFrame root = new ActiveClosingFrame("Jon's Meeting Service");
        final Frame mainFrame = root.getActiveFrame();
        mainFrame.setSize(320, 480);
        mainFrame.setLayout(new BorderLayout());
        final ActiveButton newButton = new ActiveButton(null, buttonEvent, "Create New Meeting");
        final ActiveButton findButton = new ActiveButton(null, buttonEvent, "Find Existing Meeting");
        final Container buttonContainer = new Container();
        buttonContainer.setSize(320, 480);
        buttonContainer.setLayout(new GridLayout(2, 1));
        buttonContainer.add(newButton);
        buttonContainer.add(findButton);
        mainFrame.add(buttonContainer, BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setVisible(true);
        final CSProcess[] network = {
                root,
                newButton,
                findButton
        };
        System.out.println("MeetingAccessFrame about to invoke PAR");
        new Parallel(network).run();
    }
}

 
