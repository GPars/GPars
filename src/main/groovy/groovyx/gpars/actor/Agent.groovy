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

package groovyx.gpars.actor

import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.util.PoolUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Created by IntelliJ IDEA.
 * User: Vaclav
 * Date: 13.4.2010
 * Time: 9:21:59
 * To change this template use File | Settings | File Templates.
 */
abstract class Agent {
    private static ExecutorService pool = Executors.newFixedThreadPool(PoolUtils.retrieveDefaultPoolSize(), new AgentThreadFactory())
    private static ExecutorService orchestrationPool = Executors.newCachedThreadPool(new AgentThreadFactory())

    //todo shutdown pools

    final private def msgs = new LinkedList()
    final private def msgsLock = new Object()

    private def active = false

    public void start() {}  //todo remove
    public void replyIfExists(message) {}  //todo implement

    public final send(message) {
        synchronized (msgsLock) {
            msgs.add(message)
            schedule()
        }
    }

    final def sendAndWait() {
        return sendAndWait {{a ->}}
    }

    final def sendAndWait(Closure message) {
        final DataFlowVariable result = new DataFlowVariable()
        this.send {
            result << message.call(it)
        }
        return result.val
    }

    final void sendAndContinue(message, Closure code) {
        orchestrationPool.submit {
            code.call(sendAndWait(message))
        }
    }

    public final leftShift(message) {
        send message
    }

    final void perform() {
        def message
        synchronized (msgsLock) {
            message = msgs.poll()
        }

        this.onMessage message

        synchronized (msgsLock) {
            active = false
            schedule()
        }
    }

    private void schedule() {
        if ((msgs.size() > 0) && (!active)) {
            active = true
            pool.submit({
                try {
                    this.perform()
                } catch (Throwable e) {
                    e.printStackTrace()
                }
            })
        }
    }
}

final class AgentThreadFactory implements ThreadFactory {

    Thread newThread(Runnable r) {
        final Thread thread = new Thread(r)
        thread.daemon = true
        return thread
    }
}