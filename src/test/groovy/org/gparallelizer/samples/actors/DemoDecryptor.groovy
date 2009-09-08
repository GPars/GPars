//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.samples.actors

import java.util.concurrent.CountDownLatch
import static org.gparallelizer.actors.pooledActors.PooledActors.actor

/**
 * A demo showing two cooperating actors. The decryptor decrypts received messages and replies them back.
 * The console actor sends a message to decrypt, prints out the reply and terminates both actors.
 * The main thread waits on both actors to finish using the join() method to prevent premature exit,
 * since both actors use the default pooled actor group,  which uses a daemon thread pool.
 * @author Dierk , Koenig, Vaclav Pech
 */

def decryptor = actor {
    loop {
        react {message ->
            if (message instanceof String) reply message.reverse()
            else stop()
        }
    }
}.start()

def console = actor {
    decryptor.send 'suonorhcnysa si yvoorG'
    react {
        println 'Decrypted message: ' + it
        decryptor.send false
    }
}.start()

[decryptor, console]*.join()
