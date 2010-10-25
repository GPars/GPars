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

package groovyx.gpars.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Handy methods build PA from different types
 *
 * @author Vaclav Pech
 *         Date: 24th Oct 2010
 */
@SuppressWarnings({"UtilityClass", "AbstractClassWithoutAbstractMethods", "AbstractClassNeverImplemented", "StaticMethodOnlyUsedInOneClass"})
public abstract class PAUtils {
    public static Collection<Object> createCollection(final Iterable<Object> object) {
        final Collection<Object> collection = new ArrayList<Object>();
        for (final Object item : object) {
            collection.add(item);
        }
        return collection;
    }

    public static Collection<Object> createCollection(final Iterator<Object> iterator) {
        final Collection<Object> collection = new ArrayList<Object>();
        while (iterator.hasNext()) {
            collection.add(iterator.next());
        }
        return collection;
    }

    public static String[] createArray(final CharSequence value) {
        final String[] chars = new String[value.length()];
        for (int i = 0; i < value.length(); i++) {
            chars[i] = String.valueOf(value.charAt(i));
        }
        return chars;
    }

    public static Map.Entry<Object, Object>[] createArray(final Map<Object, Object> map) {
        @SuppressWarnings({"unchecked"})
        final Map.Entry<Object, Object>[] result = new Map.Entry[map.size()];
        int i = 0;
        for (final Map.Entry<Object, Object> entry : map.entrySet()) {
            result[i] = entry;
            i++;
        }
        return result;
    }
}
