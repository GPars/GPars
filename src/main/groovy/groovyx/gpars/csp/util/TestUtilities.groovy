// GPars (formerly GParallelizer)
//
// Copyright © 2008-9  The original author or authors
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

package groovyx.gpars.csp.util

abstract class TestUtilities {

    public static boolean listContains(list1, list2) {

        if (list1.size != list2.size) {
            return false
        }
        else {
            list1.sort()
            list2.sort()
            return (list1 == list2)
        }
    } // end listContains

    public static boolean list1GEList2(list1, list2) {

        if (list1.size != list2.size) {
            return false
        }
        else {
            for (i in 0..<list1.size) {
                if (list1[i] < list2[i]) {
                    return false
                }
            }
            return true
        }

    }


}
