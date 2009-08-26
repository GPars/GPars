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
