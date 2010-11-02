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

package groovyx.gpars.pa

/**
 * Represents a {a, b -> a + b} closure
 *
 * @author Vaclav Pech
 */
@Singleton
public final class SumClosure extends Closure {
    private static final long serialVersionUID = 209099114666842715L;

    private SumClosure() {
        super(null)
    }

    @Override
    public Object call(final Object[] args) {
        return args[0] + args[1];
    }
}
