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

package groovyx.gpars.util

import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.TimeUnit
import groovyx.gpars.dataflow.DataflowVariable

/**
 *
 * @author Vaclav Pech
 * Date: Jan 15, 2010
 */

class EnhancedRWLockTest extends GroovyTestCase {
    public void testReadLock() {
        final ReentrantReadWriteLock lock = new EnhancedRWLock(false)
        lock.withReadLock {
            assertTrue lock.readLock().tryLock()
            lock.readLock().unlock()
            assertFalse lock.writeLock().tryLock(10, TimeUnit.MILLISECONDS)

            final def result1 = new DataflowVariable<Boolean>()
            Thread.start {result1 << lock.readLock().tryLock(10, TimeUnit.MILLISECONDS); lock.readLock().unlock()}
            assertTrue result1.val

            final def result2 = new DataflowVariable<Boolean>()
            Thread.start {result2 << lock.writeLock().tryLock(10, TimeUnit.MILLISECONDS)}
            assertFalse result2.val
        }
    }

    public void testWriteLock() {
        final ReentrantReadWriteLock lock = new EnhancedRWLock(false)
        lock.withWriteLock {
            assertTrue lock.readLock().tryLock()
            lock.readLock().unlock()
            assertTrue lock.writeLock().tryLock()
            lock.writeLock().unlock()

            final def result1 = new DataflowVariable<Boolean>()
            Thread.start {result1 << lock.readLock().tryLock(10, TimeUnit.MILLISECONDS)}
            assertFalse result1.val

            final def result2 = new DataflowVariable<Boolean>()
            Thread.start {result2 << lock.writeLock().tryLock(10, TimeUnit.MILLISECONDS)}
            assertFalse result2.val
        }
    }
}