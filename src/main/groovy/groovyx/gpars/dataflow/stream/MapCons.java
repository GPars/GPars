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

public class MapCons extends Cons<Object> {
    private final Closure mapClosure;
    private FList<Object> transformedRest;
    //todo consider multi-threaded context

    public MapCons(final Object first, final FList<Object> rest, final Closure mapClosure) {
        super(first, rest);
        this.mapClosure = mapClosure;
    }

    @Override
    public FList<Object> getRest() {
        if (transformedRest == null) {
            final FList<Object> nextElement = super.getRest();
            transformedRest = nextElement.map(mapClosure);
        }
        return transformedRest;
    }
}
