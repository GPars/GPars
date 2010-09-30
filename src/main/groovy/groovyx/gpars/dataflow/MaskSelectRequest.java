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

package groovyx.gpars.dataflow;

import java.util.List;

/**
 * @author Vaclav Pech
 *         Date: 30th Sep 2010
 */
abstract class MaskSelectRequest<T> implements SelectRequest<T> {
    private final List<Boolean> mask;

    /**
     * @param mask The list of boolean flags indicating shich position should be matched against. All indexes match against a null mask
     */
    MaskSelectRequest(final List<Boolean> mask) {
        this.mask = mask;
    }

    @Override
    public boolean matchesMask(final int index) {
        if (mask == null) return true;
        //noinspection AutoUnboxing
        return mask.get(index);
    }
}
