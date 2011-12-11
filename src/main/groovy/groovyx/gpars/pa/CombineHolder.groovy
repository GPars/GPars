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

package groovyx.gpars.pa

/**
 * Holds a temporary reduce result for groupBy
 */
private class CombineHolder {

    final Map content

    def CombineHolder(final content) {
        this.content = content;
    }

    final CombineHolder merge(final CombineHolder other, final Closure accumulation, final Closure initialValue) {
        for (item in other.content.entrySet()) {
            for (value in item.value) {
                addToMap(item.key, value, accumulation, initialValue)
            }
        }
        return this
    }

    def CombineHolder addToMap(final Object key, final Object item, final Closure accumulation, final Closure initialValue) {
        def currentValue = content[key] ?: initialValue()
        content[key] = accumulation(currentValue, item)
        return this
    }
}
