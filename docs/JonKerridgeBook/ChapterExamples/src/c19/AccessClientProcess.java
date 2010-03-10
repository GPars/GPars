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
import org.jcsp.lang.Parallel;
import org.jcsp.net.mobile.*;


public class AccessClientProcess extends MobileProcess {

    static final long serialVersionUID = 9;

    public void run() {
        System.out.println("MeetingAccessClient has started");
        final Any2OneChannel events = Channel.createAny2One();
        final CSProcess[] network = {
                new AccessClientCapability(events.in()),
                new AccessClientUserInterface(events.out())
        };
        new Parallel(network).run();
    }
}
