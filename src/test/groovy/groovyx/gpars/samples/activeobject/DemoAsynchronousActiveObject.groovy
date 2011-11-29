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
import groovyx.gpars.dataflow.DataflowVariable

/**
 * The demo shows that DataflowVariables returned from active methods are composed.
 */

@ActiveObject
class AsyncDecryptor {
    @ActiveMethod
    DataflowVariable<String> decrypt(String encryptedText) {
        return new DataflowVariable() << encryptedText.reverse()
    }

    @ActiveMethod
    DataflowVariable<Integer> decrypt(Integer encryptedNumber) {
        return new DataflowVariable() << -1 * encryptedNumber + 142
    }
}

final AsyncDecryptor decryptor = new AsyncDecryptor()
def firstPart = decryptor.decrypt(' noitcA ni yvoorG')
def secondPart = decryptor.decrypt(140)
def thirdPart = decryptor.decrypt('noitide dn')

print firstPart.get()
print secondPart.get()
println thirdPart.get()

