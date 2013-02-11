/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import static groovyx.gpars.extra166y.Ops.BinaryDoubleOp;
import static groovyx.gpars.extra166y.Ops.BinaryDoublePredicate;
import static groovyx.gpars.extra166y.Ops.DoubleAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.DoubleAndDoubleToObject;
import static groovyx.gpars.extra166y.Ops.DoubleAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.DoubleAndLongToLong;
import static groovyx.gpars.extra166y.Ops.DoubleAndLongToObject;
import static groovyx.gpars.extra166y.Ops.DoubleAndObjectToDouble;
import static groovyx.gpars.extra166y.Ops.DoubleAndObjectToLong;
import static groovyx.gpars.extra166y.Ops.DoubleAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.DoubleComparator;
import static groovyx.gpars.extra166y.Ops.DoubleGenerator;
import static groovyx.gpars.extra166y.Ops.DoubleOp;
import static groovyx.gpars.extra166y.Ops.DoublePredicate;
import static groovyx.gpars.extra166y.Ops.DoubleProcedure;
import static groovyx.gpars.extra166y.Ops.DoubleReducer;
import static groovyx.gpars.extra166y.Ops.DoubleToLong;
import static groovyx.gpars.extra166y.Ops.DoubleToObject;
import static groovyx.gpars.extra166y.Ops.IntAndDoublePredicate;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToObject;
import static groovyx.gpars.extra166y.Ops.IntToDouble;

/**
 * An array of doubles supporting parallel operations.  This class
 * provides methods supporting the same operations as {@link
 * ParallelArray}, but specialized for scalar doubles. It additionally
 * provides a few methods specific to numerical values.
 */
public class ParallelDoubleArray extends AbstractParallelAnyArray.DUPap {
    // Same internals as ParallelArray, but specialized for doubles
    AsList listView; // lazily constructed

    /**
     * Returns a common default executor for use in ParallelArrays.
     * This executor arranges enough parallelism to use most, but not
     * necessarily all, of the available processors on this system.
     * @return the executor
     */
    public static ForkJoinPool defaultExecutor() {
        return PAS.defaultExecutor();
    }

    /**
     * Constructor for use by subclasses to create a new ParallelDoubleArray
     * using the given executor, and initially using the supplied
     * array, with effective size bound by the given limit. This
     * constructor is designed to enable extensions via
     * subclassing. To create a ParallelDoubleArray, use {@link #create},
     * {@link #createEmpty}, {@link #createUsingHandoff} or {@link
     * #createFromCopy}.
     * @param executor the executor
     * @param array the array
     * @param limit the upper bound limit
     */
    protected ParallelDoubleArray(ForkJoinPool executor, double[] array,
                                  int limit) {
        super(executor, 0, limit, array);
        if (executor == null || array == null)
            throw new NullPointerException();
        if (limit < 0 || limit > array.length)
            throw new IllegalArgumentException();
    }

    /**
     * Trusted internal version of protected constructor.
     */
    ParallelDoubleArray(ForkJoinPool executor, double[] array) {
        super(executor, 0, array.length, array);
    }

    /**
     * Creates a new ParallelDoubleArray using the given executor and
     * an array of the given size.
     * @param size the array size
     * @param executor the executor
     */
    public static ParallelDoubleArray create
        (int size, ForkJoinPool executor) {
        double[] array = new double[size];
        return new ParallelDoubleArray(executor, array, size);
    }

    /**
     * Creates a new ParallelDoubleArray initially using the given array and
     * executor. In general, the handed off array should not be used
     * for other purposes once constructing this ParallelDoubleArray.  The
     * given array may be internally replaced by another array in the
     * course of methods that add or remove elements.
     * @param handoff the array
     * @param executor the executor
     */
    public static ParallelDoubleArray createUsingHandoff
        (double[] handoff, ForkJoinPool executor) {
        return new ParallelDoubleArray(executor, handoff, handoff.length);
    }

    /**
     * Creates a new ParallelDoubleArray using the given executor and
     * initially holding copies of the given
     * source elements.
     * @param source the source of initial elements
     * @param executor the executor
     */
    public static ParallelDoubleArray createFromCopy
        (double[] source, ForkJoinPool executor) {
        // For now, avoid copyOf so people can compile with Java5
        int size = source.length;
        double[] array = new double[size];
        System.arraycopy(source, 0, array, 0, size);
        return new ParallelDoubleArray(executor, array, size);
    }

    /**
     * Creates a new ParallelDoubleArray using an array of the given size,
     * initially holding copies of the given source truncated or
     * padded with zeros to obtain the specified length.
     * @param source the source of initial elements
     * @param size the array size
     * @param executor the executor
     */
    public static ParallelDoubleArray createFromCopy
        (int size, double[] source, ForkJoinPool executor) {
        // For now, avoid copyOf so people can compile with Java5
        double[] array = new double[size];
        System.arraycopy(source, 0, array, 0,
                         Math.min(source.length, size));
        return new ParallelDoubleArray(executor, array, size);
    }

    /**
     * Creates a new ParallelDoubleArray using the given executor and
     * an array of the given size, but with an initial effective size
     * of zero, enabling incremental insertion via {@link
     * ParallelDoubleArray#asList} operations.
     * @param size the array size
     * @param executor the executor
     */
    public static ParallelDoubleArray createEmpty
        (int size, ForkJoinPool executor) {
        double[] array = new double[size];
        return new ParallelDoubleArray(executor, array, 0);
    }

    /**
     * Summary statistics for a possibly bounded, filtered, and/or
     * mapped ParallelDoubleArray.
     */
    public static interface SummaryStatistics {
        /** Returns the number of elements */
        public int size();
        /** Returns the minimum element, or Double.MAX_VALUE if empty */
        public double min();
        /** Returns the maximum element, or -Double.MAX_VALUE if empty */
        public double max();
        /** Returns the index of the minimum element, or -1 if empty */
        public int indexOfMin();
        /** Returns the index of the maximum element, or -1 if empty */
        public int indexOfMax();
        /** Returns the sum of all elements */
        public double sum();
        /** Returns the arithmetic average of all elements */
        public double average();
    }

    /**
     * Returns the executor used for computations.
     * @return the executor
     */
    public ForkJoinPool getExecutor() { return ex; }

    /**
     * Applies the given procedure to elements.
     * @param procedure the procedure
     */
    public void apply(DoubleProcedure procedure) {
        super.apply(procedure);
    }

    /**
     * Returns reduction of elements.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return reduction
     */
    public double reduce(DoubleReducer reducer, double base) {
        return super.reduce(reducer, base);
    }

    /**
     * Returns a new ParallelDoubleArray holding all elements.
     * @return a new ParallelDoubleArray holding all elements
     */
    public ParallelDoubleArray all() {
        return super.all();
    }

    /**
     * Replaces elements with the results of applying the given op
     * to their current values.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray replaceWithMapping(DoubleOp op) {
        super.replaceWithMapping(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their indices.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray replaceWithMappedIndex(IntToDouble op) {
        super.replaceWithMappedIndex(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to each index and current element value.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray replaceWithMappedIndex(IntAndDoubleToDouble op) {
        super.replaceWithMappedIndex(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * generator. For example, to fill the array with uniform random
     * values, use
     * {@code replaceWithGeneratedValue(Ops.doubleRandom())}.
     * @param generator the generator
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray replaceWithGeneratedValue(DoubleGenerator generator) {
        super.replaceWithGeneratedValue(generator);
        return this;
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray replaceWithValue(double value) {
        super.replaceWithValue(value);
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     * @throws ArrayIndexOutOfBoundsException if other array has
     * fewer elements than this array
     */
    public ParallelDoubleArray replaceWithMapping
        (BinaryDoubleOp combiner, ParallelDoubleArrayWithDoubleMapping other) {
        super.replaceWithMapping(combiner, other);
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     * @throws ArrayIndexOutOfBoundsException if other array has
     * fewer elements than this array
     */
    public ParallelDoubleArray replaceWithMapping(BinaryDoubleOp combiner,
                                                  double[] other) {
        super.replaceWithMapping(combiner, other);
        return this;
    }

    /**
     * Returns the index of some element equal to given target, or -1
     * if not present.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public int indexOf(double target) {
        return super.indexOf(target);
    }

    /**
     * Assuming this array is sorted, returns the index of an element
     * equal to given target, or -1 if not present. If the array
     * is not sorted, the results are undefined.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public int binarySearch(double target) {
        return super.binarySearch(target);
    }

    /**
     * Assuming this array is sorted with respect to the given
     * comparator, returns the index of an element equal to given
     * target, or -1 if not present. If the array is not sorted, the
     * results are undefined.
     * @param target the element to search for
     * @param comparator the comparator
     * @return the index or -1 if not present
     */
    public int binarySearch(double target, DoubleComparator comparator) {
        return super.binarySearch(target, comparator);
    }

    /**
     * Returns summary statistics, using the given comparator
     * to locate minimum and maximum elements.
     * @param comparator the comparator to use for
     * locating minimum and maximum elements
     * @return the summary
     */
    public ParallelDoubleArray.SummaryStatistics summary
        (DoubleComparator comparator) {
        return super.summary(comparator);
    }

    /**
     * Returns summary statistics, using natural comparator.
     * @return the summary
     */
    public ParallelDoubleArray.SummaryStatistics summary() {
        return super.summary();
    }

    /**
     * Returns the minimum element, or Double.MAX_VALUE if empty.
     * @param comparator the comparator
     * @return minimum element, or Double.MAX_VALUE if empty
     */
    public double min(DoubleComparator comparator) {
        return super.min(comparator);
    }

    /**
     * Returns the minimum element, or Double.MAX_VALUE if empty.
     * @return minimum element, or Double.MAX_VALUE if empty
     */
    public double min() {
        return super.min();
    }

    /**
     * Returns the maximum element, or -Double.MAX_VALUE if empty.
     * @param comparator the comparator
     * @return maximum element, or -Double.MAX_VALUE if empty
     */
    public double max(DoubleComparator comparator) {
        return super.max(comparator);
    }

    /**
     * Returns the maximum element, or -Double.MAX_VALUE if empty.
     * @return maximum element, or -Double.MAX_VALUE if empty
     */
    public double max() {
        return super.max();
    }

    /**
     * Replaces each element with the running cumulation of applying
     * the given reducer. For example, if the contents are the numbers
     * {@code 1, 2, 3}, and the reducer operation adds numbers, then
     * after invocation of this method, the contents would be {@code 1,
     * 3, 6} (that is, {@code 1, 1+2, 1+2+3}).
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray cumulate(DoubleReducer reducer, double base) {
        super.cumulate(reducer, base);
        return this;
    }

    /**
     * Replaces each element with the cumulation of applying the given
     * reducer to all previous values, and returns the total
     * reduction. For example, if the contents are the numbers {@code 1,
     * 2, 3}, and the reducer operation adds numbers, then after
     * invocation of this method, the contents would be {@code 0, 1,
     * 3} (that is, {@code 0, 0+1, 0+1+2}, and the return value
     * would be 6 (that is, {@code  1+2+3}).
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return the total reduction
     */
    public double precumulate(DoubleReducer reducer, double base) {
        return super.precumulate(reducer, base);
    }

    /**
     * Sorts the array. Unlike Arrays.sort, this sort does
     * not guarantee that elements with equal keys maintain their
     * relative position in the array.
     * @param comparator the comparator to use
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray sort(DoubleComparator comparator) {
        super.sort(comparator);
        return this;
    }

    /**
     * Sorts the array, assuming all elements are Comparable. Unlike
     * Arrays.sort, this sort does not guarantee that elements
     * with equal keys maintain their relative position in the array.
     * @throws ClassCastException if any element is not Comparable
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray sort() {
        super.sort();
        return this;
    }

    /**
     * Removes consecutive elements that are equal,
     * shifting others leftward, and possibly decreasing size.  This
     * method may be used after sorting to ensure that this
     * ParallelDoubleArray contains a set of unique elements.
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray removeConsecutiveDuplicates() {
        // Sequential implementation for now
        int k = 0;
        int n = fence;
        if (k < n) {
            double[] arr = this.array;
            double last = arr[k++];
            for (int i = k; i < n; ++i) {
                double x = arr[i];
                if (last != x)
                    arr[k++] = last = x;
            }
            removeSlotsAt(k, n);
        }
        return this;
    }

    /**
     * Equivalent to {@code asList().addAll} but specialized for
     * array arguments and likely to be more efficient.
     * @param other the elements to add
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray addAll(double[] other) {
        int csize = other.length;
        int end = fence;
        insertSlotsAt(end, csize);
        System.arraycopy(other, 0, array, end, csize);
        return this;
    }

    /**
     * Appends all (possibly bounded, filtered, or mapped) elements of
     * the given ParallelDoubleArray, resizing and/or reallocating this
     * array if necessary.
     * @param other the elements to add
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray addAll(ParallelDoubleArrayWithDoubleMapping other) {
        int end = fence;
        if (other.hasFilter()) {
            PAS.FJDAppendAllDriver r = new PAS.FJDAppendAllDriver
                (other, end, array);
            ex.invoke(r);
            array = r.results;
            fence = end + r.resultSize;
        }
        else {
            int csize = other.size();
            insertSlotsAt(end, csize);
            if (other.hasMap())
                ex.invoke(new PAS.FJDMap(other, other.origin, other.fence,
                                         null, array, end - other.origin));
            else
                System.arraycopy(other.array, 0, array, end, csize);
        }
        return this;
    }

    /**
     * Returns a new ParallelDoubleArray containing only the unique
     * elements of this array (that is, without any duplicates).
     * @return the new ParallelDoubleArray
     */
    public ParallelDoubleArray allUniqueElements() {
        return super.allUniqueElements();
    }

    /**
     * Removes from the array all elements for which the given
     * selector holds.
     * @param selector the selector
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray removeAll(DoublePredicate selector) {
        DFPap v = new DFPap(ex, 0, fence, array, selector);
        PAS.FJRemoveAllDriver f = new PAS.FJRemoveAllDriver(v, 0, fence);
        ex.invoke(f);
        removeSlotsAt(f.offset, fence);
        return this;
    }

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are equal.
     * @param other the other array
     * @return true if equal
     */
    public boolean hasAllEqualElements
        (ParallelDoubleArrayWithDoubleMapping other) {
        return super.hasAllEqualElements(other);
    }

    /**
     * Returns the sum of elements.
     * @return the sum of elements
     */
    public double sum() {
        return super.sum();
    }

    /**
     * Replaces each element with the running sum.
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArray cumulateSum() {
        super.cumulateSum();
        return this;
    }

    /**
     * Replaces each element with its prefix sum.
     * @return the total sum
     */
    public double precumulateSum() {
        return super.precumulateSum();
    }

    /**
     * Returns an operation prefix that causes a method to
     * operate only on the elements of the array between
     * firstIndex (inclusive) and upperBound (exclusive).
     * @param firstIndex the lower bound (inclusive)
     * @param upperBound the upper bound (exclusive)
     * @return operation prefix
     */
    public ParallelDoubleArrayWithBounds withBounds(int firstIndex,
                                                    int upperBound) {
        return super.withBounds(firstIndex, upperBound);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on the elements of the array for which the given selector
     * returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelDoubleArrayWithFilter withFilter(DoublePredicate selector) {
        return super.withFilter(selector);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the given binary selector returns
     * true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelDoubleArrayWithFilter withFilter
        (BinaryDoublePredicate selector,
         ParallelDoubleArrayWithDoubleMapping other) {
        return super.withFilter(selector, other);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the given indexed selector returns
     * true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelDoubleArrayWithFilter withIndexedFilter
        (IntAndDoublePredicate selector) {
        return super.withIndexedFilter(selector);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public <U> ParallelDoubleArrayWithMapping<U> withMapping
        (DoubleToObject<? extends U> op) {
        return super.withMapping(op);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
        return super.withMapping(op);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
        return super.withMapping(op);
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
    public <V,W,X> ParallelDoubleArrayWithMapping<W> withMapping
        (DoubleAndObjectToObject<? super V, ? extends W> combiner,
         ParallelArrayWithMapping<X,V> other) {
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
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
        return super.withMapping(combiner, other);
    }

    /**
     * Returns an operation prefix that causes a method to operate on
     * mappings of this array using the given mapper that accepts as
     * arguments an element's current index and value, and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public <U> ParallelDoubleArrayWithMapping<U> withIndexedMapping
        (IntAndDoubleToObject<? extends U> mapper) {
        return super.withIndexedMapping(mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate on
     * mappings of this array using the given mapper that accepts as
     * arguments an element's current index and value, and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
        (IntAndDoubleToDouble mapper) {
        return super.withIndexedMapping(mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate on
     * mappings of this array using the given mapper that accepts as
     * arguments an element's current index and value, and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public ParallelDoubleArrayWithLongMapping withIndexedMapping
        (IntAndDoubleToLong mapper) {
        return super.withIndexedMapping(mapper);
    }

    /**
     * Returns an iterator stepping through each element of the array
     * up to the current limit. This iterator does <em>not</em>
     * support the remove operation. However, a full
     * {@code ListIterator} supporting add, remove, and set
     * operations is available via {@link #asList}.
     * @return an iterator stepping through each element
     */
    public Iterator<Double> iterator() {
        return new ParallelDoubleArrayIterator(array, fence);
    }

    static final class ParallelDoubleArrayIterator
        implements Iterator<Double> {
        int cursor;
        final double[] arr;
        final int hi;
        ParallelDoubleArrayIterator(double[] a, int limit) { arr = a; hi = limit; }
        public boolean hasNext() { return cursor < hi; }
        public Double next() {
            if (cursor >= hi)
                throw new NoSuchElementException();
            return Double.valueOf(arr[cursor++]);
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // List support

    /**
     * Returns a view of this ParallelDoubleArray as a List. This List
     * has the same structural and performance characteristics as
     * {@link ArrayList}, and may be used to modify, replace or extend
     * the bounds of the array underlying this ParallelDoubleArray.
     * The methods supported by this list view are <em>not</em> in
     * general implemented as parallel operations. This list is also
     * not itself thread-safe.  In particular, performing list updates
     * while other parallel operations are in progress has undefined
     * (and surely undesired) effects.
     * @return a list view
     */
    public List<Double> asList() {
        AsList lv = listView;
        if (lv == null)
            listView = lv = new AsList();
        return lv;
    }

    /**
     * Returns the effective size of the underlying array. The
     * effective size is the current limit, if used (see {@link
     * #setLimit}), or the length of the array otherwise.
     * @return the effective size of array
     */
    public int size() { return fence; }

    /**
     * Returns the underlying array used for computations.
     * @return the array
     */
    public double[] getArray() { return array; }

    /**
     * Returns the element of the array at the given index.
     * @param i the index
     * @return the element of the array at the given index
     */
    public double get(int i) { return array[i]; }

    /**
     * Sets the element of the array at the given index to the given value.
     * @param i the index
     * @param x the value
     */
    public void set(int i, double x) { array[i] = x; }

    /**
     * Equivalent to {@code asList().toString()}.
     * @return a string representation
     */
    public String toString() {
        return asList().toString();
    }

    /**
     * Ensures that the underlying array can be accessed up to the
     * given upper bound, reallocating and copying the underlying
     * array to expand if necessary. Or, if the given limit is less
     * than the length of the underlying array, causes computations to
     * ignore elements past the given limit.
     * @param newLimit the new upper bound
     * @throws IllegalArgumentException if newLimit less than zero
     */
    public final void setLimit(int newLimit) {
        if (newLimit < 0)
            throw new IllegalArgumentException();
        int cap = array.length;
        if (newLimit > cap)
            resizeArray(newLimit);
        fence = newLimit;
    }

    final void resizeArray(int newCap) {
        int cap = array.length;
        if (newCap > cap) {
            double[] a = new double[newCap];
            System.arraycopy(array, 0, a, 0, cap);
            array = a;
        }
    }

    final void insertElementAt(int index, double e) {
        int hi = fence++;
        if (hi >= array.length)
            resizeArray((hi * 3)/2 + 1);
        if (hi > index)
            System.arraycopy(array, index, array, index+1, hi - index);
        array[index] = e;
    }

    final void appendElement(double e) {
        int hi = fence++;
        if (hi >= array.length)
            resizeArray((hi * 3)/2 + 1);
        array[hi] = e;
    }

    /**
     * Makes len slots available at index.
     */
    final void insertSlotsAt(int index, int len) {
        if (len <= 0)
            return;
        int cap = array.length;
        int newSize = fence + len;
        if (cap < newSize) {
            cap = (cap * 3)/2 + 1;
            if (cap < newSize)
                cap = newSize;
            resizeArray(cap);
        }
        if (index < fence)
            System.arraycopy(array, index, array, index + len, fence - index);
        fence = newSize;
    }

    final void removeSlotAt(int index) {
        System.arraycopy(array, index + 1, array, index, fence - index - 1);
        --fence;
    }

    final void removeSlotsAt(int fromIndex, int toIndex) {
        if (fromIndex < toIndex) {
            int size = fence;
            System.arraycopy(array, toIndex, array, fromIndex, size - toIndex);
            int newSize = size - (toIndex - fromIndex);
            fence = newSize;
        }
    }

    final int seqIndexOf(double target) {
        double[] arr = array;
        int end = fence;
        for (int i = 0; i < end; i++)
            if (target == arr[i])
                return i;
        return -1;
    }

    final int seqLastIndexOf(double target) {
        double[] arr = array;
        for (int i = fence - 1; i >= 0; i--)
            if (target == arr[i])
                return i;
        return -1;
    }

    final class ListIter implements ListIterator<Double> {
        int cursor;
        int lastRet;
        double[] arr; // cache array and bound
        int hi;
        ListIter(int lo) {
            this.cursor = lo;
            this.lastRet = -1;
            this.arr = ParallelDoubleArray.this.array;
            this.hi = ParallelDoubleArray.this.fence;
        }

        public boolean hasNext() {
            return cursor < hi;
        }

        public Double next() {
            int i = cursor;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            double next = arr[i];
            lastRet = i;
            cursor = i + 1;
            return Double.valueOf(next);
        }

        public void remove() {
            int k = lastRet;
            if (k < 0)
                throw new IllegalStateException();
            ParallelDoubleArray.this.removeSlotAt(k);
            hi = ParallelDoubleArray.this.fence;
            if (lastRet < cursor)
                cursor--;
            lastRet = -1;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public Double previous() {
            int i = cursor - 1;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            double previous = arr[i];
            lastRet = cursor = i;
            return Double.valueOf(previous);
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void set(Double e) {
            int i = lastRet;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            arr[i] = e.doubleValue();
        }

        public void add(Double e) {
            int i = cursor;
            ParallelDoubleArray.this.insertElementAt(i, e.doubleValue());
            arr = ParallelDoubleArray.this.array;
            hi = ParallelDoubleArray.this.fence;
            lastRet = -1;
            cursor = i + 1;
        }
    }

    final class AsList extends AbstractList<Double> implements RandomAccess {
        public Double get(int i) {
            if (i >= fence)
                throw new IndexOutOfBoundsException();
            return Double.valueOf(array[i]);
        }

        public Double set(int i, Double x) {
            if (i >= fence)
                throw new IndexOutOfBoundsException();
            double[] arr = array;
            Double t = Double.valueOf(arr[i]);
            arr[i] = x.doubleValue();
            return t;
        }

        public boolean isEmpty() {
            return fence == 0;
        }

        public int size() {
            return fence;
        }

        public Iterator<Double> iterator() {
            return new ListIter(0);
        }

        public ListIterator<Double> listIterator() {
            return new ListIter(0);
        }

        public ListIterator<Double> listIterator(int index) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            return new ListIter(index);
        }

        public boolean add(Double e) {
            appendElement(e.doubleValue());
            return true;
        }

        public void add(int index, Double e) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            insertElementAt(index, e.doubleValue());
        }

        public boolean addAll(Collection<? extends Double> c) {
            int csize = c.size();
            if (csize == 0)
                return false;
            int hi = fence;
            setLimit(hi + csize);
            double[] arr = array;
            for (Double e : c)
                arr[hi++] = e.doubleValue();
            return true;
        }

        public boolean addAll(int index, Collection<? extends Double> c) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            int csize = c.size();
            if (csize == 0)
                return false;
            insertSlotsAt(index, csize);
            double[] arr = array;
            for (Double e : c)
                arr[index++] = e.doubleValue();
            return true;
        }

        public void clear() {
            fence = 0;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Double))
                return false;
            int idx = seqIndexOf(((Double)o).doubleValue());
            if (idx < 0)
                return false;
            removeSlotAt(idx);
            return true;
        }

        public Double remove(int index) {
            Double oldValue = get(index);
            removeSlotAt(index);
            return oldValue;
        }

        public void removeRange(int fromIndex, int toIndex) {
            removeSlotsAt(fromIndex, toIndex);
        }

        public boolean contains(Object o) {
            if (!(o instanceof Double))
                return false;
            return seqIndexOf(((Double)o).doubleValue()) >= 0;
        }

        public int indexOf(Object o) {
            if (!(o instanceof Double))
                return -1;
            return seqIndexOf(((Double)o).doubleValue());
        }

        public int lastIndexOf(Object o) {
            if (!(o instanceof Double))
                return -1;
            return seqLastIndexOf(((Double)o).doubleValue());
        }
    }

}
