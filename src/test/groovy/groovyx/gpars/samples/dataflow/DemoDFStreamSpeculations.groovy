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

import groovyx.gpars.dataflow.DataflowQueue
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Demonstrates speculations - an ability to run in parallel several calculations of the same value with varying time demands,
 * harvesting the first obtained result and cancelling the slower calculations.
 *
 * In the sample we're willing to check whether there's an article on 'groovy' on DZone and we're using 4 urls, which resolve to the same page being downloaded.
 * Using the speculate approach we increased our chances to get the result even if some of the urls do not work or work considerably slower than others.
 *
 * This demo uses a DataflowVariable to get the first result.
 * @author Vaclav Pech
 * Date: Sep 10th 2010
 */
def alternative1 = {
    'http://www.dzone.com/links/index.html'.toURL().text
}

def alternative2 = {
    'http://www.dzone.com/'.toURL().text
}

def alternative3 = {
    'http://www.dzzzzzone.com/'.toURL().text  //will fail due to wrong url
}

def alternative4 = {
    'http://dzone.com/'.toURL().text
}

final def result = new DataflowQueue()

[alternative1, alternative2, alternative3, alternative4].each {code ->
    task {
        try {
            result << code()
        } catch (ignore) { }
    }
}

println result.val.contains('groovy')