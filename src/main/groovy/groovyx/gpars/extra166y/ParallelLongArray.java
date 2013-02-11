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

import static groovyx.gpars.extra166y.Ops.BinaryLongOp;
import static groovyx.gpars.extra166y.Ops.BinaryLongPredicate;
import static groovyx.gpars.extra166y.Ops.IntAndLongPredicate;
import static groovyx.gpars.extra166y.Ops.IntAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.IntAndLongToLong;
import static groovyx.gpars.extra166y.Ops.IntAndLongToObject;
import static groovyx.gpars.extra166y.Ops.IntToLong;
import static groovyx.gpars.extra166y.Ops.LongAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.LongAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.LongAndDoubleToObject;
import static groovyx.gpars.extra166y.Ops.LongAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.LongAndLongToObject;
import static groovyx.gpars.extra166y.Ops.LongAndObjectToDouble;
import static groovyx.gpars.extra166y.Ops.LongAndObjectToLong;
import static groovyx.gpars.extra166y.Ops.LongAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.LongComparator;
import static groovyx.gpars.extra166y.Ops.LongGenerator;
import static groovyx.gpars.extra166y.Ops.LongOp;
import static groovyx.gpars.extra166y.Ops.LongPredicate;
import static groovyx.gpars.extra166y.Ops.LongProcedure;
import static groovyx.gpars.extra166y.Ops.LongReducer;
import static groovyx.gpars.extra166y.Ops.LongToDouble;
import static groovyx.gpars.extra166y.Ops.LongToObject;

/**
 * An array of longs supporting parallel operations.  This class
 * provides methods supporting the same operations as {@link
 * ParallelArray}, but specialized for scalar longs. It additionally
 * provides a few methods specific to numerical values.
 *
 * <p><b>Sample usages</b>.
 * Here is a complete (although naive) prime filter program:
 * <pre>
 * import java.math.BigInteger;
 * import jsr166y.*;
 * import static extra166y.Ops.*;
 * import static extra166y.ParallelLongArray.*;
 *
 * public class Sieve {
 *   public static void main(String[] args) {
 *     int n = Integer.parseInt(args[0]);
 *     // create array of divisors
 *     ParallelLongArray a = create(n-1, defaultExecutor());
 *     a.replaceWithMappedIndex(add2);
 *     int i = 0;
 *     long p = 2;
 *     while (p * p &lt; n) { // repeatedly filter
 *         a = a.withFilter(notDivisibleBy(p)).all();
 *         p = a.get(++i);
 *     }
 *     System.out.printf("sieve(%d) = %s%n", n, a);
 *     // check result
 *     if (!a.withFilter(notProbablePrime).isEmpty())
 *         throw new Error();
 *   }
 *   static IntToLong add2 = new IntToLong() {
 *     public long op(int i) { return i + 2; } };
 *   static LongPredicate notDivisibleBy(final long p) {
 *     return new LongPredicate() {
 *       public boolean op(long n) { return n &lt;= p || (n % p) != 0; }
 *     }; }
 *   static LongPredicate notProbablePrime = new LongPredicate() {
 *     private static final int CERTAINTY = 8;
 *     public boolean op(long n) {
 *       return !BigInteger.valueOf(n).isProbablePrime(CERTAINTY);
 *     }
 *   };
 * }
 * </pre>
 */
public class ParallelLongArray extends AbstractParallelAnyArray.LUPap {
    // Same internals as ParallelArray, but specialized for longs
    AsList listView;

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
     * Constructor for use by subclasses to create a new ParallelLongArray
     * using the given executor, and initially using the supplied
     * array, with effective size bound by the given limit. This
     * constructor is designed to enable extensions via
     * subclassing. To create a ParallelLongArray, use {@link #create},
     * {@link #createEmpty}, {@link #createUsingHandoff} or {@link
     * #createFromCopy}.
     * @param executor the executor
     * @param array the array
     * @param limit the upper bound limit
     */
    protected ParallelLongArray(ForkJoinPool executor, long[] array,
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
    ParallelLongArray(ForkJoinPool executor, long[] array) {
        super(executor, 0, array.length, array);
    }

    /**
     * Creates a new ParallelLongArray using the given executor and
     * an array of the given size.
     * @param size the array size
     * @param executor the executor
     */
    public static ParallelLongArray create
        (int size, ForkJoinPool executor) {
        long[] array = new long[size];
        return new ParallelLongArray(executor, array, size);
    }

    /**
     * Creates a new ParallelLongArray initially using the given array and
     * executor. In general, the handed off array should not be used
     * for other purposes once constructing this ParallelLongArray.  The
     * given array may be internally replaced by another array in the
     * course of methods that add or remove elements.
     * @param handoff the array
     * @param executor the executor
     */
    public static ParallelLongArray createUsingHandoff
        (long[] handoff, ForkJoinPool executor) {
        return new ParallelLongArray(executor, handoff, handoff.length);
    }

    /**
     * Creates a new ParallelLongArray using the given executor and
     * initially holding copies of the given
     * source elements.
     * @param source the source of initial elements
     * @param executor the executor
     */
    public static ParallelLongArray createFromCopy
        (long[] source, ForkJoinPool executor) {
        // For now, avoid copyOf so people can compile with Java5
        int size = source.length;
        long[] array = new long[size];
        System.arraycopy(source, 0, array, 0, size);
        return new ParallelLongArray(executor, array, size);
    }

    /**
     * Creates a new ParallelLongArray using an array of the given size,
     * initially holding copies of the given source truncated or
     * padded with zeros to obtain the specified length.
     * @param source the source of initial elements
     * @param size the array size
     * @param executor the executor
     */
    public static ParallelLongArray createFromCopy
        (int size, long[] source, ForkJoinPool executor) {
        // For now, avoid copyOf so people can compile with Java5
        long[] array = new long[size];
        System.arraycopy(source, 0, array, 0,
                         Math.min(source.length, size));
        return new ParallelLongArray(executor, array, size);
    }

    /**
     * Creates a new ParallelLongArray using the given executor and
     * an array of the given size, but with an initial effective size
     * of zero, enabling incremental insertion via {@link
     * ParallelLongArray#asList} operations.
     * @param size the array size
     * @param executor the executor
     */
    public static ParallelLongArray createEmpty
        (int size, ForkJoinPool executor) {
        long[] array = new long[size];
        return new ParallelLongArray(executor, array, 0);
    }

    /**
     * Summary statistics for a possibly bounded, filtered, and/or
     * mapped ParallelLongArray.
     */
    public static interface SummaryStatistics {
        /** Returns the number of elements */
        public int size();
        /** Returns the minimum element, or Long.MAX_VALUE if empty */
        public long min();
        /** Returns the maximum element, or Long.MIN_VALUE if empty */
        public long max();
        /** Returns the index of the minimum element, or -1 if empty */
        public int indexOfMin();
        /** Returns the index of the maximum element, or -1 if empty */
        public int indexOfMax();
        /** Returns the sum of all elements */
        public long sum();
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
    public void apply(LongProcedure procedure) {
        super.apply(procedure);
    }

    /**
     * Returns reduction of elements.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return reduction
     */
    public long reduce(LongReducer reducer, long base) {
        return super.reduce(reducer, base);
    }

    /**
     * Returns a new ParallelLongArray holding all elements.
     * @return a new ParallelLongArray holding all elements
     */
    public ParallelLongArray all() {
        return super.all();
    }

    /**
     * Replaces elements with the results of applying the given op
     * to their current values.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray replaceWithMapping(LongOp op) {
        super.replaceWithMapping(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their indices.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray replaceWithMappedIndex(IntToLong op) {
        super.replaceWithMappedIndex(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to each index and current element value.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray replaceWithMappedIndex(IntAndLongToLong op) {
        super.replaceWithMappedIndex(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * generator. For example, to fill the array with uniform random
     * values, use
     * {@code replaceWithGeneratedValue(Ops.longRandom())}.
     * @param generator the generator
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray replaceWithGeneratedValue(LongGenerator generator){
        super.replaceWithGeneratedValue(generator);
        return this;
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray replaceWithValue(long value) {
        super.replaceWithValue(value);
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray replaceWithMapping
        (BinaryLongOp combiner,
         ParallelLongArrayWithLongMapping other) {
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
    public ParallelLongArray replaceWithMapping(BinaryLongOp combiner,
                                                long[] other) {
        super.replaceWithMapping(combiner, other);
        return this;
    }

    /**
     * Returns the index of some element equal to given target, or -1
     * if not present.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public int indexOf(long target) {
        return super.indexOf(target);
    }

    /**
     * Assuming this array is sorted, returns the index of an element
     * equal to given target, or -1 if not present. If the array
     * is not sorted, the results are undefined.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public int binarySearch(long target) {
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
    public int binarySearch(long target, LongComparator comparator) {
        return super.binarySearch(target, comparator);
    }

    /**
     * Returns summary statistics, using the given comparator
     * to locate minimum and maximum elements.
     * @param comparator the comparator to use for
     * locating minimum and maximum elements
     * @return the summary
     */
    public ParallelLongArray.SummaryStatistics summary
        (LongComparator comparator) {
        return super.summary(comparator);
    }

    /**
     * Returns summary statistics, using natural comparator.
     * @return the summary
     */
    public ParallelLongArray.SummaryStatistics summary() {
        return super.summary();
    }

    /**
     * Returns the minimum element, or Long.MAX_VALUE if empty.
     * @param comparator the comparator
     * @return minimum element, or Long.MAX_VALUE if empty
     */
    public long min(LongComparator comparator) {
        return super.min(comparator);
    }

    /**
     * Returns the minimum element, or Long.MAX_VALUE if empty.
     * @return minimum element, or Long.MAX_VALUE if empty
     */
    public long min() {
        return super.min();
    }

    /**
     * Returns the maximum element, or Long.MIN_VALUE if empty.
     * @param comparator the comparator
     * @return maximum element, or Long.MIN_VALUE if empty
     */
    public long max(LongComparator comparator) {
        return super.max(comparator);
    }

    /**
     * Returns the maximum element, or Long.MIN_VALUE if empty.
     * @return maximum element, or Long.MIN_VALUE if empty
     */
    public long max() {
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
    public ParallelLongArray cumulate(LongReducer reducer, long base) {
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
    public long precumulate(LongReducer reducer, long base) {
        return super.precumulate(reducer, base);
    }

    /**
     * Sorts the array. Unlike Arrays.sort, this sort does
     * not guarantee that elements with equal keys maintain their
     * relative position in the array.
     * @param comparator the comparator to use
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray sort(LongComparator comparator) {
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
    public ParallelLongArray sort() {
        super.sort();
        return this;
    }

    /**
     * Removes consecutive elements that are equal,
     * shifting others leftward, and possibly decreasing size.  This
     * method may be used after sorting to ensure that this
     * ParallelLongArray contains a set of unique elements.
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray removeConsecutiveDuplicates() {
        // Sequential implementation for now
        int k = 0;
        int n = fence;
        if (k < n) {
            long[] arr = this.array;
            long last = arr[k++];
            for (int i = k; i < n; ++i) {
                long x = arr[i];
                if (last != x)
                    arr[k++] = last = x;
            }
            removeSlotsAt(k, n);
        }
        return this;
    }

    /**
     * Equivalent to {@code asList().addAll} but specialized for array
     * arguments and likely to be more efficient.
     * @param other the elements to add
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray addAll(long[] other) {
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
    public ParallelLongArray addAll(ParallelLongArrayWithLongMapping other) {
        int end = fence;
        if (other.hasFilter()) {
            PAS.FJLAppendAllDriver r = new PAS.FJLAppendAllDriver
                (other, end, array);
            ex.invoke(r);
            array = r.results;
            fence = end + r.resultSize;
        }
        else {
            int csize = other.size();
            insertSlotsAt(end, csize);
            if (other.hasMap())
                ex.invoke(new PAS.FJLMap(other, other.origin, other.fence,
                                         null, array, end - other.origin));
            else
                System.arraycopy(other.array, 0, array, end, csize);
        }
        return this;
    }

    /**
     * Returns a new ParallelLongArray containing only the unique
     * elements of this array (that is, without any duplicates).
     * @return the new ParallelLongArray
     */
    public ParallelLongArray allUniqueElements() {
        return super.allUniqueElements();
    }

    /**
     * Removes from the array all elements for which the given
     * selector holds.
     * @param selector the selector
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray removeAll(LongPredicate selector) {
        LFPap v = new LFPap(ex, 0, fence, array, selector);
        PAS.FJRemoveAllDriver f = new PAS.FJRemoveAllDriver(v, 0, fence);
        ex.invoke(f);
        removeSlotsAt(f.offset, fence);
        return this;
    }

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are equal.
     * @param other the other array
     */
    public boolean hasAllEqualElements(ParallelLongArrayWithLongMapping other) {
        return super.hasAllEqualElements(other);
    }

    /**
     * Returns the sum of elements.
     * @return the sum of elements
     */
    public long sum() {
        return super.sum();
    }

    /**
     * Replaces each element with the running sum.
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArray cumulateSum() {
        super.cumulateSum();
        return this;
    }

    /**
     * Replaces each element with its prefix sum.
     * @return the total sum
     */
    public long precumulateSum() {
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
    public ParallelLongArrayWithBounds withBounds(int firstIndex,
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
    public ParallelLongArrayWithFilter withFilter(LongPredicate selector) {
        return super.withFilter(selector);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the given binary selector returns
     * true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelLongArrayWithFilter withFilter
        (BinaryLongPredicate selector,
         ParallelLongArrayWithLongMapping other) {
        return super.withFilter(selector, other);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the given indexed selector returns
     * true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelLongArrayWithFilter withIndexedFilter
        (IntAndLongPredicate selector) {
        return super.withIndexedFilter(selector);
    }


    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public <U> ParallelLongArrayWithMapping<U> withMapping
        (LongToObject<? extends U> op) {
        return super.withMapping(op);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
        return super.withMapping(op);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
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
    public <V,W,X> ParallelLongArrayWithMapping<W> withMapping
        (LongAndObjectToObject<? super V, ? extends W> combiner,
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
    public <V> ParallelLongArrayWithMapping<V> withMapping
        (LongAndDoubleToObject<? extends V> combiner,
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
    public <V> ParallelLongArrayWithMapping<V> withMapping
        (LongAndLongToObject<? extends V> combiner,
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
    public <V,W> ParallelLongArrayWithDoubleMapping withMapping
        (LongAndObjectToDouble<? super V> combiner,
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
    public ParallelLongArrayWithDoubleMapping withMapping
        (LongAndDoubleToDouble combiner,
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
    public ParallelLongArrayWithDoubleMapping withMapping
        (LongAndLongToDouble combiner,
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
    public <V,W> ParallelLongArrayWithLongMapping withMapping
        (LongAndObjectToLong<? super V> combiner,
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
    public ParallelLongArrayWithLongMapping withMapping
        (LongAndDoubleToLong combiner,
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
    public ParallelLongArrayWithLongMapping withMapping
        (BinaryLongOp combiner,
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
    public <U> ParallelLongArrayWithMapping<U> withIndexedMapping
        (IntAndLongToObject<? extends U> mapper) {
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
    public ParallelLongArrayWithDoubleMapping withIndexedMapping
        (IntAndLongToDouble mapper) {
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
    public ParallelLongArrayWithLongMapping withIndexedMapping
        (IntAndLongToLong mapper) {
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
    public Iterator<Long> iterator() {
        return new ParallelLongArrayIterator(array, fence);
    }

    static final class ParallelLongArrayIterator implements Iterator<Long> {
        int cursor;
        final long[] arr;
        final int hi;
        ParallelLongArrayIterator(long[] a, int limit) { arr = a; hi = limit; }
        public boolean hasNext() { return cursor < hi; }
        public Long next() {
            if (cursor >= hi)
                throw new NoSuchElementException();
            return Long.valueOf(arr[cursor++]);
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // List support

    /**
     * Returns a view of this ParallelLongArray as a List. This List has
     * the same structural and performance characteristics as {@link
     * ArrayList}, and may be used to modify, replace or extend the
     * bounds of the array underlying this ParallelLongArray.  The methods
     * supported by this list view are <em>not</em> in general
     * implemented as parallel operations. This list is also not
     * itself thread-safe.  In particular, performing list updates
     * while other parallel operations are in progress has undefined
     * (and surely undesired) effects.
     * @return a list view
     */
    public List<Long> asList() {
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
    public long[] getArray() { return array; }

    /**
     * Returns the element of the array at the given index.
     * @param i the index
     * @return the element of the array at the given index
     */
    public long get(int i) { return array[i]; }

    /**
     * Sets the element of the array at the given index to the given value.
     * @param i the index
     * @param x the value
     */
    public void set(int i, long x) { array[i] = x; }

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

    final void replaceElementsParallelLongArrayWith(long[] a) {
        System.arraycopy(a, 0, array, 0, a.length);
        fence = a.length;
    }

    final void resizeArray(int newCap) {
        int cap = array.length;
        if (newCap > cap) {
            long[] a = new long[newCap];
            System.arraycopy(array, 0, a, 0, cap);
            array = a;
        }
    }

    final void insertElementAt(int index, long e) {
        int hi = fence++;
        if (hi >= array.length)
            resizeArray((hi * 3)/2 + 1);
        if (hi > index)
            System.arraycopy(array, index, array, index+1, hi - index);
        array[index] = e;
    }

    final void appendElement(long e) {
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

    final int seqIndexOf(long target) {
        long[] arr = array;
        int end = fence;
        for (int i = 0; i < end; i++)
            if (target == arr[i])
                return i;
        return -1;
    }

    final int seqLastIndexOf(long target) {
        long[] arr = array;
        for (int i = fence - 1; i >= 0; i--)
            if (target == arr[i])
                return i;
        return -1;
    }

    final class ListIter implements ListIterator<Long> {
        int cursor;
        int lastRet;
        long[] arr; // cache array and bound
        int hi;
        ListIter(int lo) {
            this.cursor = lo;
            this.lastRet = -1;
            this.arr = ParallelLongArray.this.array;
            this.hi = ParallelLongArray.this.fence;
        }

        public boolean hasNext() {
            return cursor < hi;
        }

        public Long next() {
            int i = cursor;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            long next = arr[i];
            lastRet = i;
            cursor = i + 1;
            return Long.valueOf(next);
        }

        public void remove() {
            int k = lastRet;
            if (k < 0)
                throw new IllegalStateException();
            ParallelLongArray.this.removeSlotAt(k);
            hi = ParallelLongArray.this.fence;
            if (lastRet < cursor)
                cursor--;
            lastRet = -1;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public Long previous() {
            int i = cursor - 1;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            long previous = arr[i];
            lastRet = cursor = i;
            return Long.valueOf(previous);
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void set(Long e) {
            int i = lastRet;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            arr[i] = e.longValue();
        }

        public void add(Long e) {
            int i = cursor;
            ParallelLongArray.this.insertElementAt(i, e.longValue());
            arr = ParallelLongArray.this.array;
            hi = ParallelLongArray.this.fence;
            lastRet = -1;
            cursor = i + 1;
        }
    }

    final class AsList extends AbstractList<Long> implements RandomAccess {
        public Long get(int i) {
            if (i >= fence)
                throw new IndexOutOfBoundsException();
            return Long.valueOf(array[i]);
        }

        public Long set(int i, Long x) {
            if (i >= fence)
                throw new IndexOutOfBoundsException();
            long[] arr = array;
            Long t = Long.valueOf(arr[i]);
            arr[i] = x.longValue();
            return t;
        }

        public boolean isEmpty() {
            return fence == 0;
        }

        public int size() {
            return fence;
        }

        public Iterator<Long> iterator() {
            return new ListIter(0);
        }

        public ListIterator<Long> listIterator() {
            return new ListIter(0);
        }

        public ListIterator<Long> listIterator(int index) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            return new ListIter(index);
        }

        public boolean add(Long e) {
            appendElement(e.longValue());
            return true;
        }

        public void add(int index, Long e) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            insertElementAt(index, e.longValue());
        }

        public boolean addAll(Collection<? extends Long> c) {
            int csize = c.size();
            if (csize == 0)
                return false;
            int hi = fence;
            setLimit(hi + csize);
            long[] arr = array;
            for (Long e : c)
                arr[hi++] = e.longValue();
            return true;
        }

        public boolean addAll(int index, Collection<? extends Long> c) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            int csize = c.size();
            if (csize == 0)
                return false;
            insertSlotsAt(index, csize);
            long[] arr = array;
            for (Long e : c)
                arr[index++] = e.longValue();
            return true;
        }

        public void clear() {
            fence = 0;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Long))
                return false;
            int idx = seqIndexOf(((Long)o).longValue());
            if (idx < 0)
                return false;
            removeSlotAt(idx);
            return true;
        }

        public Long remove(int index) {
            Long oldValue = get(index);
            removeSlotAt(index);
            return oldValue;
        }

        public void removeRange(int fromIndex, int toIndex) {
            removeSlotsAt(fromIndex, toIndex);
        }

        public boolean contains(Object o) {
            if (!(o instanceof Long))
                return false;
            return seqIndexOf(((Long)o).longValue()) >= 0;
        }

        public int indexOf(Object o) {
            if (!(o instanceof Long))
                return -1;
            return seqIndexOf(((Long)o).longValue());
        }

        public int lastIndexOf(Object o) {
            if (!(o instanceof Long))
                return -1;
            return seqLastIndexOf(((Long)o).longValue());
        }
    }

}
