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

import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.Parallel;
import org.jcsp.net.mobile.*;
import org.jcsp.util.OverWriteOldestBuffer;

public class NoServiceClientProcess extends MobileProcess {

    static final long serialVersionUID = 9;

    public void run() {
        System.out.println("No service client process started ");
        final Any2OneChannel responseEvent = Channel.createAny2One(new OverWriteOldestBuffer(10));

        final One2OneChannel messageConfigure = Channel.createOne2One(new OverWriteOldestBuffer(10));

        final CSProcess[] network = {
                new NoServiceClientCapability(
                        responseEvent.in(),
                        messageConfigure.out()),
                new NoServiceClientUserInterface(
                        responseEvent.out(),
                        messageConfigure.in())
        };
        new Parallel(network).run();
    }
}

