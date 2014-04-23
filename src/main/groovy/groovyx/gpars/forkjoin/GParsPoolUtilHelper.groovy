// GPars - Groovy Parallel Systems
//
// Copyright © 2008–2012, 2014  The original author or authors
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

package groovyx.gpars.forkjoin

import groovyx.gpars.GParsPoolUtil
import groovyx.gpars.TransparentParallel
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.scheduler.FJPool

import java.util.concurrent.Future

/**
 * Off-loads some of the heavy-on-Groovy functionality from GParsPoolUtil
 *
 * @author Vaclav Pech
 * @author Russel Winder
 */
class GParsPoolUtilHelper {

  public static Closure async(Closure cl) {
    return {Object... args -> (args != null && args.size() == 0) ? GParsPoolUtil.callParallel(cl) : GParsPoolUtil.callParallel({ -> cl(* args) })}
   }

  public static <T> Future<T> callAsync(final Closure<T> cl, final Object... args) {
    return GParsPoolUtil.callParallel({-> cl(* args) });
  }

  public static Closure asyncFun(final Closure original, final boolean blocking, final FJPool pool = null) {
    final FJPool localPool = pool ?: retrieveFJPool();
    return {final Object[] args ->
      final DataflowVariable result = new DataflowVariable()
      PAUtils.evaluateArguments(localPool ?: new FJPool(GParsPoolUtil.retrievePool()), args.clone(), 0, [], result, original, false)
      blocking ? result.get() : result
    }
  }


       /**
        * Overrides the iterative methods like each(), collect() and such, so that they call their parallel variants from the GParsPoolUtil class
        * like eachParallel(), collectParallel() and such.
        * The first time it is invoked on a collection the method creates a TransparentParallel class instance and mixes it
        * in the object it is invoked on. After mixing-in, the isConcurrent() method will return true.
        * Delegates to GParsPoolUtil.makeConcurrent().
        * @param collection The object to make transparent
        * @return The instance of the TransparentParallel class wrapping the original object and overriding the iterative methods with new parallel behavior
        */
       public static Object makeConcurrent(final Object collection) {
           if (!(collection.respondsTo('isConcurrent'))) throw new IllegalStateException("Cannot make the object transparently concurrent. Apparently we're not inside a GParsPool.withPool() block nor the collection has been enhanced with ParallelEnhancer.enhance().")
           //noinspection GroovyGetterCallCanBePropertyAccess
           if (collection.isConcurrent()) {
               collection.concurrencyActive = true
           } else collection.getMetaClass().mixin(TransparentParallel)
           return collection
       }

       /**
        * Gives the iterative methods like each() or find() the original sequential semantics.
        * @param collection The collection to apply the change to
        * @return The collection itself
        */
       public static Object makeSequential(final Object collection) {
           if (!(collection.respondsTo('isConcurrent'))) throw new IllegalStateException("Cannot make the object sequential. Apparently we're not inside a GParsPool.withPool() block nor the collection has been enhanced with ParallelEnhancer.enhance().")
           if (collection.isConcurrent()) {
               collection.concurrencyActive = false
           }

           return collection
       }


}
