// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.samples.collections

import groovy.transform.TupleConstructor

import static groovyx.gpars.GParsPool.withPool

/**
 * @author Mario Garcia, Vaclav Pech
 */

@TupleConstructor
class BrandedCar {
    String model
    String brand
    Double price

    String toString() {
        "PricedCar $model"
    }
}

def cars = [new BrandedCar('F550', 'a', 2342.223), new BrandedCar('F550', 'b', 234.234), new BrandedCar('Da', 'c', 2222.2)]

final createAccumulator = { new BrandedCar('', '', 0.0) }

withPool {
    def modelResult =
        cars.parallel.map {
            [it.model, it]
        }.combine(createAccumulator) { sum, value ->
            sum.model = value.model
            sum.brand = "Mixed values"
            sum.price += value.price
            sum
        }
        .values()

    println modelResult

    def brandedResult =
        cars.parallel.map {
            [it.model + it.brand, it]
        }.combine(createAccumulator) { sum, value ->
            sum.model = value.model
            sum.brand = value.brand
            sum.price += value.price
            sum
        }
        .values()

    println brandedResult
}