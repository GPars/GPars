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

import groovyx.gpars.dataflow.Promise
import static groovyx.gpars.GParsPool.withPool

/**
 * Illustrates the use of asynchronous functions to orchestrate a build process
 *
 * @author Vaclav Pech
 */

//Mock-up definitions of build steps
final createABuildStep = {name -> {param -> println "Starting $name"; sleep 3000; println "Finished $name"; true}}
final createAThreeArgBuildStep = {name -> {a, b, c -> println "Starting $name"; sleep 3000; println "Finished $name"; true}}
final checkout = createABuildStep 'Checkout Sources'
final compileSources = createABuildStep 'Compile Sources'
final generateAPIDoc = createABuildStep 'Generate API Doc'
final generateUserDocumentation = createABuildStep 'Generate User Documentation'
final packageProject = createAThreeArgBuildStep 'Package Sources'
final deploy = createABuildStep 'Deploy'

/* First, we need a thread pool */

withPool {

    /* Second, we need asynchronous variants of all the individual build steps */

    final aCheckout = checkout.asyncFun()
    final aCompileSources = compileSources.asyncFun()
    final aGenerateAPIDoc = generateAPIDoc.asyncFun()
    final aGenerateUserDocumentation = generateUserDocumentation.asyncFun()
    final aPackageProject = packageProject.asyncFun()
    final aDeploy = deploy.asyncFun()

    /* Third, here's the composition of asynchronous build steps to form a process */

    Promise projectRoot = aCheckout('git@github.com:vaclav/GPars.git')
    Promise classes = aCompileSources(projectRoot)
    Promise api = aGenerateAPIDoc(projectRoot)
    Promise guide = aGenerateUserDocumentation(projectRoot)
    Promise result = aDeploy(aPackageProject(classes, api, guide))

    /* Now we're setup and can wait for the build to finish */

    println "Starting the build process. This line is quite likely to be printed first ..."

    println result.get()
}