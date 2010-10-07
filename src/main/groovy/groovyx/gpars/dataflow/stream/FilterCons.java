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

public class FilterCons<T> extends Cons<T> {
    private final Closure filterClosure;
    private FList<T> filteredRest;
    //todo consider multi-threaded context

    public FilterCons(final T first, final FList<T> rest, final Closure filterClosure) {
        super(first, rest);
        this.filterClosure = filterClosure;
    }

    @Override
    public FList<T> getRest() {
        if (filteredRest == null) {
            final FList<T> nextElement = super.getRest();
            filteredRest = nextElement.filter(filterClosure);
        }
        return filteredRest;
    }
}
