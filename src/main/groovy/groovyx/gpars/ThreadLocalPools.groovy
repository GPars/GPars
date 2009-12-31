//  GPars (formerly GParallelizer)
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

package groovyx.gpars

/**
 * Holds a thread-local stack of pools to allow for nested calls to Parallelizer.doParallel() or Asynchronizer.doParallel()
 *
 * @author Vaclav Pech
 * Date: Dec 15, 2009
 */
final class ThreadLocalPools extends ThreadLocal<List> {

    protected List initialValue() {
        new LinkedList()
    }

    /**
     * Adds a new element
     */
    void leftShift(pool) {
        get() << pool
    }

    /**
     * Removes the top (last) element
     */
    void pop() {
        assert !get().isEmpty()
        get().removeLast()
    }

    /**
     * Gives the current element
     */
    def getCurrent() {
        final def stack = get()
        stack.size()>0 ? stack.last : null
    }
}
