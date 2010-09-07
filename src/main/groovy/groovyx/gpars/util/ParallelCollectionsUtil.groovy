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

package groovyx.gpars.util

/**
 *
 * @author Vaclav Pech
 * Date: 6th Sep 2010
 */
abstract class ParallelCollectionsUtil {

    static java.util.Collection createCollection(Object object) {
        def collection = []
        for (element in object) collection << element
        return collection
    }

    /**
     * If the passed-in closure expects two arguments, it is considered to be a map-iterative code and is then wrapped
     * with a single-argument closure, which unwraps the key:value pairs for the original closure.
     * If the supplied closure doesn't expect two arguments, it is returned unchanged.
     * @param cl The closure to use for parallel methods
     * @return The original or an unwrapping closure
     */
    static public Closure buildClosureForMaps(Closure cl) {
        if (cl.maximumNumberOfParameters == 2) return {cl.call(it.key, it.value)}
        return cl
    }

    /**
     * If the passed-in closure expects three arguments, it is considered to be a map-iterative_with_index code and is then wrapped
     * with a two-argument closure, which unwraps the key:value pairs for the original closure.
     * If the supplied closure doesn't expect three arguments, it is returned unchanged.
     * @param cl The closure to use for parallel methods
     * @return The original or an unwrapping closure
     */
    static public Closure buildClosureForMapsWithIndex(Closure cl) {
        if (cl.maximumNumberOfParameters == 3) return {item, index -> cl.call(item.key, item.value, index)}
        return cl
    }

    /**
     * Builds a resulting map out of an map entry collection
     * @param result The collection containing map entries
     * @return A corresponding map instance
     */
    public static <K, V> Map<K, V> buildResultMap(List<Map.Entry<K, V>> result) {
        Map<K, V> map = new HashMap<K, V>(result.size())
        for (item in result) {map[item.key] = item.value}
        return map
    }


}
