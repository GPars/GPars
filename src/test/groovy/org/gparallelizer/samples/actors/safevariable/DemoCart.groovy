//  GParallelizer
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

package org.gparallelizer.samples.actors.safevariables

import org.gparallelizer.actors.pooledActors.SafeVariable

class ShoppingCart {

    private def cartState = new SafeVariable([:])
//    private SafeVariable<Map<String, Integer>> cartState = new SafeVariable<Map<String, Integer>>(new HashMap<String, Integer>())

    public void addItem(String product, int quantity) {
        cartState << {it[product] = quantity}
    }

    public void removeItem(String product) {
        cartState << {it.remove(product)}
    }

    public Object listContent() {
        return cartState.val
    }

    public void increaseQuantity(String product, int quantityChange) {
        cartState << this.&changeQuantity.curry(product, quantityChange)
    }

    public void clearItems() {
        cartState << performClear
    }

    private void changeQuantity(String product, int quantityChange, Map items) {
        items[product] = (items[product] ?: 0) + quantityChange
    }

    private Closure performClear = { it.clear() }
}

final ShoppingCart cart = new ShoppingCart()
cart.addItem 'Pilsner', 10
cart.addItem 'Budweisser', 5
cart.addItem 'Staropramen', 20
cart.removeItem 'Budweisser'
cart.addItem 'Budweisser', 15
println "Contents ${cart.listContent()}"
cart.increaseQuantity 'Budweisser', 3
println "Contents ${cart.listContent()}"
cart.clearItems()
println "Contents ${cart.listContent()}"
