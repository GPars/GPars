// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.activeobject

import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.dataflow.Promise

@ActiveObject
class Decryptor {
    @ActiveMethod
    def decrypt(String encryptedText) {
        return encryptedText.reverse()
    }

    @ActiveMethod
    def decrypt(Integer encryptedNumber) {
        return -1 * encryptedNumber + 142
    }
}

final Decryptor decryptor = new Decryptor()
Promise<String> part1 = decryptor.decrypt(' noitcA ni yvoorG')
Promise<Integer> part2 = decryptor.decrypt(140)
Promise<String> part3 = decryptor.decrypt('noitide dn')

print part1.get()
print part2.get()
println part3.get()


