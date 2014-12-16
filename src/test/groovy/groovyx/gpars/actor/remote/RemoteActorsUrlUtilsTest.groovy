// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
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

package groovyx.gpars.actor.remote

import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.gpars.actor.remote.RemoteActorsUrlUtils.getActorName
import static groovyx.gpars.actor.remote.RemoteActorsUrlUtils.getGroupName
import static groovyx.gpars.actor.remote.RemoteActorsUrlUtils.isValidActorName

class RemoteActorsUrlUtilsTest extends Specification {
    @Unroll
    def "IsValidActorName #name"() {
        expect:
        isValidActorName(name) == valid

        where:
        name   | valid
        "name" | true
        "a/a"  | false
        "aaa"  | true
        null   | false
        ""     | true
        "/////"| false
    }

    @Unroll
    def "GetActorName #url"() {
        expect:
        getActorName(url) == name

        where:
        url               | name
        "group/actor"     | "actor"
        "//"              | ""
        "just-actor-name" | "just-actor-name"
        "just-group/"     | ""
    }

    @Unroll
    def "GetGroupName #url"() {
        expect:
        getGroupName(url) == groupName

        where:
        url               | groupName
        "group/actor"     | "group"
        "//"              | "/"
        "just-actor-name" | ""
        "just-group/"     | "just-group"
        "/just-name"      | ""
    }
}
