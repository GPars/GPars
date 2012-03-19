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

package groovyx.gpars.samples.dataflow

import static groovyx.gpars.dataflow.Dataflow.task
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

/**
 * Illustrates the use of dataflow variables and tasks to orchestrate a build process
 *
 * @author Vaclav Pech
 */

//Mock-up definitions of build steps
def createABuildStep = {name -> {param -> println "Starting $name"; sleep 3000; println "Finished $name"; true}}
def createATwoArgBuildStep = {name -> {a, b -> println "Starting $name"; sleep 3000; println "Finished $name"; true}}
def createAThreeArgBuildStep = {name -> {a, b, c -> println "Starting $name"; sleep 3000; println "Finished $name"; true}}
def checkout = createABuildStep 'Checkout Sources'
def fetchSourceLibs = createABuildStep 'Fetch Source Libs'
def fetchTestLibs = createABuildStep 'Fetch Test Libs'
def compileSources = createABuildStep 'Compile Sources'
def compileUnitTests = createATwoArgBuildStep 'Compile Unit Tests'
def runUnitTests = createABuildStep('Run Unit Tests') >> {[unitTestSuccessful: true]}
def generateAPIDoc = createABuildStep 'Generate API Doc'
def generateUserDocumentation = createABuildStep 'Generate User Documentation'
def packageSources = createABuildStep 'Package Sources'
def deploy = createABuildStep 'Deploy'
def uploadDocumentation = createAThreeArgBuildStep 'Upload Documentation'

/* Here's the composition of individual build steps into a process */

def checkoutDone = task {checkout('git@github.com:vaclav/GPars.git')}
def sourceCompiled = checkoutDone.then fetchSourceLibs then compileSources
def testLibsReady = checkoutDone.then fetchTestLibs
def unitTestsResult = whenAllBound([sourceCompiled, testLibsReady], compileUnitTests).then runUnitTests
def deployed = unitTestsResult.then {buildContext ->
    if (buildContext.unitTestSuccessful) {
        deploy(packageSources(buildContext))
    } else return buildContext
}
def apiDocGenerated = checkoutDone.then generateAPIDoc
def userDocGenerated = checkoutDone.then generateUserDocumentation

def docsUploaded = whenAllBound([apiDocGenerated, userDocGenerated, unitTestsResult], uploadDocumentation)

/* Now we're setup and can wait for the build to finish */

println "Starting the build process. This line is quite likely to be printed first ..."

[deployed, docsUploaded]*.join()