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
import org.jcsp.awt.ActiveFrame;
import org.jcsp.awt.ActiveLabel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.ChannelInput;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.lang.Parallel;

import java.awt.*;


public class NoServiceClientUserInterface implements CSProcess {
    private ChannelOutput responseEvent;
    private ChannelInput messageConfigure;

    public NoServiceClientUserInterface(ChannelOutput responseEvent, ChannelInput messageConfigure) {
        this.responseEvent = responseEvent;
        this.messageConfigure = messageConfigure;
    }

    public void run() {
        System.out.println("No service client user interface started ");
        final ActiveFrame root = new ActiveFrame(null, responseEvent, "JK Meeting Service");
        root.setSize(320, 480);
        root.setLayout(new BorderLayout());
        final ActiveLabel message = new ActiveLabel(messageConfigure, "                         ");
        final ActiveButton retry = new ActiveButton(null, responseEvent, "Retry");
        final ActiveButton close = new ActiveButton(null, responseEvent, "Close");
        root.add(message, BorderLayout.NORTH);
        root.add(retry, BorderLayout.CENTER);
        root.add(close, BorderLayout.SOUTH);
        root.pack();
        System.out.println("NSCUI:  packed");
        root.setVisible(true);
        System.out.println("NSCUI:  setvisible");
        final CSProcess[] network = {
                message,
                retry,
                close
        };
        System.out.println("NSCUI:  about to run");
        new Parallel(network).run();
    }
}