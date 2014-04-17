/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import static groovyx.gpars.extra166y.Ops.BinaryOp;
import static groovyx.gpars.extra166y.Ops.BinaryPredicate;
import static groovyx.gpars.extra166y.Ops.Generator;
import static groovyx.gpars.extra166y.Ops.IntAndObjectPredicate;
import static groovyx.gpars.extra166y.Ops.IntAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.IntToObject;
import static groovyx.gpars.extra166y.Ops.Op;
import static groovyx.gpars.extra166y.Ops.Predicate;

/**
 * A prefix view of ParallelArray that causes operations to apply
 * only to elements for which a selector returns true.
 * Instances of this class may be constructed only via prefix
 * methods of ParallelArray or its other prefix classes.
 */
public abstract class ParallelArrayWithFilter<T>
    extends ParallelArrayWithMapping<T,T> {
    ParallelArrayWithFilter(ForkJoinPool ex, int origin, int fence, T[] array) {
        super(ex, origin, fence, array);
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their current values.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelArrayWithFilter<T> replaceWithMapping
                                             (Op<? super T, ? extends T> op) {
        ex.invoke(new PAS.FJOTransform(this, origin, fence,
                                       null, op));
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their indices.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelArrayWithFilter<T> replaceWithMappedIndex
                                             (IntToObject<? extends T> op) {
        ex.invoke(new PAS.FJOIndexMap(this, origin, fence,
                                      null, op));
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to each index and current element value.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelArrayWithFilter<T> replaceWithMappedIndex
        (IntAndObjectToObject<? super T, ? extends T> op) {
        ex.invoke(new PAS.FJOBinaryIndexMap
                  (this, origin, fence, null, op));
        return this;
    }

    /**
     * Replaces elements with results of applying the given generator.
     * @param generator the generator
     * @return this (to simplify use in expressions)
     */
    public ParallelArrayWithFilter<T> replaceWithGeneratedValue
        (Generator<? extends T> generator) {
        ex.invoke(new PAS.FJOGenerate
                  (this, origin, fence, null, generator));
        return this;
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     * @return this (to simplify use in expressions)
     */
    public ParallelArrayWithFilter<T> replaceWithValue(T value) {
        ex.invoke(new PAS.FJOFill(this, origin, fence,
                                  null, value));
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public <V,W> ParallelArrayWithFilter<T> replaceWithMapping
        (BinaryOp<? super T, ? super V, ? extends T> combiner,
         ParallelArrayWithMapping<W,V> other) {
        ex.invoke(new PAS.FJOPACombineInPlace
                  (this, origin, fence, null,
                   other, other.origin - origin, combiner));
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public ParallelArrayWithFilter<T> replaceWithMapping
        (BinaryOp<T,T,T> combiner, T[] other) {
        ex.invoke(new PAS.FJOCombineInPlace
                  (this, origin, fence, null, other,
                   -origin, combiner));
        return this;
    }

    /**
     * Returns a new ParallelArray containing only non-null unique
     * elements (that is, without any duplicates). This method
     * uses each element's {@code equals} method to test for
     * duplication.
     * @return the new ParallelArray
     */
    public ParallelArray<T> allUniqueElements() {
        PAS.UniquifierTable tab = new PAS.UniquifierTable
            (fence - origin, this, false);
        PAS.FJOUniquifier f = new PAS.FJOUniquifier
            (this, origin, fence, null, tab);
        ex.invoke(f);
        T[] res = (T[])(tab.uniqueObjects(f.count));
        return new ParallelArray<T>(ex, res);
    }

    /**
     * Returns a new ParallelArray containing only non-null unique
     * elements (that is, without any duplicates). This method
     * uses reference identity to test for duplication.
     * @return the new ParallelArray
     */
    public ParallelArray<T> allNonidenticalElements() {
        PAS.UniquifierTable tab = new PAS.UniquifierTable
            (fence - origin, this, true);
        PAS.FJOUniquifier f = new PAS.FJOUniquifier
            (this, origin, fence, null, tab);
        ex.invoke(f);
        T[] res = (T[])(tab.uniqueObjects(f.count));
        return new ParallelArray<T>(ex, res);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public abstract ParallelArrayWithFilter<T> withFilter
        (Predicate<? super T> selector);

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given binary selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public <V,W> ParallelArrayWithFilter<T> withFilter
        (BinaryPredicate<? super T, ? super V> selector,
         ParallelArrayWithMapping<W,V> other) {
        return withIndexedFilter(AbstractParallelAnyArray.indexedSelector(selector, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given indexed selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public abstract ParallelArrayWithFilter<T> withIndexedFilter
        (IntAndObjectPredicate<? super T> selector);



    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are equal.
     * @param other the other array
     * @return true if equal
     */
    public <U,V> boolean hasAllEqualElements
        (ParallelArrayWithMapping<U,V> other) {
        return withFilter(CommonOps.inequalityPredicate(),
                          other).anyIndex() < 0;
    }

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are identical.
     * @param other the other array
     * @return true if equal
     */
    public <U,V> boolean hasAllIdenticalElements
        (ParallelArrayWithMapping<U,V> other) {
        return withFilter(CommonOps.nonidentityPredicate(),
                          other).anyIndex() < 0;
    }

    final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
        final Object[] a = this.array;
        for (int i = lo; i < hi; ++i)
            dest[offset++] = (a[i]);
    }

    final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                   Object[] dest, int offset) {
        final Object[] a = this.array;
        for (int i = loIdx; i < hiIdx; ++i)
            dest[offset++] = (a[indices[i]]);
    }

    final Object oget(int i) { return this.array[i]; }
}
