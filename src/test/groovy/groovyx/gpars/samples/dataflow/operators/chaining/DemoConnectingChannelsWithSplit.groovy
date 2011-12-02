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

package groovyx.gpars.samples.dataflow.operators.chaining

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * The chainWith() method available on all channels allows you to build pipe-lines off the original channel.
 * The split() method than lets you attach the output of the pipeline to several other channels/pipelines.
 *
 * @author Vaclav Pech
 */

def toUpperCase = {s -> s.toUpperCase()}

final DataflowReadChannel encrypt = new DataflowQueue()
final DataflowWriteChannel messagesToSave = new DataflowQueue()
final DataflowWriteChannel messagesToLog = new DataflowQueue()

//Define the decryption pipeline
final DataflowWriteChannel messagesToDecrypt = new DataflowQueue()
final decryptedMessages = messagesToDecrypt.chainWith {it[15..-4]} chainWith {it.reverse()} chainWith {it.toLowerCase()} chainWith {'Groovy leaks! Check out a decrypted secret message: ' + it}

//Define the encryption pipeline and split it into the save, log and decrypt pipelines
encrypt.chainWith toUpperCase chainWith {it.reverse()} chainWith {'###encrypted###' + it + '###'} split([messagesToSave, messagesToLog, messagesToDecrypt])

//Run it all asynchronously
task {
    encrypt << "I need to keep this message secret!"
    encrypt << "GPars can build operator pipelines really easy"
}

task {
    2.times {
        println "Saving " + messagesToSave.val
    }
}

task {
    2.times {
        println "Logging " + messagesToLog.val
    }
}

task {
    2.times {
        println "Decrypted " + decryptedMessages.val
    }
}

sleep 1000

