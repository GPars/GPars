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

package groovyx.gpars.stm;

import groovy.lang.Closure;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;

/**
 * @author Vaclav Pech
 */
public final class AtomicBlock<T> implements AtomicClosure<T> {
    private final Closure code;

    AtomicBlock(final Closure code) {
        if (code == null) throw new IllegalArgumentException("The code for an atomic block must not be null.");
        this.code = code;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T execute(final Transaction transaction) {
        return (T) code.call(transaction);
    }
}
