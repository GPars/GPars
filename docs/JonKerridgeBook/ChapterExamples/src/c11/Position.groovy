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

class Position implements JCSPCopy {
    def int id            // particle number
    def int lx            // current x location of particle
    def int ly            // current y location of particle
    def int px            // previous x location of particle
    def int py            // previous y location of particle
    def int temperature   // current working temperature

    def copy() {
        def p = new Position(id: this.id,
                lx: this.lx, ly: this.ly,
                px: this.px, py: this.py,
                temperature: this.temperature)
        return p
    }

    def String toString() {
        def s = "[Position-> " + id + ", " + lx + ", " + ly
        s = s + ", " + px + ", " + py
        s = s + ", " + temperature + " ]"
        return s
    }
}
