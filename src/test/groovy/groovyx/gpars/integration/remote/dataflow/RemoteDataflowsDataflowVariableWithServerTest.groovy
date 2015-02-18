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

package groovyx.gpars.integration.remote.dataflow

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowVariableWithServerTest extends RemoteSpecification {
    def static PORT = 9021

    @Shared
    RemoteDataflows serverRemoteDataflows

    @Shared
    RemoteDataflows clientRemoteDataflows

    def setupSpec() {
        serverRemoteDataflows = RemoteDataflows.create()
        serverRemoteDataflows.startServer getHostAddress(), PORT

        clientRemoteDataflows = RemoteDataflows.create()
    }

    def cleanupSpec() {
        serverRemoteDataflows.stopServer()
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable"() {
        setup:
        def variableName = "test-variable-1"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        serverRemoteDataflows.publish variable, variableName
        def remoteVariable = clientRemoteDataflows.getVariable getHostAddress(), PORT, variableName get()

        variable << testValue

        then:
        remoteVariable.val == testValue
    }

    @Timeout(5)
    def "can retrieve published bound DataflowVariable"() {
        setup:
        def variable = new DataflowVariable()

        when:
        variable << testValue

        serverRemoteDataflows.publish variable, variableName
        def remoteVariable = clientRemoteDataflows.getVariable getHostAddress(), PORT, variableName get()

        then:
        remoteVariable.val == testValue

        where:
        variableName << ["test-variable-2a", "test-variable-2b"]
        testValue << ["test DataflowVariable", null]
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable and bind it remotely"() {
        setup:
        def variable = new DataflowVariable()

        when:
        serverRemoteDataflows.publish variable, variableName
        def remoteVariable = clientRemoteDataflows.getVariable getHostAddress(), PORT, variableName get()

        remoteVariable << testValue

        then:
        variable.val == testValue

        where:
        variableName << ["test-variable-3a", "test-variable-3b"]
        testValue << ["test DataflowVariable", null]
    }
}
