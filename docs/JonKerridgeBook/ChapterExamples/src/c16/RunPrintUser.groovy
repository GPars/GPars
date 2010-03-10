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

package c16

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

def user = Ask.Int("User Number ? ", 1, 999)

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory())

def pRequest = CNS.createAny2Net("REQUEST")
def pRelease = CNS.createAny2Net("RELEASE")

new PAR([new PrintUser(printerRequest: pRequest,
        printerRelease: pRelease,
        userId: user
)
]).run()

