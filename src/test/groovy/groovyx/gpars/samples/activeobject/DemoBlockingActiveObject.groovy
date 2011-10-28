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

@ActiveObject
class BlockingDecryptor {
    @ActiveMethod(blocking = true)
    String decrypt(String encryptedText) {
        encryptedText.reverse()
    }

    @ActiveMethod(blocking = true)
    Integer decrypt(Integer encryptedNumber) {
        -1 * encryptedNumber + 142
    }
}

final BlockingDecryptor decryptor = new BlockingDecryptor()
print decryptor.decrypt(' noitcA ni yvoorG')
print decryptor.decrypt(140)
println decryptor.decrypt('noitide dn')