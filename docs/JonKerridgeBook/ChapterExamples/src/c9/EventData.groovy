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

package c9

import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventData implements Serializable, JCSPCopy {

    def int source = 0
    def int data = 0
    def int missed = -1

    def copy() {
        def e = new EventData(source: this.source,
                data: this.data,
                missed: this.missed)
        return e
    }

    def String toString() {
        def s = "EventData -> [source: "
        s = s + source + ", data: "
        s = s + data + ", missed: "
        s = s + missed + "]"
        return s
    }

}


