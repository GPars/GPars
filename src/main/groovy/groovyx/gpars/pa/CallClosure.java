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

package groovyx.gpars.pa;

import groovy.lang.Closure;

public final class CallClosure extends Closure {
    private final Closure target;
    private static final long serialVersionUID = 209099114666842715L;

    public CallClosure(final Closure target) {
        super(target.getOwner());
        this.target = target;
    }

    @Override
    public Object call(final Object[] args) {
        return target.call(args);
    }

    @Override
    public Object call() {
        return target.call();
    }

    @Override
    public Object call(final Object arguments) {
        return target.call(arguments);
    }

    @Override
    public Object clone() {
        return super.clone();
    }
}
