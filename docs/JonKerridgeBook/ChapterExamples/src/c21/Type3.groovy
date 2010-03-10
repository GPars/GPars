// GPars (formerly GParallelizer)
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

package c21


class Type3 implements Serializable {

    def typeName = "Type3"
    def int typeInstance
    def int instanceValue

    def modify(value) {
        typeInstance = typeInstance + value
    }

    def String toString() {
        def int nodeId = typeInstance / 100000
        def int typeNumber = (typeInstance - (nodeId * 100000)) / 1000
        def int typeInstanceValue = (typeInstance - (nodeId * 100000)) % 1000
        return " Node: $nodeId, Type: $typeNumber, TypeInstanceValue: $typeInstanceValue, Sequence: $instanceValue"
//      return "Instance of $typeName instance $typeInstance and value $instanceValue"
    }


}