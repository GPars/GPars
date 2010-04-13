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

package groovyx.gpars.agent;

import java.util.concurrent.ThreadFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Vaclav
 * Date: 13.4.2010
 * Time: 12:36:37
 * To change this template use File | Settings | File Templates.
 */
final class AgentThreadFactory implements ThreadFactory {
    public Thread newThread(final Runnable r) {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }
}