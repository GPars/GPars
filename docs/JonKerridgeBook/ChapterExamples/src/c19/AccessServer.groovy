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

import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.mobile.*
import org.jcsp.groovy.*
import phw.util.*

class AccessServer implements CSProcess {

    def ChannelInput fromAccessSender

    void run() {
        def theServer = new MultiMobileProcessServer("A", fromAccessSender)
        //theServer.init("A", fromAccessSender)
        println " AccessServer: about to run"
        new PAR([theServer]).run()
    }
}
