/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import static groovyx.gpars.extra166y.Ops.BinaryDoubleOp;
import static groovyx.gpars.extra166y.Ops.DoubleAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.DoubleAndDoubleToObject;
import static groovyx.gpars.extra166y.Ops.DoubleAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.DoubleAndLongToLong;
import static groovyx.gpars.extra166y.Ops.DoubleAndLongToObject;
import static groovyx.gpars.extra166y.Ops.DoubleAndObjectToDouble;
import static groovyx.gpars.extra166y.Ops.DoubleAndObjectToLong;
import static groovyx.gpars.extra166y.Ops.DoubleAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.DoubleComparator;
import static groovyx.gpars.extra166y.Ops.DoubleOp;
import static groovyx.gpars.extra166y.Ops.DoubleProcedure;
import static groovyx.gpars.extra166y.Ops.DoubleReducer;
import static groovyx.gpars.extra166y.Ops.DoubleToLong;
import static groovyx.gpars.extra166y.Ops.DoubleToObject;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToObject;

/**
 * A prefix view of ParallelArray that causes operations to apply
 * to mappings of elements, not to the elements themselves.
 * Instances of this class may be constructed only via prefix
 * methods of ParallelDoubleArray or its other prefix classes.
 */
public abstract class ParallelDoubleArrayWithDoubleMapping extends AbstractParallelAnyArray.DPap {
    ParallelDoubleArrayWithDoubleMapping
        (ForkJoinPool ex, int origin, int fence, double[] array) {
        super(ex, origin, fence, array);
    }

    /**
     * Applies the given procedure to elements.
     * @param procedure the procedure
     */
    public void apply(DoubleProcedure procedure) {
        ex.invoke(new PAS.FJDApply(this, origin, fence, null, procedure));
    }

    /**
     * Returns reduction of elements.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return reduction
     */
    public double reduce(DoubleReducer reducer, double base) {
        PAS.FJDReduce f = new PAS.FJDReduce
            (this, origin, fence, null, reducer, base);
        ex.invoke(f);
        return f.result;
    }

    /**
     * Returns the minimum element, or Double.MAX_VALUE if empty.
     * @return minimum element, or Double.MAX_VALUE if empty
     */
    public double min() {
        return reduce(CommonOps.naturalDoubleMinReducer(), Double.MAX_VALUE);
    }

    /**
     * Returns the minimum element, or Double.MAX_VALUE if empty.
     * @param comparator the comparator
     * @return minimum element, or Double.MAX_VALUE if empty
     */
    public double min(DoubleComparator comparator) {
        return reduce(CommonOps.doubleMinReducer(comparator), Double.MAX_VALUE);
    }

    /**
     * Returns the maximum element, or -Double.MAX_VALUE if empty.
     * @return maximum element, or -Double.MAX_VALUE if empty
     */
    public double max() {
        return reduce(CommonOps.naturalDoubleMaxReducer(), -Double.MAX_VALUE);
    }

    /**
     * Returns the maximum element, or -Double.MAX_VALUE if empty.
     * @param comparator the comparator
     * @return maximum element, or -Double.MAX_VALUE if empty
     */
    public double max(DoubleComparator comparator) {
        return reduce(CommonOps.doubleMaxReducer(comparator), -Double.MAX_VALUE);
    }

    /**
     * Returns the sum of elements.
     * @return the sum of elements
     */
    public double sum() {
        return reduce(CommonOps.doubleAdder(), 0);
    }

    /**
     * Returns summary statistics.
     * @param comparator the comparator to use for
     * locating minimum and maximum elements
     * @return the summary
     */
    public ParallelDoubleArray.SummaryStatistics summary
        (DoubleComparator comparator) {
        PAS.FJDStats f = new PAS.FJDStats
            (this, origin, fence, null, comparator);
        ex.invoke(f);
        return f;
    }

    /**
     * Returns summary statistics, using natural comparator.
     * @return the summary
     */
    public ParallelDoubleArray.SummaryStatistics summary() {
        return summary(CommonOps.naturalDoubleComparator());
    }

    /**
     * Returns a new ParallelDoubleArray holding elements.
     * @return a new ParallelDoubleArray holding elements
     */
    public ParallelDoubleArray all() {
        return new ParallelDoubleArray(ex, allDoubles());
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithDoubleMapping withMapping
        (DoubleOp op);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithLongMapping withMapping
        (DoubleToLong op);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public abstract <U> ParallelDoubleArrayWithMapping<U> withMapping
        (DoubleToObject<? extends U> op);

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V,W,X> ParallelDoubleArrayWithMapping<W> withMapping
        (DoubleAndObjectToObject<? super V, ? extends W> combiner,
         ParallelArrayWithMapping<X,V> other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V> ParallelDoubleArrayWithMapping<V> withMapping
        (DoubleAndDoubleToObject<? extends V> combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V> ParallelDoubleArrayWithMapping<V> withMapping
        (DoubleAndLongToObject<? extends V> combiner,
         ParallelLongArrayWithLongMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V,W> ParallelDoubleArrayWithDoubleMapping withMapping
        (DoubleAndObjectToDouble<? super V> combiner,
         ParallelArrayWithMapping<W,V> other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelDoubleArrayWithDoubleMapping withMapping
        (BinaryDoubleOp combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelDoubleArrayWithDoubleMapping withMapping
        (DoubleAndLongToDouble combiner,
         ParallelLongArrayWithLongMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V,W> ParallelDoubleArrayWithLongMapping withMapping
        (DoubleAndObjectToLong<? super V> combiner,
         ParallelArrayWithMapping<W,V> other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelDoubleArrayWithLongMapping withMapping
        (DoubleAndDoubleToLong combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelDoubleArrayWithLongMapping withMapping
        (DoubleAndLongToLong combiner,
         ParallelLongArrayWithLongMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mappings of this array using the given mapper that
     * accepts as arguments an element's current index and value
     * (as mapped by preceding mappings, if any), and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public abstract <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
        (IntAndDoubleToObject<? extends V> mapper);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mappings of this array using the given mapper that
     * accepts as arguments an element's current index and value
     * (as mapped by preceding mappings, if any), and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithDoubleMapping withIndexedMapping
        (IntAndDoubleToDouble mapper);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mappings of this array using the given mapper that
     * accepts as arguments an element's current index and value
     * (as mapped by preceding mappings, if any), and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithLongMapping withIndexedMapping
        (IntAndDoubleToLong mapper);

    /**
     * Returns an Iterable view to sequentially step through mapped
     * elements also obeying bound and filter constraints, without
     * performing computations to evaluate them in parallel.
     * @return the Iterable view
     */
    public Iterable<Double> sequentially() {
        return new SequentiallyAsDouble();
    }
}
