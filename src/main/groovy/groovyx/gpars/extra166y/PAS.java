/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import jsr166y.RecursiveAction;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongArray;

import static groovyx.gpars.extra166y.Ops.BinaryDoubleOp;
import static groovyx.gpars.extra166y.Ops.BinaryLongOp;
import static groovyx.gpars.extra166y.Ops.BinaryOp;
import static groovyx.gpars.extra166y.Ops.DoubleComparator;
import static groovyx.gpars.extra166y.Ops.DoubleGenerator;
import static groovyx.gpars.extra166y.Ops.DoubleOp;
import static groovyx.gpars.extra166y.Ops.DoubleProcedure;
import static groovyx.gpars.extra166y.Ops.DoubleReducer;
import static groovyx.gpars.extra166y.Ops.Generator;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.IntAndLongToLong;
import static groovyx.gpars.extra166y.Ops.IntAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.IntToDouble;
import static groovyx.gpars.extra166y.Ops.IntToLong;
import static groovyx.gpars.extra166y.Ops.IntToObject;
import static groovyx.gpars.extra166y.Ops.LongComparator;
import static groovyx.gpars.extra166y.Ops.LongGenerator;
import static groovyx.gpars.extra166y.Ops.LongOp;
import static groovyx.gpars.extra166y.Ops.LongProcedure;
import static groovyx.gpars.extra166y.Ops.LongReducer;
import static groovyx.gpars.extra166y.Ops.Op;
import static groovyx.gpars.extra166y.Ops.Procedure;
import static groovyx.gpars.extra166y.Ops.Reducer;

/**
 * Shared internal execution support for ParallelArray and
 * specializations.
 */
class PAS {
    private PAS() {} // all-static, non-instantiable

    /** Global default executor */
    private static volatile ForkJoinPool defaultExecutor;
    /** Lock for on-demand initialization of defaultExecutor */
    private static final Object poolLock = new Object();

    static ForkJoinPool defaultExecutor() {
        ForkJoinPool p = defaultExecutor; // double-check
        if (p == null) {
            synchronized (poolLock) {
                p = defaultExecutor;
                if (p == null) {
                    // use ceil(7/8 * ncpus)
                    int nprocs = Runtime.getRuntime().availableProcessors();
                    int nthreads = nprocs - (nprocs >>> 3);
                    defaultExecutor = p = new ForkJoinPool(nthreads);
                }
            }
        }
        return p;
    }

    /**
     * Base for most divide-and-conquer tasks used for computing
     * ParallelArray operations. Rather than pure recursion, it links
     * right-hand-sides and then joins up the tree, exploiting cases
     * where tasks aren't stolen.  This generates and joins tasks with
     * a bit less overhead than pure recursive style -- there are only
     * as many tasks as leaves (no strictly internal nodes).
     *
     * Split control relies on pap.getThreshold(), which is
     * expected to err on the side of generating too many tasks. To
     * counterbalance, if a task pops off its own smallest subtask, it
     * directly runs its leaf action rather than possibly resplitting.
     *
     * There are, with a few exceptions, three flavors of each FJBase
     * subclass, prefixed FJO (object reference), FJD (double) and FJL
     * (long).
     */
    abstract static class FJBase extends RecursiveAction {
        final AbstractParallelAnyArray pap;
        final int lo;
        final int hi;
        final FJBase next; // the next task that creator should join
        FJBase(AbstractParallelAnyArray pap, int lo, int hi, FJBase next) {
            this.pap = pap;
            this.lo = lo;
            this.hi = hi;
            this.next = next;
        }

        public final void compute() {
            int g = pap.getThreshold();
            int l = lo;
            int h = hi;
            if (h - l > g)
                internalCompute(l, h, g);
            else
                atLeaf(l, h);
        }

        final void internalCompute(int l, int h, int g) {
            FJBase r = null;
            do {
                int rh = h;
                h = (l + h) >>> 1;
                (r = newSubtask(h, rh, r)).fork();
            } while (h - l > g);
            atLeaf(l, h);
            do {
                if (r.tryUnfork()) r.atLeaf(r.lo, r.hi); else r.join();
                onReduce(r);
                r = r.next;
            } while (r != null);
        }

        /** Leaf computation */
        abstract void atLeaf(int l, int h);
        /** Operation performed after joining right subtask -- default noop */
        void onReduce(FJBase right) {}
        /** Factory method to create new subtask, normally of current type */
        abstract FJBase newSubtask(int l, int h, FJBase r);
    }

    // apply

    static final class FJOApply extends FJBase {
        final Procedure procedure;
        FJOApply(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                 Procedure procedure) {
            super(pap, lo, hi, next);
            this.procedure = procedure;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOApply(pap, l, h, r, procedure);
        }
        void atLeaf(int l, int h) {
            pap.leafApply(l, h, procedure);
        }
    }

    static final class FJDApply extends FJBase {
        final DoubleProcedure procedure;
        FJDApply(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                 DoubleProcedure procedure) {
            super(pap, lo, hi, next);
            this.procedure = procedure;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDApply(pap, l, h, r, procedure);
        }
        void atLeaf(int l, int h) {
            pap.leafApply(l, h, procedure);
        }
    }

    static final class FJLApply extends FJBase {
        final LongProcedure procedure;
        FJLApply(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                 LongProcedure procedure) {
            super(pap, lo, hi, next);
            this.procedure = procedure;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLApply(pap, l, h, r, procedure);
        }
        void atLeaf(int l, int h) {
            pap.leafApply(l, h, procedure);
        }
    }

    // reduce

    static final class FJOReduce extends FJBase {
        final Reducer reducer;
        Object result;
        FJOReduce(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                  Reducer reducer, Object base) {
            super(pap, lo, hi, next);
            this.reducer = reducer;
            this.result = base;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOReduce(pap, l, h, r, reducer, result);
        }
        void atLeaf(int l, int h) {
            result = pap.leafReduce(l, h, reducer, result);
        }
        void onReduce(FJBase right) {
            result = reducer.op(result, ((FJOReduce)right).result);
        }
    }

    static final class FJDReduce extends FJBase {
        final DoubleReducer reducer;
        double result;
        FJDReduce(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                  DoubleReducer reducer, double base) {
            super(pap, lo, hi, next);
            this.reducer = reducer;
            this.result = base;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDReduce(pap, l, h, r, reducer, result);
        }
        void atLeaf(int l, int h) {
            result = pap.leafReduce(l, h, reducer, result);
        }
        void onReduce(FJBase right) {
            result = reducer.op(result, ((FJDReduce)right).result);
        }
    }

    static final class FJLReduce extends FJBase {
        final LongReducer reducer;
        long result;
        FJLReduce(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                  LongReducer reducer, long base) {
            super(pap, lo, hi, next);
            this.reducer = reducer;
            this.result = base;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLReduce(pap, l, h, r, reducer, result);
        }
        void atLeaf(int l, int h) {
            result = pap.leafReduce(l, h, reducer, result);
        }
        void onReduce(FJBase right) {
            result = reducer.op(result, ((FJLReduce)right).result);
        }
    }

    // map

    static final class FJOMap extends FJBase {
        final Object[] dest;
        final int offset;
        FJOMap(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
               Object[] dest, int offset) {
            super(pap, lo, hi, next);
            this.dest = dest;
            this.offset = offset;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOMap(pap, l, h, r, dest, offset);
        }
        void atLeaf(int l, int h) {
            pap.leafTransfer(l, h, dest, l + offset);
        }
    }

    static final class FJDMap extends FJBase {
        final double[] dest;
        final int offset;
        FJDMap(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
               double[] dest, int offset) {
            super(pap, lo, hi, next);
            this.dest = dest;
            this.offset = offset;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDMap(pap, l, h, r, dest, offset);
        }
        void atLeaf(int l, int h) {
            pap.leafTransfer(l, h, dest, l + offset);
        }
    }

    static final class FJLMap extends FJBase {
        final long[] dest;
        final int offset;
        FJLMap(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
               long[] dest, int offset) {
            super(pap, lo, hi, next);
            this.dest = dest;
            this.offset = offset;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLMap(pap, l, h, r, dest, offset);
        }
        void atLeaf(int l, int h) {
            pap.leafTransfer(l, h, dest, l + offset);
        }
    }

    // transform

    static final class FJOTransform extends FJBase {
        final Op op;
        FJOTransform(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                     Op op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOTransform(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafTransform(l, h, op);
        }
    }

    static final class FJDTransform extends FJBase {
        final DoubleOp op;
        FJDTransform(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                     DoubleOp op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDTransform(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafTransform(l, h, op);
        }
    }

    static final class FJLTransform extends FJBase {
        final LongOp op;
        FJLTransform(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                     LongOp op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLTransform(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafTransform(l, h, op);
        }
    }

    // index map

    static final class FJOIndexMap extends FJBase {
        final IntToObject op;
        FJOIndexMap(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                    IntToObject op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOIndexMap(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafIndexMap(l, h, op);
        }
    }

    static final class FJDIndexMap extends FJBase {
        final IntToDouble op;
        FJDIndexMap(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                    IntToDouble op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDIndexMap(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafIndexMap(l, h, op);
        }
    }

    static final class FJLIndexMap extends FJBase {
        final IntToLong op;
        FJLIndexMap(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                    IntToLong op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLIndexMap(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafIndexMap(l, h, op);
        }
    }

    // binary index map

    static final class FJOBinaryIndexMap extends FJBase {
        final IntAndObjectToObject op;
        FJOBinaryIndexMap(AbstractParallelAnyArray pap, int lo, int hi,
                          FJBase next, IntAndObjectToObject op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOBinaryIndexMap(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafBinaryIndexMap(l, h, op);
        }
    }

    static final class FJDBinaryIndexMap extends FJBase {
        final IntAndDoubleToDouble op;
        FJDBinaryIndexMap(AbstractParallelAnyArray pap, int lo, int hi,
                          FJBase next, IntAndDoubleToDouble op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDBinaryIndexMap(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafBinaryIndexMap(l, h, op);
        }
    }

    static final class FJLBinaryIndexMap extends FJBase {
        final IntAndLongToLong op;
        FJLBinaryIndexMap(AbstractParallelAnyArray pap, int lo, int hi,
                          FJBase next, IntAndLongToLong op) {
            super(pap, lo, hi, next);
            this.op = op;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLBinaryIndexMap(pap, l, h, r, op);
        }
        void atLeaf(int l, int h) {
            pap.leafBinaryIndexMap(l, h, op);
        }
    }


    // generate

    static final class FJOGenerate extends FJBase {
        final Generator generator;
        FJOGenerate(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                    Generator generator) {
            super(pap, lo, hi, next);
            this.generator = generator;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOGenerate(pap, l, h, r, generator);
        }
        void atLeaf(int l, int h) {
            pap.leafGenerate(l, h, generator);
        }
    }

    static final class FJDGenerate extends FJBase {
        final DoubleGenerator generator;
        FJDGenerate(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                    DoubleGenerator generator) {
            super(pap, lo, hi, next);
            this.generator = generator;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDGenerate(pap, l, h, r, generator);
        }
        void atLeaf(int l, int h) {
            pap.leafGenerate(l, h, generator);
        }
    }

    static final class FJLGenerate extends FJBase {
        final LongGenerator generator;
        FJLGenerate(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                    LongGenerator generator) {
            super(pap, lo, hi, next);
            this.generator = generator;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLGenerate(pap, l, h, r, generator);
        }
        void atLeaf(int l, int h) {
            pap.leafGenerate(l, h, generator);
        }
    }

    // fill

    static final class FJOFill extends FJBase {
        final Object value;
        FJOFill(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                Object value) {
            super(pap, lo, hi, next);
            this.value = value;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOFill(pap, l, h, r, value);
        }
        void atLeaf(int l, int h) {
            pap.leafFill(l, h, value);
        }
    }

    static final class FJDFill extends FJBase {
        final double value;
        FJDFill(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                double value) {
            super(pap, lo, hi, next);
            this.value = value;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDFill(pap, l, h, r, value);
        }
        void atLeaf(int l, int h) {
            pap.leafFill(l, h, value);
        }
    }

    static final class FJLFill extends FJBase {
        final long value;
        FJLFill(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                long value) {
            super(pap, lo, hi, next);
            this.value = value;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLFill(pap, l, h, r, value);
        }
        void atLeaf(int l, int h) {
            pap.leafFill(l, h, value);
        }
    }

    // combine in place

    static final class FJOCombineInPlace extends FJBase {
        final Object[] other;
        final int otherOffset;
        final BinaryOp combiner;
        FJOCombineInPlace(AbstractParallelAnyArray pap, int lo, int hi,
                          FJBase next, Object[] other, int otherOffset,
                          BinaryOp combiner) {
            super(pap, lo, hi, next);
            this.other = other;
            this.otherOffset = otherOffset;
            this.combiner = combiner;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOCombineInPlace
                (pap, l, h, r, other, otherOffset, combiner);
        }
        void atLeaf(int l, int h) {
            pap.leafCombineInPlace(l, h, other, otherOffset, combiner);
        }
    }

    static final class FJDCombineInPlace extends FJBase {
        final double[] other;
        final int otherOffset;
        final BinaryDoubleOp combiner;
        FJDCombineInPlace(AbstractParallelAnyArray pap, int lo, int hi,
                          FJBase next, double[] other, int otherOffset,
                          BinaryDoubleOp combiner) {
            super(pap, lo, hi, next);
            this.other = other;
            this.otherOffset = otherOffset;
            this.combiner = combiner;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDCombineInPlace
                (pap, l, h, r, other, otherOffset, combiner);
        }
        void atLeaf(int l, int h) {
            pap.leafCombineInPlace(l, h, other, otherOffset, combiner);
        }
    }

    static final class FJLCombineInPlace extends FJBase {
        final long[] other;
        final int otherOffset;
        final BinaryLongOp combiner;
        FJLCombineInPlace(AbstractParallelAnyArray pap, int lo, int hi,
                          FJBase next, long[] other, int otherOffset,
                          BinaryLongOp combiner) {
            super(pap, lo, hi, next);
            this.other = other;
            this.otherOffset = otherOffset;
            this.combiner = combiner;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLCombineInPlace
                (pap, l, h, r, other, otherOffset, combiner);
        }
        void atLeaf(int l, int h) {
            pap.leafCombineInPlace(l, h, other, otherOffset, combiner);
        }
    }

    static final class FJOPACombineInPlace extends FJBase {
        final ParallelArrayWithMapping other;
        final int otherOffset;
        final BinaryOp combiner;
        FJOPACombineInPlace(AbstractParallelAnyArray pap, int lo, int hi,
                            FJBase next,
                            ParallelArrayWithMapping other, int otherOffset,
                            BinaryOp combiner) {
            super(pap, lo, hi, next);
            this.other = other;
            this.otherOffset = otherOffset;
            this.combiner = combiner;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOPACombineInPlace
                (pap, l, h, r, other, otherOffset, combiner);
        }
        void atLeaf(int l, int h) {
            pap.leafCombineInPlace(l, h, other, otherOffset, combiner);
        }
    }

    static final class FJDPACombineInPlace extends FJBase {
        final ParallelDoubleArrayWithDoubleMapping other;
        final int otherOffset;
        final BinaryDoubleOp combiner;
        FJDPACombineInPlace(AbstractParallelAnyArray pap, int lo, int hi,
                            FJBase next,
                            ParallelDoubleArrayWithDoubleMapping other,
                            int otherOffset, BinaryDoubleOp combiner) {
            super(pap, lo, hi, next);
            this.other = other;
            this.otherOffset = otherOffset;
            this.combiner = combiner;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDPACombineInPlace
                (pap, l, h, r, other, otherOffset, combiner);
        }
        void atLeaf(int l, int h) {
            pap.leafCombineInPlace(l, h, other, otherOffset, combiner);
        }
    }

    static final class FJLPACombineInPlace extends FJBase {
        final ParallelLongArrayWithLongMapping other;
        final int otherOffset;
        final BinaryLongOp combiner;
        FJLPACombineInPlace(AbstractParallelAnyArray pap, int lo, int hi,
                            FJBase next,
                            ParallelLongArrayWithLongMapping other,
                            int otherOffset, BinaryLongOp combiner) {
            super(pap, lo, hi, next);
            this.other = other;
            this.otherOffset = otherOffset;
            this.combiner = combiner;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLPACombineInPlace
                (pap, l, h, r, other, otherOffset, combiner);
        }
        void atLeaf(int l, int h) {
            pap.leafCombineInPlace(l, h, other, otherOffset, combiner);
        }
    }

    // stats

    static final class FJOStats extends FJBase
        implements ParallelArray.SummaryStatistics {
        final Comparator comparator;
        public int size() { return size; }
        public Object min() { return min; }
        public Object max() { return max; }
        public int indexOfMin() { return indexOfMin; }
        public int indexOfMax() { return indexOfMax; }
        int size;
        Object min;
        Object max;
        int indexOfMin;
        int indexOfMax;
        FJOStats(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                 Comparator comparator) {
            super(pap, lo, hi, next);
            this.comparator = comparator;
            this.indexOfMin = -1;
            this.indexOfMax = -1;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOStats(pap, l, h, r, comparator);
        }
        void onReduce(FJBase right) {
            FJOStats r = (FJOStats)right;
            size += r.size;
            updateMin(r.indexOfMin, r.min);
            updateMax(r.indexOfMax, r.max);
        }
        void updateMin(int i, Object x) {
            if (i >= 0 &&
                (indexOfMin < 0 || comparator.compare(min, x) > 0)) {
                min = x;
                indexOfMin = i;
            }
        }
        void updateMax(int i, Object x) {
            if (i >= 0 &&
                (indexOfMax < 0 || comparator.compare(max, x) < 0)) {
                max = x;
                indexOfMax = i;
            }
        }

        void atLeaf(int l, int h) {
            if (pap.hasFilter())
                filteredAtLeaf(l, h);
            else {
                size = h - l;
                for (int i = l; i < h; ++i) {
                    Object x = pap.oget(i);
                    updateMin(i, x);
                    updateMax(i, x);
                }
            }
        }

        void filteredAtLeaf(int l, int h) {
            for (int i = l; i < h; ++i) {
                if (pap.isSelected(i)) {
                    Object x = pap.oget(i);
                    ++size;
                    updateMin(i, x);
                    updateMax(i, x);
                }
            }
        }

        public String toString() {
            return
                "size: " + size +
                " min: " + min + " (index " + indexOfMin +
                ") max: " + max + " (index " + indexOfMax + ")";
        }

    }

    static final class FJDStats extends FJBase
        implements ParallelDoubleArray.SummaryStatistics {
        final DoubleComparator comparator;
        public int size() { return size; }
        public double min() { return min; }
        public double max() { return max; }
        public double sum() { return sum; }
        public double average() { return sum / size; }
        public int indexOfMin() { return indexOfMin; }
        public int indexOfMax() { return indexOfMax; }
        int size;
        double min;
        double max;
        double sum;
        int indexOfMin;
        int indexOfMax;
        FJDStats(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                 DoubleComparator comparator) {
            super(pap, lo, hi, next);
            this.comparator = comparator;
            this.indexOfMin = -1;
            this.indexOfMax = -1;
            this.min = Double.MAX_VALUE;
            this.max = -Double.MAX_VALUE;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDStats(pap, l, h, r, comparator);
        }
        void onReduce(FJBase right) {
            FJDStats r = (FJDStats)right;
            size += r.size;
            sum += r.sum;
            updateMin(r.indexOfMin, r.min);
            updateMax(r.indexOfMax, r.max);
        }
        void updateMin(int i, double x) {
            if (i >= 0 &&
                (indexOfMin < 0 || comparator.compare(min, x) > 0)) {
                min = x;
                indexOfMin = i;
            }
        }
        void updateMax(int i, double x) {
            if (i >= 0 &&
                (indexOfMax < 0 || comparator.compare(max, x) < 0)) {
                max = x;
                indexOfMax = i;
            }
        }
        void atLeaf(int l, int h) {
            if (pap.hasFilter())
                filteredAtLeaf(l, h);
            else {
                size = h - l;
                for (int i = l; i < h; ++i) {
                    double x = pap.dget(i);
                    sum += x;
                    updateMin(i, x);
                    updateMax(i, x);
                }
            }
        }

        void filteredAtLeaf(int l, int h) {
            for (int i = l; i < h; ++i) {
                if (pap.isSelected(i)) {
                    double x = pap.dget(i);
                    ++size;
                    sum += x;
                    updateMin(i, x);
                    updateMax(i, x);
                }
            }
        }

        public String toString() {
            return
                "size: " + size +
                " min: " + min + " (index " + indexOfMin +
                ") max: " + max + " (index " + indexOfMax +
                ") sum: " + sum;
        }
    }

    static final class FJLStats extends FJBase
        implements ParallelLongArray.SummaryStatistics {
        final LongComparator comparator;
        public int size() { return size; }
        public long min() { return min; }
        public long max() { return max; }
        public long sum() { return sum; }
        public double average() { return (double)sum / size; }
        public int indexOfMin() { return indexOfMin; }
        public int indexOfMax() { return indexOfMax; }
        int size;
        long min;
        long max;
        long sum;
        int indexOfMin;
        int indexOfMax;
        FJLStats(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                 LongComparator comparator) {
            super(pap, lo, hi, next);
            this.comparator = comparator;
            this.indexOfMin = -1;
            this.indexOfMax = -1;
            this.min = Long.MAX_VALUE;
            this.max = Long.MIN_VALUE;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLStats(pap, l, h, r, comparator);
        }
        void onReduce(FJBase right) {
            FJLStats r = (FJLStats)right;
            size += r.size;
            sum += r.sum;
            updateMin(r.indexOfMin, r.min);
            updateMax(r.indexOfMax, r.max);
        }
        void updateMin(int i, long x) {
            if (i >= 0 &&
                (indexOfMin < 0 || comparator.compare(min, x) > 0)) {
                min = x;
                indexOfMin = i;
            }
        }
        void updateMax(int i, long x) {
            if (i >= 0 &&
                (indexOfMax < 0 || comparator.compare(max, x) < 0)) {
                max = x;
                indexOfMax = i;
            }
        }

        void atLeaf(int l, int h) {
            if (pap.hasFilter())
                filteredAtLeaf(l, h);
            else {
                size = h - l;
                for (int i = l; i < h; ++i) {
                    long x = pap.lget(i);
                    sum += x;
                    updateMin(i, x);
                    updateMax(i, x);
                }
            }
        }

        void filteredAtLeaf(int l, int h) {
            for (int i = l; i < h; ++i) {
                if (pap.isSelected(i)) {
                    long x = pap.lget(i);
                    ++size;
                    sum += x;
                    updateMin(i, x);
                    updateMax(i, x);
                }
            }
        }

        public String toString() {
            return
                "size: " + size +
                " min: " + min + " (index " + indexOfMin +
                ") max: " + max + " (index " + indexOfMax +
                ") sum: " + sum;
        }
    }

    // count

    static final class FJCountSelected extends FJBase {
        int count;
        FJCountSelected(AbstractParallelAnyArray pap, int lo, int hi,
                        FJBase next) {
            super(pap, lo, hi, next);
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJCountSelected(pap, l, h, r);
        }
        void onReduce(FJBase right) {
            count += ((FJCountSelected)right).count;
        }
        void atLeaf(int l, int h) {
            int n = 0;
            for (int i = l; i < h; ++i) {
                if (pap.isSelected(i))
                    ++n;
            }
            count = n;
        }
    }

    /**
     * Base for cancellable search tasks. Same idea as FJBase
     * but cancels tasks when result nonnegative.
     */
    abstract static class FJSearchBase extends RecursiveAction {
        final AbstractParallelAnyArray pap;
        final int lo;
        final int hi;
        final FJSearchBase next;
        final AtomicInteger result;

        FJSearchBase(AbstractParallelAnyArray pap, int lo, int hi,
                     FJSearchBase next,
                     AtomicInteger result) {
            this.pap = pap;
            this.lo = lo;
            this.hi = hi;
            this.next = next;
            this.result = result;
        }

        public void compute() {
            if (result.get() >= 0)
                return;
            FJSearchBase r = null;
            int l = lo;
            int h = hi;
            int g = pap.getThreshold();
            while (h - l > g) {
                int rh = h;
                h = (l + h) >>> 1;
                (r = newSubtask(h, rh, r)).fork();
            }
            atLeaf(l, h);
            boolean stopping = false;
            while (r != null) {
                stopping |= result.get() >= 0;
                if (r.tryUnfork()) {
                    if (!stopping)
                        r.atLeaf(r.lo, r.hi);
                }
                else if (stopping)
                    r.cancel(false);
                else
                    r.join();
                r = r.next;
            }
        }
        abstract FJSearchBase newSubtask(int l, int h, FJSearchBase r);
        abstract void atLeaf(int l, int h);
    }

    // select any

    static final class FJSelectAny extends FJSearchBase {
        FJSelectAny(AbstractParallelAnyArray pap, int lo, int hi,
                    FJSearchBase next, AtomicInteger result) {
            super(pap, lo, hi, next, result);
        }
        FJSearchBase newSubtask(int l, int h, FJSearchBase r) {
            return new FJSelectAny(pap, l, h, r, result);
        }
        void atLeaf(int l, int h) {
            for (int i = l; i < h; ++i) {
                if (pap.isSelected(i)) {
                    result.compareAndSet(-1, i);
                    break;
                }
                else if (result.get() >= 0)
                    break;
            }
        }
    }

    // index of

    static final class FJOIndexOf extends FJSearchBase {
        final Object target;
        FJOIndexOf(AbstractParallelAnyArray pap, int lo, int hi,
                   FJSearchBase next, AtomicInteger result, Object target) {
            super(pap, lo, hi, next, result);
            this.target = target;
        }
        FJSearchBase newSubtask(int l, int h, FJSearchBase r) {
            return new FJOIndexOf(pap, l, h, r, result, target);
        }
        void atLeaf(int l, int h) {
            final Object[] array = pap.ogetArray();
            if (array == null) return;
            for (int i = l; i < h; ++i) {
                if (target.equals(array[i])) {
                    result.compareAndSet(-1, i);
                    break;
                }
                else if (result.get() >= 0)
                    break;
            }
        }
    }

    static final class FJDIndexOf extends FJSearchBase {
        final double target;
        FJDIndexOf(AbstractParallelAnyArray pap, int lo, int hi,
                   FJSearchBase next, AtomicInteger result, double target) {
            super(pap, lo, hi, next, result);
            this.target = target;
        }
        FJSearchBase newSubtask(int l, int h, FJSearchBase r) {
            return new FJDIndexOf(pap, l, h, r, result, target);
        }
        void atLeaf(int l, int h) {
            final double[] array = pap.dgetArray();
            if (array == null) return;
            for (int i = l; i < h; ++i) {
                if (target == (array[i])) {
                    result.compareAndSet(-1, i);
                    break;
                }
                else if (result.get() >= 0)
                    break;
            }
        }
    }

    static final class FJLIndexOf extends FJSearchBase {
        final long target;
        FJLIndexOf(AbstractParallelAnyArray pap, int lo, int hi,
                   FJSearchBase next, AtomicInteger result, long target) {
            super(pap, lo, hi, next, result);
            this.target = target;
        }
        FJSearchBase newSubtask(int l, int h, FJSearchBase r) {
            return new FJLIndexOf(pap, l, h, r, result, target);
        }
        void atLeaf(int l, int h) {
            final long[] array = pap.lgetArray();
            if (array == null) return;
            for (int i = l; i < h; ++i) {
                if (target == (array[i])) {
                    result.compareAndSet(-1, i);
                    break;
                }
                else if (result.get() >= 0)
                    break;
            }
        }
    }

    // select all

    /**
     * SelectAll proceeds in two passes. In the first phase, indices
     * of matching elements are recorded in indices array.  In second
     * pass, once the size of results is known and result array is
     * constructed in driver, the matching elements are placed into
     * corresponding result positions.
     */
    static final class FJSelectAll extends RecursiveAction {
        final FJSelectAllDriver driver;
        FJSelectAll left, right;
        final int lo;
        final int hi;
        int count;  // number of matching elements
        int offset;
        boolean isInternal; // true if this is a non-leaf node
        final int threshold;

        FJSelectAll(FJSelectAllDriver driver, int lo, int hi) {
            this.driver = driver;
            this.lo = lo;
            this.hi = hi;
            this.threshold = driver.pap.getThreshold();
        }

        public void compute() {
            int l = lo;
            int h = hi;
            FJSelectAllDriver d = driver;
            if (d.phase == 0) {
                AbstractParallelAnyArray p = d.pap;
                if (isInternal = (h - l > threshold))
                    internalPhase0();
                else
                    count = p.leafIndexSelected(l, h, true, d.indices);
            }
            else if (count != 0) {
                if (isInternal)
                    internalPhase1();
                else
                    d.leafPhase1(l, l+count, offset);
            }
        }

        void internalPhase0() {
            int mid = (lo + hi) >>> 1;
            FJSelectAll l = new FJSelectAll(driver, lo, mid);
            FJSelectAll r = new FJSelectAll(driver, mid, hi);
            r.fork();
            l.compute();
            if (r.tryUnfork()) r.compute(); else r.join();
            int ln = l.count;
            if (ln != 0)
                left = l;
            int rn = r.count;
            if (rn != 0)
                right = r;
            count = ln + rn;
        }

        void internalPhase1() {
            int k = offset;
            if (left != null) {
                int ln = left.count;
                left.offset = k;
                left.reinitialize();
                if (right != null) {
                    right.offset = k + ln;
                    right.reinitialize();
                    right.fork();
                    left.compute();
                    if (right.tryUnfork()) right.compute(); else right.join();
                }
                else
                    left.compute();
            }
            else if (right != null) {
                right.offset = k;
                right.compute();
            }
        }
    }

    abstract static class FJSelectAllDriver extends RecursiveAction {
        final int[] indices;
        final AbstractParallelAnyArray pap;
        final int initialOffset;
        int phase;
        int resultSize;
        FJSelectAllDriver(AbstractParallelAnyArray pap, int initialOffset) {
            this.pap = pap;
            this.initialOffset = initialOffset;
            int n = pap.fence - pap.origin;
            indices = new int[n];
        }
        public final void compute() {
            FJSelectAll r = new FJSelectAll(this, pap.origin, pap.fence);
            r.offset = initialOffset;
            r.compute();
            createResults(resultSize = r.count);
            phase = 1;
            r.compute();
        }
        abstract void createResults(int size);
        abstract void leafPhase1(int loIdx, int hiIdx, int offset);
    }

    static final class FJOSelectAllDriver extends FJSelectAllDriver {
        final Class elementType;
        Object[] results;
        FJOSelectAllDriver(AbstractParallelAnyArray pap, Class elementType) {
            super(pap, 0);
            this.elementType = elementType;
        }
        void createResults(int size) {
            results = (Object[])Array.newInstance(elementType, size);
        }
        void leafPhase1(int loIdx, int hiIdx, int offset) {
            pap.leafTransferByIndex(indices, loIdx, hiIdx, results, offset);
        }
    }

    static final class FJDSelectAllDriver extends FJSelectAllDriver {
        double[] results;
        FJDSelectAllDriver(AbstractParallelAnyArray pap) {
            super(pap, 0);
        }
        void createResults(int size) {
            results = new double[size];
        }
        void leafPhase1(int loIdx, int hiIdx, int offset) {
            pap.leafTransferByIndex(indices, loIdx, hiIdx, results, offset);
        }
    }

    static final class FJLSelectAllDriver extends FJSelectAllDriver {
        long[] results;
        FJLSelectAllDriver(AbstractParallelAnyArray pap) {
            super(pap, 0);
        }
        void createResults(int size) {
            results = new long[size];
        }
        void leafPhase1(int loIdx, int hiIdx, int offset) {
            pap.leafTransferByIndex(indices, loIdx, hiIdx, results, offset);
        }
    }

    static final class FJOAppendAllDriver extends FJSelectAllDriver {
        Object[] results;
        FJOAppendAllDriver(AbstractParallelAnyArray pap, int initialOffset,
                           Object[] results) {
            super(pap, 0);
            this.results = results;
        }
        void createResults(int size) {
            int newSize = initialOffset + size;
            int oldLength = results.length;
            if (newSize > oldLength) {
                Class elementType = results.getClass().getComponentType();
                Object[] r = (Object[])Array.newInstance(elementType, newSize);
                System.arraycopy(results, 0, r, 0, oldLength);
                results = r;
            }
        }
        void leafPhase1(int loIdx, int hiIdx, int offset) {
            pap.leafTransferByIndex(indices, loIdx, hiIdx, results, offset);
        }
    }

    static final class FJDAppendAllDriver extends FJSelectAllDriver {
        double[] results;
        FJDAppendAllDriver(AbstractParallelAnyArray pap, int initialOffset,
                           double[] results) {
            super(pap, initialOffset);
            this.results = results;
        }
        void createResults(int size) {
            int newSize = initialOffset + size;
            int oldLength = results.length;
            if (newSize > oldLength) {
                double[] r = new double[newSize];
                System.arraycopy(results, 0, r, 0, oldLength);
                results = r;
            }
        }
        void leafPhase1(int loIdx, int hiIdx, int offset) {
            pap.leafTransferByIndex(indices, loIdx, hiIdx, results, offset);
        }
    }

    static final class FJLAppendAllDriver extends FJSelectAllDriver {
        long[] results;
        FJLAppendAllDriver(AbstractParallelAnyArray pap, int initialOffset,
                           long[] results) {
            super(pap, initialOffset);
            this.results = results;
        }
        void createResults(int size) {
            int newSize = initialOffset + size;
            int oldLength = results.length;
            if (newSize > oldLength) {
                long[] r = new long[newSize];
                System.arraycopy(results, 0, r, 0, oldLength);
                results = r;
            }
        }
        void leafPhase1(int loIdx, int hiIdx, int offset) {
            pap.leafTransferByIndex(indices, loIdx, hiIdx, results, offset);
        }
    }


    /**
     * Root node for FJRemoveAll. Spawns subtasks and shifts elements
     * as indices become available, bypassing index array creation
     * when offsets are known. This differs from SelectAll mainly in
     * that data movement is all done by the driver rather than in a
     * second parallel pass.
     */
    static final class FJRemoveAllDriver extends RecursiveAction {
        final AbstractParallelAnyArray pap;
        final int lo;
        final int hi;
        final int[] indices;
        int offset;
        final int threshold;
        FJRemoveAllDriver(AbstractParallelAnyArray pap, int lo, int hi) {
            this.pap = pap;
            this.lo = lo;
            this.hi = hi;
            this.indices = new int[hi - lo];
            this.threshold = pap.getThreshold();
        }

        public void compute() {
            FJRemoveAll r = null;
            int l = lo;
            int h = hi;
            int g = threshold;
            while (h - l > g) {
                int rh = h;
                h = (l + h) >>> 1;
                (r = new FJRemoveAll(pap, h, rh, r, indices)).fork();
            }
            int k = pap.leafMoveSelected(l, h, l, false);
            while (r != null) {
                if (r.tryUnfork())
                    k = pap.leafMoveSelected(r.lo, r.hi, k, false);
                else {
                    r.join();
                    int n = r.count;
                    if (n != 0)
                        pap.leafMoveByIndex(indices, r.lo, r.lo+n, k);
                    k += n;
                    FJRemoveAll rr = r.right;
                    if (rr != null)
                        k = inorderMove(rr, k);
                }
                r = r.next;
            }
            offset = k;
        }

        /**
         * Inorder traversal to move indexed elements across reachable
         * nodes.  This guarantees that element shifts don't overwrite
         * those still being used by active subtasks.
         */
        static int inorderMove(FJRemoveAll t, int index) {
            while (t != null) {
                int n = t.count;
                if (n != 0)
                    t.pap.leafMoveByIndex(t.indices, t.lo, t.lo+n, index);
                index += n;
                FJRemoveAll p = t.next;
                if (p != null)
                    index = inorderMove(p, index);
                t = t.right;
            }
            return index;
        }
    }

    /**
     * Basic FJ task for non-root FJRemoveAll nodes. Differs from
     * FJBase because it requires maintaining explicit right pointers so
     * FJRemoveAllDriver can traverse them
     */
    static final class FJRemoveAll extends RecursiveAction {
        final AbstractParallelAnyArray pap;
        final int lo;
        final int hi;
        final FJRemoveAll next;
        final int[] indices;
        int count;
        FJRemoveAll right;
        final int threshold;
        FJRemoveAll(AbstractParallelAnyArray pap, int lo, int hi,
                    FJRemoveAll next, int[] indices) {
            this.pap = pap;
            this.lo = lo;
            this.hi = hi;
            this.next = next;
            this.indices = indices;
            this.threshold = pap.getThreshold();
        }

        public void compute() {
            FJRemoveAll r = null;
            int l = lo;
            int h = hi;
            int g = threshold;
            while (h - l > g) {
                int rh = h;
                h = (l + h) >>> 1;
                (r = new FJRemoveAll(pap, h, rh, r, indices)).fork();
            }
            right = r;
            count = pap.leafIndexSelected(l, h, false, indices);
            while (r != null) {
                if (r.tryUnfork())
                    r.count = pap.leafIndexSelected
                        (r.lo, r.hi, false, indices);
                else
                    r.join();
                r = r.next;
            }
        }
    }

    // unique elements

    static final class FJOUniquifier extends FJBase {
        final UniquifierTable table;
        int count;
        FJOUniquifier(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                     UniquifierTable table) {
            super(pap, lo, hi, next);
            this.table = table;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJOUniquifier(pap, l, h, r, table);
        }
        void atLeaf(int l, int h) {
            count = table.addObjects(l, h);
        }
        void onReduce(FJBase right) {
            count += ((FJOUniquifier)right).count;
        }
    }

    static final class FJDUniquifier extends FJBase {
        final UniquifierTable table;
        int count;
        FJDUniquifier(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                     UniquifierTable table) {
            super(pap, lo, hi, next);
            this.table = table;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJDUniquifier(pap, l, h, r, table);
        }
        void atLeaf(int l, int h) {
            count = table.addDoubles(l, h);
        }
        void onReduce(FJBase right) {
            count += ((FJDUniquifier)right).count;
        }
    }

    static final class FJLUniquifier extends FJBase {
        final UniquifierTable table;
        int count;
        FJLUniquifier(AbstractParallelAnyArray pap, int lo, int hi, FJBase next,
                      UniquifierTable table) {
            super(pap, lo, hi, next);
            this.table = table;
        }
        FJBase newSubtask(int l, int h, FJBase r) {
            return new FJLUniquifier(pap, l, h, r, table);
        }
        void atLeaf(int l, int h) {
            count = table.addLongs(l, h);
        }
        void onReduce(FJBase right) {
            count += ((FJLUniquifier)right).count;
        }
    }

    /**
     * Base class of fixed-size hash tables for
     * uniquification. Opportunistically subclasses
     * AtomicLongArray. The high word of each slot is the cached
     * massaged hash of an element, and the low word contains its
     * index, plus one, to ensure that a zero tab entry means
     * empty. The mechanics for this are just folded into the
     * main addElements method.
     * Each leaf step places source array elements into table,
     * Even though this table undergoes a lot of contention when
     * elements are concurrently inserted by parallel threads, it is
     * generally faster to do this than to have separate tables and
     * then merge them.
     */
    static final class UniquifierTable extends AtomicLongArray {
        final AbstractParallelAnyArray pap;
        final boolean byIdentity;
        UniquifierTable(int size, AbstractParallelAnyArray pap,
                        boolean byIdentity) {
            super(tableSizeFor(size));
            this.pap = pap;
            this.byIdentity = byIdentity;
        }

        /** Returns a good size for table */
        static int tableSizeFor(int n) {
            int padded = n + (n >>> 1) + 1;
            if (padded < n) // int overflow
                throw new OutOfMemoryError();
            int s = 8;
            while (s < padded) s <<= 1;
            return s;
        }

        // Same hashcode conditioning as HashMap
        static int hash(int h) {
            h ^= (h >>> 20) ^ (h >>> 12);
            return h ^ (h >>> 7) ^ (h >>> 4);
        }

        int addObjects(int lo, int hi) {
            boolean filtered = pap.hasFilter();
            Object[] src = pap.ogetArray();
            final int mask = length() - 1;
            int count = 0;
            for (int k = lo; k < hi; ++k) {
                Object x;
                if ((filtered && !pap.isSelected(k)) ||
                    (x = src[k]) == null)
                    continue;
                int hc = byIdentity ? System.identityHashCode(x) : x.hashCode();
                int hash = hash(hc);
                long entry = (((long)hash) << 32) + (k + 1);
                int idx = hash & mask;
                for (;;) {
                    long d = get(idx);
                    if (d != 0) {
                        if ((int)(d >>> 32) == hash) {
                            Object y = src[(int)((d-1) & 0x7fffffffL)];
                            if (x == y || (!byIdentity && x.equals(y)))
                                break;
                        }
                        idx = (idx + 1) & mask;
                    }
                    else if (compareAndSet(idx, 0, entry)) {
                        ++count;
                        break;
                    }
                }
            }
            return count;
        }

        int addDoubles(int lo, int hi) {
            boolean filtered = pap.hasFilter();
            double[] src = pap.dgetArray();
            final int mask = length() - 1;
            int count = 0;
            for (int k = lo; k < hi; ++k) {
                if (filtered && !pap.isSelected(k))
                    continue;
                double x = src[k];
                long bits = Double.doubleToLongBits(x);
                int hash = hash((int)(bits ^ (bits >>> 32)));
                long entry = (((long)hash) << 32) + (k + 1);
                int idx = hash & mask;
                for (;;) {
                    long d = get(idx);
                    if (d != 0) {
                        if ((int)(d >>> 32) == hash &&
                            x == src[(int)((d - 1) & 0x7fffffffL)])
                            break;
                        idx = (idx + 1) & mask;
                    }
                    else if (compareAndSet(idx, 0, entry)) {
                        ++count;
                        break;
                    }
                }
            }
            return count;
        }

        int addLongs(int lo, int hi) {
            boolean filtered = pap.hasFilter();
            long[] src = pap.lgetArray();
            final int mask = length() - 1;
            int count = 0;
            for (int k = lo; k < hi; ++k) {
                if (filtered && !pap.isSelected(k))
                    continue;
                long x = src[k];
                int hash = hash((int)(x ^ (x >>> 32)));
                long entry = (((long)hash) << 32) + (k + 1);
                int idx = hash & mask;
                for (;;) {
                    long d = get(idx);
                    if (d != 0) {
                        if ((int)(d >>> 32) == hash &&
                            x == src[(int)((d - 1) & 0x7fffffffL)])
                            break;
                        idx = (idx + 1) & mask;
                    }
                    else if (compareAndSet(idx, 0, entry)) {
                        ++count;
                        break;
                    }
                }
            }
            return count;
        }

        /**
         * Returns new array holding all elements.
         */
        Object[] uniqueObjects(int size) {
            Object[] src = pap.ogetArray();
            Class sclass = src.getClass().getComponentType();
            Object[] res = (Object[])Array.newInstance(sclass, size);
            int k = 0;
            int n = length();
            for (int i = 0; i < n && k < size; ++i) {
                long d = get(i);
                if (d != 0)
                    res[k++] = src[((int)((d - 1) & 0x7fffffffL))];
            }
            return res;
        }

        double[] uniqueDoubles(int size) {
            double[] src = pap.dgetArray();
            double[] res = new double[size];
            int k = 0;
            int n = length();
            for (int i = 0; i < n && k < size; ++i) {
                long d = get(i);
                if (d != 0)
                    res[k++] = src[((int)((d - 1) & 0x7fffffffL))];
            }
            return res;
        }

        long[] uniqueLongs(int size) {
            long[] src = pap.lgetArray();
            long[] res = new long[size];
            int k = 0;
            int n = length();
            for (int i = 0; i < n && k < size; ++i) {
                long d = get(i);
                if (d != 0)
                    res[k++] = src[((int)((d - 1) & 0x7fffffffL))];
            }
            return res;
        }
    }

    /**
     * Sorter classes based mainly on CilkSort
     * <A href="http://supertech.lcs.mit.edu/cilk/"> Cilk</A>:
     * Basic algorithm:
     * if array size is small, just use a sequential quicksort
     *         Otherwise:
     *         1. Break array in half.
     *         2. For each half,
     *             a. break the half in half (i.e., quarters),
     *             b. sort the quarters
     *             c. merge them together
     *         3. merge together the two halves.
     *
     * One reason for splitting in quarters is that this guarantees
     * that the final sort is in the main array, not the workspace
     * array.  (workspace and main swap roles on each subsort step.)
     * Leaf-level sorts use a Sequential quicksort, that in turn uses
     * insertion sort if under threshold.  Otherwise it uses median of
     * three to pick pivot, and loops rather than recurses along left
     * path.
     *
     * It is sad but true that sort and merge performance are
     * sensitive enough to inner comparison overhead to warrant
     * creating 6 versions (not just 3) -- one each for natural
     * comparisons vs supplied comparators.
     */
    static final class FJOSorter extends RecursiveAction {
        final Comparator cmp;
        final Object[] a;     // array to be sorted.
        final Object[] w;     // workspace for merge
        final int origin;     // origin of the part of array we deal with
        final int n;          // Number of elements in (sub)arrays.
        final int gran;       // split control
        FJOSorter(Comparator cmp,
                  Object[] a, Object[] w, int origin, int n, int gran) {
            this.cmp = cmp;
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.gran = gran;
        }

        public void compute() {
            int l = origin;
            int g = gran;
            if (n > g) {
                int h = n >>> 1; // half
                int q = n >>> 2; // lower quarter index
                int u = h + q;   // upper quarter
                FJSubSorter ls = new FJSubSorter
                    (new FJOSorter(cmp, a, w, l,   q,   g),
                     new FJOSorter(cmp, a, w, l+q, h-q, g),
                     new FJOMerger(cmp, a, w, l,   q,
                                   l+q, h-q, l, g, null));
                FJSubSorter rs = new FJSubSorter
                    (new FJOSorter(cmp, a, w, l+h, q,   g),
                     new FJOSorter(cmp, a, w, l+u, n-u, g),
                     new FJOMerger(cmp, a, w, l+h, q,
                                   l+u, n-u, l+h, g, null));
                rs.fork();
                ls.compute();
                if (rs.tryUnfork()) rs.compute(); else rs.join();
                new FJOMerger(cmp, w, a, l, h,
                              l+h, n-h, l, g, null).compute();
            }
            else
                Arrays.sort(a, l, l+n, cmp);
        }
    }

    static final class FJOCSorter extends RecursiveAction {
        final Comparable[] a; final Comparable[] w;
        final int origin; final int n; final int gran;
        FJOCSorter(Comparable[] a, Comparable[] w,
                   int origin, int n, int gran) {
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.gran = gran;
        }
        public void compute() {
            int l = origin;
            int g = gran;
            if (n > g) {
                int h = n >>> 1;
                int q = n >>> 2;
                int u = h + q;
                FJSubSorter ls = new FJSubSorter
                    (new FJOCSorter(a, w, l,   q,   g),
                     new FJOCSorter(a, w, l+q, h-q, g),
                     new FJOCMerger(a, w, l,   q,
                                   l+q, h-q, l, g, null));
                FJSubSorter rs = new FJSubSorter
                    (new FJOCSorter(a, w, l+h, q,   g),
                     new FJOCSorter(a, w, l+u, n-u, g),
                     new FJOCMerger(a, w, l+h, q,
                                   l+u, n-u, l+h, g, null));
                rs.fork();
                ls.compute();
                if (rs.tryUnfork()) rs.compute(); else rs.join();
                new FJOCMerger(w, a, l, h,
                               l+h, n-h, l, g, null).compute();
            }
            else
                Arrays.sort(a, l, l+n);
        }
    }

    static final class FJDSorter extends RecursiveAction {
        final DoubleComparator cmp; final double[] a; final double[] w;
        final int origin; final int n; final int gran;
        FJDSorter(DoubleComparator cmp,
                  double[] a, double[] w, int origin, int n, int gran) {
            this.cmp = cmp;
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.gran = gran;
        }
        public void compute() {
            int l = origin;
            int g = gran;
            if (n > g) {
                int h = n >>> 1;
                int q = n >>> 2;
                int u = h + q;
                FJSubSorter ls = new FJSubSorter
                    (new FJDSorter(cmp, a, w, l,   q,   g),
                     new FJDSorter(cmp, a, w, l+q, h-q, g),
                     new FJDMerger(cmp, a, w, l,   q,
                                   l+q, h-q, l, g, null));
                FJSubSorter rs = new FJSubSorter
                    (new FJDSorter(cmp, a, w, l+h, q,   g),
                     new FJDSorter(cmp, a, w, l+u, n-u, g),
                     new FJDMerger(cmp, a, w, l+h, q,
                                   l+u, n-u, l+h, g, null));
                rs.fork();
                ls.compute();
                if (rs.tryUnfork()) rs.compute(); else rs.join();
                new FJDMerger(cmp, w, a, l, h,
                              l+h, n-h, l, g, null).compute();
            }
            else
                dquickSort(a, cmp, l, l+n-1);
        }
    }

    static final class FJDCSorter extends RecursiveAction {
        final double[] a; final double[] w;
        final int origin; final int n; final int gran;
        FJDCSorter(double[] a, double[] w, int origin,
                   int n, int gran) {
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.gran = gran;
        }
        public void compute() {
            int l = origin;
            int g = gran;
            if (n > g) {
                int h = n >>> 1;
                int q = n >>> 2;
                int u = h + q;
                FJSubSorter ls = new FJSubSorter
                    (new FJDCSorter(a, w, l,   q,   g),
                     new FJDCSorter(a, w, l+q, h-q, g),
                     new FJDCMerger(a, w, l,   q,
                                    l+q, h-q, l, g, null));
                FJSubSorter rs = new FJSubSorter
                    (new FJDCSorter(a, w, l+h, q,   g),
                     new FJDCSorter(a, w, l+u, n-u, g),
                     new FJDCMerger(a, w, l+h, q,
                                    l+u, n-u, l+h, g, null));
                rs.fork();
                ls.compute();
                if (rs.tryUnfork()) rs.compute(); else rs.join();
                new FJDCMerger(w, a, l, h,
                               l+h, n-h, l, g, null).compute();
            }
            else
                Arrays.sort(a, l, l+n);
        }
    }

    static final class FJLSorter extends RecursiveAction {
        final LongComparator cmp; final long[] a; final long[] w;
        final int origin; final int n; final int gran;
        FJLSorter(LongComparator cmp,
                  long[] a, long[] w, int origin, int n, int gran) {
            this.cmp = cmp;
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.gran = gran;
        }

        public void compute() {
            int l = origin;
            int g = gran;
            if (n > g) {
                int h = n >>> 1;
                int q = n >>> 2;
                int u = h + q;
                FJSubSorter ls = new FJSubSorter
                    (new FJLSorter(cmp, a, w, l,   q,   g),
                     new FJLSorter(cmp, a, w, l+q, h-q, g),
                     new FJLMerger(cmp, a, w, l,   q,
                                   l+q, h-q, l, g, null));
                FJSubSorter rs = new FJSubSorter
                    (new FJLSorter(cmp, a, w, l+h, q,   g),
                     new FJLSorter(cmp, a, w, l+u, n-u, g),
                     new FJLMerger(cmp, a, w, l+h, q,
                                   l+u, n-u, l+h, g, null));
                rs.fork();
                ls.compute();
                if (rs.tryUnfork()) rs.compute(); else rs.join();
                new FJLMerger(cmp, w, a, l, h,
                              l+h, n-h, l, g, null).compute();
            }
            else
                lquickSort(a, cmp, l, l+n-1);
        }
    }

    static final class FJLCSorter extends RecursiveAction {
        final long[] a; final long[] w;
        final int origin; final int n; final int gran;
        FJLCSorter(long[] a, long[] w, int origin,
                   int n, int gran) {
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.gran = gran;
        }
        public void compute() {
            int l = origin;
            int g = gran;
            if (n > g) {
                int h = n >>> 1;
                int q = n >>> 2;
                int u = h + q;
                FJSubSorter ls = new FJSubSorter
                    (new FJLCSorter(a, w, l,   q,   g),
                     new FJLCSorter(a, w, l+q, h-q, g),
                     new FJLCMerger(a, w, l,   q,
                                    l+q, h-q, l, g, null));

                FJSubSorter rs = new FJSubSorter
                    (new FJLCSorter(a, w, l+h, q,   g),
                     new FJLCSorter(a, w, l+u, n-u, g),
                     new FJLCMerger(a, w, l+h, q,
                                    l+u, n-u, l+h, g, null));
                rs.fork();
                ls.compute();
                if (rs.tryUnfork()) rs.compute(); else rs.join();
                new FJLCMerger(w, a, l, h,
                               l+h, n-h, l, g, null).compute();
            }
            else
                Arrays.sort(a, l, l+n);
        }
    }

    /** Utility class to sort half a partitioned array */
    static final class FJSubSorter extends RecursiveAction {
        final RecursiveAction left;
        final RecursiveAction right;
        final RecursiveAction merger;
        FJSubSorter(RecursiveAction left, RecursiveAction right,
                    RecursiveAction merger) {
            this.left = left; this.right = right; this.merger = merger;
        }
        public void compute() {
            right.fork();
            left.invoke();
            right.join();
            merger.invoke();
        }
    }

    /**
     * Performs merging for FJSorter. If big enough, splits Left
     * partition in half; finds the greatest point in Right partition
     * less than the beginning of the second half of Left via binary
     * search; and then, in parallel, merges left half of Left with
     * elements of Right up to split point, and merges right half of
     * Left with elements of R past split point. At leaf, it just
     * sequentially merges. This is all messy to code; sadly we need
     * six versions.
     */
    static final class FJOMerger extends RecursiveAction {
        final Comparator cmp;
        final Object[] a;      // partitioned  array.
        final Object[] w;      // Output array.
        final int lo;          // relative origin of left side of a
        final int ln;          // number of elements on left of a
        final int ro;          // relative origin of right side of a
        final int rn;          // number of elements on right of a
        final int wo;          // origin for output
        final int gran;
        final FJOMerger next;

        FJOMerger(Comparator cmp, Object[] a, Object[] w,
                  int lo, int ln, int ro, int rn, int wo,
                  int gran, FJOMerger next) {
            this.cmp = cmp;
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.gran = gran;
            this.next = next;
        }

        public void compute() {
            // spawn right subtasks
            FJOMerger rights = null;
            int nleft = ln;
            int nright = rn;
            while (nleft > gran) {
                int lh = nleft >>> 1;
                int splitIndex = lo + lh;
                Object split = a[splitIndex];
                // binary search r for split
                int rl = 0;
                int rh = nright;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (cmp.compare(split, a[ro + mid]) <= 0)
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                (rights = new FJOMerger
                 (cmp, a, w, splitIndex, nleft-lh, ro+rh,
                  nright-rh, wo+lh+rh, gran, rights)).fork();
                nleft = lh;
                nright = rh;
            }

            // sequentially merge
            int l = lo;
            int lFence = lo + nleft;
            int r = ro;
            int rFence = ro + nright;
            int k = wo;
            while (l < lFence && r < rFence) {
                Object al = a[l];
                Object ar = a[r];
                Object t;
                if (cmp.compare(al, ar) <= 0) {++l; t=al;} else {++r; t=ar;}
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];

            // join subtasks
            while (rights != null) {
                if (rights.tryUnfork())
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }
    }

    static final class FJOCMerger extends RecursiveAction {
        final Comparable[] a; final Comparable[] w;
        final int lo; final int ln; final int ro;  final int rn; final int wo;
        final int gran;
        final FJOCMerger next;
        FJOCMerger(Comparable[] a, Comparable[] w, int lo,
                   int ln, int ro, int rn, int wo,
                   int gran, FJOCMerger next) {
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln; this.ro = ro; this.rn = rn;
            this.wo = wo;
            this.gran = gran;
            this.next = next;
        }

        public void compute() {
            FJOCMerger rights = null;
            int nleft = ln;
            int nright = rn;
            while (nleft > gran) {
                int lh = nleft >>> 1;
                int splitIndex = lo + lh;
                Comparable split = a[splitIndex];
                int rl = 0;
                int rh = nright;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (split.compareTo(a[ro + mid]) <= 0)
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                (rights = new FJOCMerger
                 (a, w, splitIndex, nleft-lh, ro+rh,
                  nright-rh, wo+lh+rh, gran, rights)).fork();
                nleft = lh;
                nright = rh;
            }

            int l = lo;
            int lFence = lo + nleft;
            int r = ro;
            int rFence = ro + nright;
            int k = wo;
            while (l < lFence && r < rFence) {
                Comparable al = a[l];
                Comparable ar = a[r];
                Comparable t;
                if (al.compareTo(ar) <= 0) {++l; t=al;} else {++r; t=ar; }
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
            while (rights != null) {
                if (rights.tryUnfork())
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }
    }

    static final class FJDMerger extends RecursiveAction {
        final DoubleComparator cmp; final double[] a; final double[] w;
        final int lo; final int ln; final int ro; final int rn; final int wo;
        final int gran;
        final FJDMerger next;
        FJDMerger(DoubleComparator cmp, double[] a, double[] w,
                  int lo, int ln, int ro, int rn, int wo,
                  int gran, FJDMerger next) {
            this.cmp = cmp;
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.gran = gran;
            this.next = next;
        }
        public void compute() {
            FJDMerger rights = null;
            int nleft = ln;
            int nright = rn;
            while (nleft > gran) {
                int lh = nleft >>> 1;
                int splitIndex = lo + lh;
                double split = a[splitIndex];
                int rl = 0;
                int rh = nright;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (cmp.compare(split, a[ro + mid]) <= 0)
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                (rights = new FJDMerger
                 (cmp, a, w, splitIndex, nleft-lh, ro+rh,
                  nright-rh, wo+lh+rh, gran, rights)).fork();
                nleft = lh;
                nright = rh;
            }

            int l = lo;
            int lFence = lo + nleft;
            int r = ro;
            int rFence = ro + nright;
            int k = wo;
            while (l < lFence && r < rFence) {
                double al = a[l];
                double ar = a[r];
                double t;
                if (cmp.compare(al, ar) <= 0) {++l; t=al;} else {++r; t=ar; }
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
            while (rights != null) {
                if (rights.tryUnfork())
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }
    }

    static final class FJDCMerger extends RecursiveAction {
        final double[] a; final double[] w;
        final int lo; final int ln; final int ro; final int rn; final int wo;
        final int gran;
        final FJDCMerger next;
        FJDCMerger(double[] a, double[] w, int lo,
                   int ln, int ro, int rn, int wo,
                   int gran, FJDCMerger next) {
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.gran = gran;
            this.next = next;
        }
        public void compute() {
            FJDCMerger rights = null;
            int nleft = ln;
            int nright = rn;
            while (nleft > gran) {
                int lh = nleft >>> 1;
                int splitIndex = lo + lh;
                double split = a[splitIndex];
                int rl = 0;
                int rh = nright;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (split <= a[ro + mid])
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                (rights = new FJDCMerger
                 (a, w, splitIndex, nleft-lh, ro+rh,
                  nright-rh, wo+lh+rh, gran, rights)).fork();
                nleft = lh;
                nright = rh;
            }

            int l = lo;
            int lFence = lo + nleft;
            int r = ro;
            int rFence = ro + nright;
            int k = wo;
            while (l < lFence && r < rFence) {
                double al = a[l];
                double ar = a[r];
                double t;
                if (al <= ar) {++l; t=al;} else {++r; t=ar; }
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
            while (rights != null) {
                if (rights.tryUnfork())
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }
    }

    static final class FJLMerger extends RecursiveAction {
        final LongComparator cmp; final long[] a; final long[] w;
        final int lo; final int ln; final int ro; final int rn; final int wo;
        final int gran;
        final FJLMerger next;
        FJLMerger(LongComparator cmp, long[] a, long[] w,
                  int lo, int ln, int ro, int rn, int wo,
                  int gran, FJLMerger next) {
            this.cmp = cmp;
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.gran = gran;
            this.next = next;
        }
        public void compute() {
            FJLMerger rights = null;
            int nleft = ln;
            int nright = rn;
            while (nleft > gran) {
                int lh = nleft >>> 1;
                int splitIndex = lo + lh;
                long split = a[splitIndex];
                int rl = 0;
                int rh = nright;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (cmp.compare(split, a[ro + mid]) <= 0)
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                (rights = new FJLMerger
                 (cmp, a, w, splitIndex, nleft-lh, ro+rh,
                  nright-rh, wo+lh+rh, gran, rights)).fork();
                nleft = lh;
                nright = rh;
            }

            int l = lo;
            int lFence = lo + nleft;
            int r = ro;
            int rFence = ro + nright;
            int k = wo;
            while (l < lFence && r < rFence) {
                long al = a[l];
                long ar = a[r];
                long t;
                if (cmp.compare(al, ar) <= 0) {++l; t=al;} else {++r; t=ar;}
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
            while (rights != null) {
                if (rights.tryUnfork())
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }
    }

    static final class FJLCMerger extends RecursiveAction {
        final long[] a; final long[] w;
        final int lo; final int ln; final int ro; final int rn; final int wo;
        final int gran;
        final FJLCMerger next;
        FJLCMerger(long[] a, long[] w, int lo,
                   int ln, int ro, int rn, int wo,
                   int gran, FJLCMerger next) {
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.gran = gran;
            this.next = next;
        }
        public void compute() {
            FJLCMerger rights = null;
            int nleft = ln;
            int nright = rn;
            while (nleft > gran) {
                int lh = nleft >>> 1;
                int splitIndex = lo + lh;
                long split = a[splitIndex];
                int rl = 0;
                int rh = nright;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (split <= a[ro + mid])
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                (rights = new FJLCMerger
                 (a, w, splitIndex, nleft-lh, ro+rh,
                  nright-rh, wo+lh+rh, gran, rights)).fork();
                nleft = lh;
                nright = rh;
            }

            int l = lo;
            int lFence = lo + nleft;
            int r = ro;
            int rFence = ro + nright;
            int k = wo;
            while (l < lFence && r < rFence) {
                long al = a[l];
                long ar = a[r];
                long t;
                if (al <= ar) {++l; t=al;} else {++r; t = ar;}
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
            while (rights != null) {
                if (rights.tryUnfork())
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }
    }

    /** Cutoff for when to use insertion-sort instead of quicksort */
    static final int INSERTION_SORT_THRESHOLD = 8;

    // versions of quicksort with comparators


    static void dquickSort(double[] a, DoubleComparator cmp, int lo, int hi) {
        for (;;) {
            if (hi - lo <= INSERTION_SORT_THRESHOLD) {
                for (int i = lo + 1; i <= hi; i++) {
                    double t = a[i];
                    int j = i - 1;
                    while (j >= lo && cmp.compare(t, a[j]) < 0) {
                        a[j+1] = a[j];
                        --j;
                    }
                    a[j+1] = t;
                }
                return;
            }

            int mid = (lo + hi) >>> 1;
            if (cmp.compare(a[lo], a[mid]) > 0) {
                double t = a[lo]; a[lo] = a[mid]; a[mid] = t;
            }
            if (cmp.compare(a[mid], a[hi]) > 0) {
                double t = a[mid]; a[mid] = a[hi]; a[hi] = t;
                if (cmp.compare(a[lo], a[mid]) > 0) {
                    double u = a[lo]; a[lo] = a[mid]; a[mid] = u;
                }
            }

            double pivot = a[mid];
            int left = lo+1;
            int right = hi-1;
            boolean sameLefts = true;
            for (;;) {
                while (cmp.compare(pivot, a[right]) < 0)
                    --right;
                int c;
                while (left < right &&
                       (c = cmp.compare(pivot, a[left])) >= 0) {
                    if (c != 0)
                        sameLefts = false;
                    ++left;
                }
                if (left < right) {
                    double t = a[left]; a[left] = a[right]; a[right] = t;
                    --right;
                }
                else break;
            }

            if (sameLefts && right == hi - 1)
                return;
            if (left - lo <= hi - right) {
                dquickSort(a, cmp, lo, left);
                lo = left + 1;
            }
            else {
                dquickSort(a, cmp, right, hi);
                hi = left;
            }
        }
    }

    static void lquickSort(long[] a, LongComparator cmp, int lo, int hi) {
        for (;;) {
            if (hi - lo <= INSERTION_SORT_THRESHOLD) {
                for (int i = lo + 1; i <= hi; i++) {
                    long t = a[i];
                    int j = i - 1;
                    while (j >= lo && cmp.compare(t, a[j]) < 0) {
                        a[j+1] = a[j];
                        --j;
                    }
                    a[j+1] = t;
                }
                return;
            }

            int mid = (lo + hi) >>> 1;
            if (cmp.compare(a[lo], a[mid]) > 0) {
                long t = a[lo]; a[lo] = a[mid]; a[mid] = t;
            }
            if (cmp.compare(a[mid], a[hi]) > 0) {
                long t = a[mid]; a[mid] = a[hi]; a[hi] = t;
                if (cmp.compare(a[lo], a[mid]) > 0) {
                    long u = a[lo]; a[lo] = a[mid]; a[mid] = u;
                }
            }

            long pivot = a[mid];
            int left = lo+1;
            int right = hi-1;
            boolean sameLefts = true;
            for (;;) {
                while (cmp.compare(pivot, a[right]) < 0)
                    --right;
                int c;
                while (left < right &&
                       (c = cmp.compare(pivot, a[left])) >= 0) {
                    if (c != 0)
                        sameLefts = false;
                    ++left;
                }
                if (left < right) {
                    long t = a[left]; a[left] = a[right]; a[right] = t;
                    --right;
                }
                else break;
            }

            if (sameLefts && right == hi - 1)
                return;
            if (left - lo <= hi - right) {
                lquickSort(a, cmp, lo, left);
                lo = left + 1;
            }
            else {
                lquickSort(a, cmp, right, hi);
                hi = left;
            }
        }
    }

    /**
     * Cumulative scan
     *
     * A basic version of scan is straightforward.
     *  Keep dividing by two to threshold segment size, and then:
     *   Pass 1: Create tree of partial sums for each segment
     *   Pass 2: For each segment, cumulate with offset of left sibling
     * See G. Blelloch's http://www.cs.cmu.edu/~scandal/alg/scan.html
     *
     * This version improves performance within FJ framework mainly by
     * allowing second pass of ready left-hand sides to proceed even
     * if some right-hand side first passes are still executing.  It
     * also combines first and second pass for leftmost segment, and
     * for cumulate (not precumulate) also skips first pass for
     * rightmost segment (whose result is not needed for second pass).
     *
     * To manage this, it relies on "phase" phase/state control field
     * maintaining bits CUMULATE, SUMMED, and FINISHED. CUMULATE is
     * main phase bit. When false, segments compute only their sum.
     * When true, they cumulate array elements. CUMULATE is set at
     * root at beginning of second pass and then propagated down. But
     * it may also be set earlier for subtrees with lo==origin (the
     * left spine of tree). SUMMED is a one bit join count. For leafs,
     * set when summed. For internal nodes, becomes true when one
     * child is summed.  When second child finishes summing, it then
     * moves up tree to trigger cumulate phase. FINISHED is also a one
     * bit join count. For leafs, it is set when cumulated. For
     * internal nodes, it becomes true when one child is cumulated.
     * When second child finishes cumulating, it then moves up tree,
     * executing complete() at the root.
     *
     * This class maintains only the basic control logic.  Subclasses
     * maintain the "in" and "out" fields, and *Ops classes perform
     * computations.
     */
    abstract static class FJScan extends ForkJoinTask<Void> {
        static final short CUMULATE = (short)1;
        static final short SUMMED   = (short)2;
        static final short FINISHED = (short)4;

        final FJScan parent;
        final FJScanOp op;
        FJScan left, right;
        volatile int phase;  // phase/state
        final int lo;
        final int hi;

        static final AtomicIntegerFieldUpdater<FJScan> phaseUpdater =
            AtomicIntegerFieldUpdater.newUpdater(FJScan.class, "phase");

        FJScan(FJScan parent, FJScanOp op, int lo, int hi) {
            this.parent = parent;
            this.op = op;
            this.lo = lo;
            this.hi = hi;
        }

        public final Void getRawResult() { return null; }
        protected final void setRawResult(Void mustBeNull) { }

        /** Returns true if can CAS CUMULATE bit true */
        final boolean transitionToCumulate() {
            int c;
            while (((c = phase) & CUMULATE) == 0)
                if (phaseUpdater.compareAndSet(this, c, c | CUMULATE))
                    return true;
            return false;
        }

        public final boolean exec() {
            if (hi - lo > op.threshold) {
                if (left == null) { // first pass
                    int mid = (lo + hi) >>> 1;
                    left =  op.newSubtask(this, lo, mid);
                    right = op.newSubtask(this, mid, hi);
                }

                boolean cumulate = (phase & CUMULATE) != 0;
                if (cumulate)
                    op.pushDown(this, left, right);

                if (!cumulate || right.transitionToCumulate())
                    right.fork();
                if (!cumulate || left.transitionToCumulate())
                    left.exec();
            }
            else {
                int cb;
                for (;;) { // Establish action: sum, cumulate, or both
                    int b = phase;
                    if ((b & FINISHED) != 0) // already done
                        return false;
                    if ((b & CUMULATE) != 0)
                        cb = FINISHED;
                    else if (lo == op.origin) // combine leftmost
                        cb = (SUMMED|FINISHED);
                    else
                        cb = SUMMED;
                    if (phaseUpdater.compareAndSet(this, b, b|cb))
                        break;
                }

                if (cb == SUMMED)
                    op.sumLeaf(lo, hi, this);
                else if (cb == FINISHED)
                    op.cumulateLeaf(lo, hi, this);
                else if (cb == (SUMMED|FINISHED))
                    op.sumAndCumulateLeaf(lo, hi, this);

                // propagate up
                FJScan ch = this;
                FJScan par = parent;
                for (;;) {
                    if (par == null) {
                        if ((cb & FINISHED) != 0)
                            ch.complete(null);
                        break;
                    }
                    int pb = par.phase;
                    if ((pb & cb & FINISHED) != 0) { // both finished
                        ch = par;
                        par = par.parent;
                    }
                    else if ((pb & cb & SUMMED) != 0) { // both summed
                        op.pushUp(par, par.left, par.right);
                        int refork =
                            ((pb & CUMULATE) == 0 &&
                             par.lo == op.origin) ? CUMULATE : 0;
                        int nextPhase = pb|cb|refork;
                        if (pb == nextPhase ||
                            phaseUpdater.compareAndSet(par, pb, nextPhase)) {
                            if (refork != 0)
                                par.fork();
                            cb = SUMMED; // drop finished bit
                            ch = par;
                            par = par.parent;
                        }
                    }
                    else if (phaseUpdater.compareAndSet(par, pb, pb|cb))
                        break;
                }
            }
            return false;
        }

        // no-op versions of methods to get/set in/out, overridden as
        // appropriate in subclasses
        Object ogetIn() { return null; }
        Object ogetOut() { return null; }
        void rsetIn(Object x) { }
        void rsetOut(Object x) { }

        double dgetIn() { return 0; }
        double dgetOut() { return 0; }
        void dsetIn(double x) { }
        void dsetOut(double x) { }

        long lgetIn() { return 0; }
        long lgetOut() { return 0; }
        void lsetIn(long x) { }
        void lsetOut(long x) { }
    }

    // Subclasses adding in/out fields of the appropriate type
    static final class FJOScan extends FJScan {
        Object in;
        Object out;
        FJOScan(FJScan parent, FJScanOp op, int lo, int hi) {
            super(parent, op, lo, hi);
        }
        Object ogetIn() { return in; }
        Object ogetOut() { return out; }
        void rsetIn(Object x) { in = x; }
        void rsetOut(Object x) { out = x; }
    }

    static final class FJDScan extends FJScan {
        double in;
        double out;
        FJDScan(FJScan parent, FJScanOp op, int lo, int hi) {
            super(parent, op, lo, hi);
        }
        double dgetIn() { return in; }
        double dgetOut() { return out; }
        void dsetIn(double x) { in = x; }
        void dsetOut(double x) { out = x; }

    }

    static final class FJLScan extends FJScan {
        long in;
        long out;
        FJLScan(FJScan parent, FJScanOp op, int lo, int hi) {
            super(parent, op, lo, hi);
        }
        long lgetIn() { return in; }
        long lgetOut() { return out; }
        void lsetIn(long x) { in = x; }
        void lsetOut(long x) { out = x; }
    }

    /**
     * Computational operations for FJScan
     */
    abstract static class FJScanOp {
        final int threshold;
        final int origin;
        final int fence;
        FJScanOp(AbstractParallelAnyArray pap) {
            this.origin = pap.origin;
            this.fence = pap.fence;
            this.threshold = pap.computeThreshold();
        }
        abstract void pushDown(FJScan parent, FJScan left, FJScan right);
        abstract void pushUp(FJScan parent, FJScan left, FJScan right);
        abstract void sumLeaf(int lo, int hi, FJScan f);
        abstract void cumulateLeaf(int lo, int hi, FJScan f);
        abstract void sumAndCumulateLeaf(int lo, int hi, FJScan f);
        abstract FJScan newSubtask(FJScan parent, int lo, int hi);
    }

    abstract static class FJOScanOp extends FJScanOp {
        final Object[] array;
        final Reducer reducer;
        final Object base;
        FJOScanOp(AbstractParallelAnyArray.OPap pap,
                  Reducer reducer, Object base) {
            super(pap);
            this.array = pap.array;
            this.reducer = reducer;
            this.base = base;
        }
        final void pushDown(FJScan parent, FJScan left, FJScan right) {
            Object pin = parent.ogetIn();
            left.rsetIn(pin);
            right.rsetIn(reducer.op(pin, left.ogetOut()));
        }
        final void pushUp(FJScan parent, FJScan left, FJScan right) {
            parent.rsetOut(reducer.op(left.ogetOut(),
                                           right.ogetOut()));
        }
        final FJScan newSubtask(FJScan parent, int lo, int hi) {
            FJOScan f = new FJOScan(parent, this, lo, hi);
            f.in = base;
            f.out = base;
            return f;
        }
    }

    static final class FJOCumulateOp extends FJOScanOp {
        FJOCumulateOp(AbstractParallelAnyArray.OPap pap,
                      Reducer reducer, Object base) {
            super(pap, reducer, base);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            Object sum = base;
            if (hi != fence) {
                Object[] arr = array;
                for (int i = lo; i < hi; ++i)
                    sum = reducer.op(sum, arr[i]);
            }
            f.rsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            Object[] arr = array;
            Object sum = f.ogetIn();
            for (int i = lo; i < hi; ++i)
                arr[i] = sum = reducer.op(sum, arr[i]);
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            Object[] arr = array;
            Object sum = base;
            for (int i = lo; i < hi; ++i)
                arr[i] = sum = reducer.op(sum, arr[i]);
            f.rsetOut(sum);
        }
    }

    static final class FJOPrecumulateOp extends FJOScanOp {
        FJOPrecumulateOp(AbstractParallelAnyArray.OPap pap,
                         Reducer reducer, Object base) {
            super(pap, reducer, base);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            Object[] arr = array;
            Object sum = base;
            for (int i = lo; i < hi; ++i)
                sum = reducer.op(sum, arr[i]);
            f.rsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            Object[] arr = array;
            Object sum = f.ogetIn();
            for (int i = lo; i < hi; ++i) {
                Object x = arr[i];
                arr[i] = sum;
                sum = reducer.op(sum, x);
            }
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            Object[] arr = array;
            Object sum = base;
            for (int i = lo; i < hi; ++i) {
                Object x = arr[i];
                arr[i] = sum;
                sum = reducer.op(sum, x);
            }
            f.rsetOut(sum);
        }
    }

    abstract static class FJDScanOp extends FJScanOp {
        final double[] array;
        final DoubleReducer reducer;
        final double base;
        FJDScanOp(AbstractParallelAnyArray.DPap pap,
                  DoubleReducer reducer, double base) {
            super(pap);
            this.array = pap.array;
            this.reducer = reducer;
            this.base = base;
        }
        final void pushDown(FJScan parent, FJScan left, FJScan right) {
            double pin = parent.dgetIn();
            left.dsetIn(pin);
            right.dsetIn(reducer.op(pin, left.dgetOut()));
        }
        final void pushUp(FJScan parent, FJScan left, FJScan right) {
            parent.dsetOut(reducer.op(left.dgetOut(),
                                           right.dgetOut()));
        }
        final FJScan newSubtask(FJScan parent, int lo, int hi) {
            FJDScan f = new FJDScan(parent, this, lo, hi);
            f.in = base;
            f.out = base;
            return f;
        }
    }

    static final class FJDCumulateOp extends FJDScanOp {
        FJDCumulateOp(AbstractParallelAnyArray.DPap pap,
                      DoubleReducer reducer, double base) {
            super(pap, reducer, base);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            double sum = base;
            if (hi != fence) {
                double[] arr = array;
                for (int i = lo; i < hi; ++i)
                    sum = reducer.op(sum, arr[i]);
            }
            f.dsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = f.dgetIn();
            for (int i = lo; i < hi; ++i)
                arr[i] = sum = reducer.op(sum, arr[i]);
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = base;
            for (int i = lo; i < hi; ++i)
                arr[i] = sum = reducer.op(sum, arr[i]);
            f.dsetOut(sum);
        }
    }

    static final class FJDPrecumulateOp extends FJDScanOp {
        FJDPrecumulateOp(AbstractParallelAnyArray.DPap pap,
                         DoubleReducer reducer, double base) {
            super(pap, reducer, base);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = base;
            for (int i = lo; i < hi; ++i)
                sum = reducer.op(sum, arr[i]);
            f.dsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = f.dgetIn();
            for (int i = lo; i < hi; ++i) {
                double x = arr[i];
                arr[i] = sum;
                sum = reducer.op(sum, x);
            }
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = base;
            for (int i = lo; i < hi; ++i) {
                double x = arr[i];
                arr[i] = sum;
                sum = reducer.op(sum, x);
            }
            f.dsetOut(sum);
        }
    }

    abstract static class FJLScanOp extends FJScanOp {
        final long[] array;
        final LongReducer reducer;
        final long base;
        FJLScanOp(AbstractParallelAnyArray.LPap pap,
                  LongReducer reducer, long base) {
            super(pap);
            this.array = pap.array;
            this.reducer = reducer;
            this.base = base;
        }
        final void pushDown(FJScan parent, FJScan left, FJScan right) {
            long pin = parent.lgetIn();
            left.lsetIn(pin);
            right.lsetIn(reducer.op(pin, left.lgetOut()));
        }
        final void pushUp(FJScan parent, FJScan left, FJScan right) {
            parent.lsetOut(reducer.op(left.lgetOut(),
                                           right.lgetOut()));
        }
        final FJScan newSubtask(FJScan parent, int lo, int hi) {
            FJLScan f = new FJLScan(parent, this, lo, hi);
            f.in = base;
            f.out = base;
            return f;
        }
    }

    static final class FJLCumulateOp extends FJLScanOp {
        FJLCumulateOp(AbstractParallelAnyArray.LPap pap,
                      LongReducer reducer, long base) {
            super(pap, reducer, base);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            long sum = base;
            if (hi != fence) {
                long[] arr = array;
                for (int i = lo; i < hi; ++i)
                    sum = reducer.op(sum, arr[i]);
            }
            f.lsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = f.lgetIn();
            for (int i = lo; i < hi; ++i)
                arr[i] = sum = reducer.op(sum, arr[i]);
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = base;
            for (int i = lo; i < hi; ++i)
                arr[i] = sum = reducer.op(sum, arr[i]);
            f.lsetOut(sum);
        }
    }

    static final class FJLPrecumulateOp extends FJLScanOp {
        FJLPrecumulateOp(AbstractParallelAnyArray.LPap pap,
                         LongReducer reducer, long base) {
            super(pap, reducer, base);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = base;
            for (int i = lo; i < hi; ++i)
                sum = reducer.op(sum, arr[i]);
            f.lsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = f.lgetIn();
            for (int i = lo; i < hi; ++i) {
                long x = arr[i];
                arr[i] = sum;
                sum = reducer.op(sum, x);
            }
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = base;
            for (int i = lo; i < hi; ++i) {
                long x = arr[i];
                arr[i] = sum;
                sum = reducer.op(sum, x);
            }
            f.lsetOut(sum);
        }
    }

    // specialized versions for plus

    abstract static class FJDScanPlusOp extends FJScanOp {
        final double[] array;
        FJDScanPlusOp(AbstractParallelAnyArray.DPap pap) {
            super(pap);
            this.array = pap.array;
        }
        final void pushDown(FJScan parent, FJScan left, FJScan right) {
            double pin = parent.dgetIn();
            left.dsetIn(pin);
            right.dsetIn(pin + left.dgetOut());
        }
        final void pushUp(FJScan parent, FJScan left, FJScan right) {
            parent.dsetOut(left.dgetOut() + right.dgetOut());
        }
        final FJScan newSubtask(FJScan parent, int lo, int hi) {
            FJDScan f = new FJDScan(parent, this, lo, hi);
            f.in = 0.0;
            f.out = 0.0;
            return f;
        }
    }

    static final class FJDCumulatePlusOp extends FJDScanPlusOp {
        FJDCumulatePlusOp(AbstractParallelAnyArray.DPap pap) {
            super(pap);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            double sum = 0.0;
            if (hi != fence) {
                double[] arr = array;
                for (int i = lo; i < hi; ++i)
                    sum += arr[i];
            }
            f.dsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = f.dgetIn();
            for (int i = lo; i < hi; ++i)
                arr[i] = sum += arr[i];
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = 0.0;
            for (int i = lo; i < hi; ++i)
                arr[i] = sum += arr[i];
            f.dsetOut(sum);
        }
    }

    static final class FJDPrecumulatePlusOp extends FJDScanPlusOp {
        FJDPrecumulatePlusOp(AbstractParallelAnyArray.DPap pap) {
            super(pap);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = 0.0;
            for (int i = lo; i < hi; ++i)
                sum += arr[i];
            f.dsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = f.dgetIn();
            for (int i = lo; i < hi; ++i) {
                double x = arr[i];
                arr[i] = sum;
                sum += x;
            }
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            double[] arr = array;
            double sum = 0.0;
            for (int i = lo; i < hi; ++i) {
                double x = arr[i];
                arr[i] = sum;
                sum += x;
            }
            f.dsetOut(sum);
        }
    }

    abstract static class FJLScanPlusOp extends FJScanOp {
        final long[] array;
        FJLScanPlusOp(AbstractParallelAnyArray.LPap pap) {
            super(pap);
            this.array = pap.array;
        }
        final void pushDown(FJScan parent, FJScan left, FJScan right) {
            long pin = parent.lgetIn();
            left.lsetIn(pin);
            right.lsetIn(pin + left.lgetOut());
        }

        final void pushUp(FJScan parent, FJScan left, FJScan right) {
            parent.lsetOut(left.lgetOut() + right.lgetOut());
        }

        final FJScan newSubtask(FJScan parent, int lo, int hi) {
            FJLScan f = new FJLScan(parent, this, lo, hi);
            f.in = 0L;
            f.out = 0L;
            return f;
        }
    }

    static final class FJLCumulatePlusOp extends FJLScanPlusOp {
        FJLCumulatePlusOp(AbstractParallelAnyArray.LPap pap) {
            super(pap);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            long sum = 0L;
            if (hi != fence) {
                long[] arr = array;
                for (int i = lo; i < hi; ++i)
                    sum += arr[i];
            }
            f.lsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = f.lgetIn();
            for (int i = lo; i < hi; ++i)
                arr[i] = sum += arr[i];
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = 0L;
            for (int i = lo; i < hi; ++i)
                arr[i] = sum += arr[i];
            f.lsetOut(sum);
        }
    }

    static final class FJLPrecumulatePlusOp extends FJLScanPlusOp {
        FJLPrecumulatePlusOp(AbstractParallelAnyArray.LPap pap) {
            super(pap);
        }
        void sumLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = 0L;
            for (int i = lo; i < hi; ++i)
                sum += arr[i];
            f.lsetOut(sum);
        }
        void cumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = f.lgetIn();
            for (int i = lo; i < hi; ++i) {
                long x = arr[i];
                arr[i] = sum;
                sum += x;
            }
        }
        void sumAndCumulateLeaf(int lo, int hi, FJScan f) {
            long[] arr = array;
            long sum = 0L;
            for (int i = lo; i < hi; ++i) {
                long x = arr[i];
                arr[i] = sum;
                sum += x;
            }
            f.lsetOut(sum);
        }
    }

}
