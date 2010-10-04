// GPars - Groovy Parallel Systems
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

package groovyx.gpars.dataflow.stream;

import groovy.lang.Closure;

import java.util.Iterator;
import java.util.NoSuchElementException;

final class EmptyList implements FList<Object> {
    private static final String EMPTY_LIST_HAS_NO_ELEMENTS = "EmptyList has no elements.";
    private static final Iterator<Object> emptyListIterator = new EmptyListIterator();

    @Override
    public Object getFirst() {
        throw new NoSuchElementException(EMPTY_LIST_HAS_NO_ELEMENTS);
    }

    @Override
    public FList<Object> getRest() {
        throw new NoSuchElementException(EMPTY_LIST_HAS_NO_ELEMENTS);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String appendingString() {
        return "";
    }

    @Override
    public FList<Object> filter(final Closure filterClosure) {
        return this;
    }

    @Override
    public FList<Object> map(final Closure mapClosure) {
        return this;
    }

    @Override
    public Object reduce(final Closure reduceClosure) {
        return null;
    }

    @Override
    public Object reduce(final Object seed, final Closure reduceClosure) {
        return seed;
    }

    @Override
    public Iterator<Object> iterator() {
        return emptyListIterator;
    }

    private static class EmptyListIterator implements Iterator<Object> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(EMPTY_LIST_HAS_NO_ELEMENTS);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Can't remove from an EmptyList iterator");
        }
    }
}
