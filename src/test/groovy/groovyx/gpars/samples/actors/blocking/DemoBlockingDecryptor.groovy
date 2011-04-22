// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.samples.actors.blocking

import static groovyx.gpars.actor.Actors.blockingActor

/**
 * A demo showing two cooperating actors. The decryptor decrypts received messages and replies them back.
 * The console actor sends a message to decrypt, prints out the reply and terminates both actors.
 * The main thread waits on both actors to finish using the join() method to prevent premature exit,
 * since both actors use the default pooled actor group,  which uses a daemon thread pool.
 * @author Vaclav Pech
 */

def decryptor = blockingActor {
    while (true) {
        receive {message ->
            if (message instanceof String) reply message.reverse()
            else stop()
        }
    }
}

def console = blockingActor {
    decryptor.send 'lellarap si yvoorG'
    println 'Decrypted message: ' + receive()
    decryptor.send false
}

[decryptor, console]*.join()
