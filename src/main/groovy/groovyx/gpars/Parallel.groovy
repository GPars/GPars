//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars

/**
 * Enhances by mixing-in a collection within a Parallelizer.doParallel() block overriding the iterative methods,
 * like each, collect and such to delegate to eachParallel, collectParallel and other parallel iterative methods.
 * The collections returned from collect(), findAll() and grep() are again mixed with a Parallel instance,
 * so their iterative methods are transparently parallel as well.
 *
 * Author: Vaclav Pech, Dierk Koenig
 * Date: Oct 30, 2009
 */
final class Parallel {
    public def final each(Closure yield) { mixedIn[Object].eachParallel(yield) }
    public def final eachWithIndex(Closure yield) { mixedIn[Object].eachWithIndexParallel(yield)}
    public def final collect(Closure yield) { mixedIn[Object].collectParallel(yield).makeTransparentlyParallel()}
    public def final find(Closure yield) { mixedIn[Object].findParallel(yield)}
    public def final findAll(Closure yield) { mixedIn[Object].findAllParallel(yield).makeTransparentlyParallel()}
    public def final grep(filter) { mixedIn[Object].grepParallel(filter).makeTransparentlyParallel()}
    public def final all(Closure yield) { mixedIn[Object].allParallel(yield)}
    public def final any(Closure yield) { mixedIn[Object].anyParallel(yield)}
    public def final groupBy(Closure yield) { mixedIn[Object].groupByParallel(yield)}
    def final boolean isTransparentlyParallel() {return true}
}
