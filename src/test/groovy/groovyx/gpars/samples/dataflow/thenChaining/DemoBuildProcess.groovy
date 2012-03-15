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

package groovyx.gpars.samples.dataflow.thenChaining

import static groovyx.gpars.dataflow.Dataflow.task
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

/**
 * Illustrates the use of datflow variables and tasks to orchestrate a build process
 * @author Vaclav Pech
 */

def createABuildStep = {name -> {-> println "Starting $name"; sleep 3000; println "Finished $name"}}
def createAComplexBuildStep = {name -> {a, b -> println "Starting $name"; sleep 3000; println "Finished $name"}}
def checkoutSourceLibs = createABuildStep 'Checkout Source Libs'
def fetchSourceLibs = createABuildStep 'Fetch Source Libs'
def fetchTestLibs = createABuildStep 'Fetch Test Libs'
def compileSources = createABuildStep 'Compile Sources'
def compileUnitTests = createAComplexBuildStep 'Compile Unit Tests'
def compileIntegrationTests = createAComplexBuildStep 'Compile Integration Tests'
def runUnitTests = createABuildStep 'Run Unit Tests'
def runIntegrationTests = createABuildStep 'Run Integration Tests'
def generateAPIDoc = createABuildStep 'Generate API Doc'
def generateUserDocumentation = createABuildStep 'Generate User Documentation'
def packageSources = createABuildStep 'Package Sources'
def deploy = createABuildStep 'Deploy'
def uploadDocumentation = createAComplexBuildStep 'Upload Documentation'

def checkout = task checkoutSourceLibs
def sourceCompiled = checkout.then fetchSourceLibs then compileSources
def testLibsReady = checkout.then fetchTestLibs
def unitTestsResult = whenAllBound([sourceCompiled, testLibsReady], compileUnitTests).then runUnitTests
def integrationTestsResult = whenAllBound([sourceCompiled, testLibsReady], compileIntegrationTests).then runIntegrationTests
def apiDoc = checkout.then generateAPIDoc
def userDoc = checkout.then generateUserDocumentation

whenAllBound([unitTestsResult, integrationTestsResult]) {uReport, iReport -> if (uReport && iReport) packageSources()}.then deploy
whenAllBound([apiDoc, userDoc], uploadDocumentation)

sleep 30000