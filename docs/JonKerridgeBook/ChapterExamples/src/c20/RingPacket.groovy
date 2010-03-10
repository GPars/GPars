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

package c20

import org.jcsp.lang.*
import org.jcsp.groovy.*

class RingPacket implements Serializable, JCSPCopy {
    def int source
    def int destination
    def int value
    def boolean full

    def copy() {
        def p = new RingPacket(source: this.source,
                destination: this.destination,
                value: this.value,
                full: this.full)
        return p
    }

    def String toString() {
        def s = "Packet [ s: ${source}, d: ${destination}, v: ${value}, f: ${full} ] "
        return s
    }
}
