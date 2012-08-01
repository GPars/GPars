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

package groovyx.gpars.memoize;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protects stored resources from eviction from memory following the LRU (Last Recently Used) strategy.
 * If the maximum size has been reached all newly added elements will cause the oldest element to be removed from the storage
 * in order not to exceed the maximum capacity.
 * The touch method can be used to renew an element and move it to the from the LRU queue.
 *
 * @author Vaclav Pech
 *         Date: Jun 22, 2010
 */
public final class LRUProtectionStorage extends LinkedHashMap<Object, Object> {
    private static final long serialVersionUID = 1L;

    private final int maxSize;

    public LRUProtectionStorage(final int maxSize) {
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(@SuppressWarnings("rawtypes") final Map.Entry<Object, Object> eldest) {
        return size() > maxSize;
    }

    /**
     * The touch method can be used to renew an element and move it to the from of the LRU queue.
     *
     * @param key   The key of the element to renew
     * @param value A value to newly associate with the key
     */
    @SuppressWarnings({"SynchronizedMethod"})
    public synchronized void touch(final Object key, final Object value) {
        if (value == get(key)) return;
        remove(key);
        put(key, value);
    }

    /**
     * Performs a shallow clone
     *
     * @return The cloned instance
     */
    @SuppressWarnings({"EmptyMethod"})
    @Override
    public Object clone() {
        return super.clone();
    }

}
