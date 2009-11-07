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
 * Enhances objects by being mixed-in either within a Parallelizer.doParallel() block or after enhancement by
 * the ParallelEnhancer through the makeTransparent() method.
 * It overrides the iterative methods, like each, collect and such to delegate to eachParallel, collectParallel
 * and other parallel iterative methods.
 * The collections returned from collect(), findAll() and grep() are again mixed with a TransparentParallel instance,
 * so their iterative methods are transparently parallel as well.
 *
 * Author: Vaclav Pech, Dierk Koenig
 * Date: Oct 30, 2009
 */
final class TransparentParallel {
    public def final each(Closure yield) { this.eachParallel(yield) }
    public def final eachWithIndex(Closure yield) { this.eachWithIndexParallel(yield)}
    public def final collect(Closure yield) { this.collectParallel(yield).makeTransparent()}
    public def final find(Closure yield) { this.findParallel(yield)}
    public def final findAll(Closure yield) { this.findAllParallel(yield).makeTransparent()}
    public def final grep(filter) { this.grepParallel(filter).makeTransparent()}
    public def final every(Closure yield) { this.everyParallel(yield)}
    public def final any(Closure yield) { this.anyParallel(yield)}
    public def final groupBy(Closure yield) { this.groupByParallel(yield)}
    public def final min(Closure yield) { this.minParallel(yield)}
    public def final min() { this.minParallel()}
    public def final max(Closure yield) { this.maxParallel(yield)}
    public def final max() { this.maxParallel()}
    public def final sum() { this.sumParallel()}
    public def final fold(Closure yield) { this.foldParallel(yield)}

    /**
     * Indicates, whether the iterative methods like each() or collect() have been made parallel.
     */
    public def boolean isTransparent() {return true}
}
