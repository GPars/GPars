/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import java.util.Comparator;

/**
 * Interfaces and utilities declaring per-element operations used
 * within parallel methods on aggregates. This class provides type
 * names for all operation signatures accepting zero, one or two
 * arguments, and returning zero or one results, for parameterized
 * types, as well as specializations to {@code int}, {@code long},
 * and {@code double}. In keeping with normal Java evaluation rules
 * that promote, for example {@code short} to {@code int}, operation
 * names for these smaller types are absent.
 *
 * <p><b>Preliminary release note: Some of the declarations in this
 * class are likely to be moved elsewhere in the JDK libraries
 * upon actual release, and most likely will not all nested in the
 * same class.</b>
 *
 * <p>The naming conventions are as follows:
 * <ul>
 *
 * <li> The name of the single method declared in each interface is
 * simply {@code op} (short for "operate").
 *
 * <li> An {@code Op} (short for "operation") maps a single argument to
 * a result. Example: negating a value.
 *
 * <li> The names for scalar ops accepting and returning the same type
 * are prefaced by their type name.
 *
 * <li> A {@code BinaryOp} maps two arguments to a result. Example:
 * dividing two numbers
 *
 * <li>A {@code Reducer} is an <em>associative</em> binary op
 * accepting and returning values of the same type; where op(a, op(b,
 * c)) should have the same result as op(op(a, b), c).  Example:
 * adding two numbers.
 *
 * <li> Scalar binary ops accepting and returning the same type
 * include their type name.
 *
 * <li> Mixed-type operators are named just by their argument type
 * names.
 *
 * <li> A {@code Generator} takes no arguments and returns a result.
 * Examples: random number generators, builders
 *
 * <li> A {@code Procedure} accepts an argument but doesn't return a
 * result. Example: printing a value.  An {@code Action} is a
 * Procedure that takes no arguments.
 *
 * <li>A {@code Predicate} accepts a value and returns a boolean indicator
 * that the argument obeys some property. Example: testing if a number is even.
 *
 * <li>A {@code BinaryPredicate} accepts two values and returns a
 * boolean indicator that the arguments obeys some relation. Example:
 * testing if two numbers are relatively prime.
 *
 * <li>Scalar versions of {@link Comparator} have the same properties
 * as the Object version -- returning negative, zero, or positive
 * if the first argument is less, equal, or greater than the second.
 *
 * </ul>
 *
 * <table border=1 cellpadding=0 cellspacing=0 > <caption>result = op(a) or
 * result = op(a,b)</caption>
 * <tr>
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <tr>
 * <td >
 * <th colspan=2 >arg types
 * <th colspan=4 >result type
 * <td >
 * <tr>
 * <td >
 * <th >a
 * <th >b
 * <th >{@code int}
 * <th >{@code long}
 * <th >{@code double}
 * <th >Object
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <tr>
 * <td >
 * <td >{@code int}
 * <td ><em>&lt;none&gt;</em>
 * <td ><a href="Ops.IntOp.html">Ops.IntOp</a>
 * <td ><a href="Ops.IntToLong.html">Ops.IntToLong</a>
 * <td ><a href="Ops.IntToDouble.html">Ops.IntToDouble</a>
 * <td ><a href="Ops.IntToObject.html">Ops.IntToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code int}
 * <td ><a href="Ops.BinaryIntOp.html">Ops.BinaryIntOp</a>
 * <td ><a href="Ops.IntAndIntToLong.html">Ops.IntAndIntToLong</a>
 * <td ><a href="Ops.IntAndIntToDouble.html">Ops.IntAndIntToDouble</a>
 * <td ><a href="Ops.IntAndIntToObject.html">Ops.IntAndIntToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code long}
 * <td ><a href="Ops.IntAndLongToInt.html">Ops.IntAndLongToInt</a>
 * <td ><a href="Ops.IntAndLongToLong.html">Ops.IntAndLongToLong</a>
 * <td ><a href="Ops.IntAndLongToDouble.html">Ops.IntAndLongToDouble</a>
 * <td ><a href="Ops.IntAndLongToObject.html">Ops.IntAndLongToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code double}
 * <td ><a href="Ops.IntAndDoubleToInt.html">Ops.IntAndDoubleToInt</a>
 * <td ><a href="Ops.IntAndDoubleToLong.html">Ops.IntAndDoubleToLong</a>
 * <td ><a href="Ops.IntAndDoubleToDouble.html">Ops.IntAndDoubleToDouble</a>
 * <td ><a href="Ops.IntAndDoubleToObject.html">Ops.IntAndDoubleToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >Object
 * <td ><a href="Ops.IntAndObjectToInt.html">Ops.IntAndObjectToInt</a>
 * <td ><a href="Ops.IntAndObjectToLong.html">Ops.IntAndObjectToLong</a>
 * <td ><a href="Ops.IntAndObjectToDouble.html">Ops.IntAndObjectToDouble</a>
 * <td ><a href="Ops.IntAndObjectToObject.html">Ops.IntAndObjectToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <tr>
 * <td >
 * <td >{@code long}
 * <td ><em>&lt;none&gt;</em>
 * <td ><a href="Ops.LongToInt.html">Ops.LongToInt</a>
 * <td ><a href="Ops.LongOp.html">Ops.LongOp</a>
 * <td ><a href="Ops.LongToDouble.html">Ops.LongToDouble</a>
 * <td ><a href="Ops.LongToObject.html">Ops.LongToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code int}
 * <td ><a href="Ops.LongAndIntToInt.html">Ops.LongAndIntToInt</a>
 * <td ><a href="Ops.LongAndIntToLong.html">Ops.LongAndIntToLong</a>
 * <td ><a href="Ops.LongAndIntToDouble.html">Ops.LongAndIntToDouble</a>
 * <td ><a href="Ops.LongAndIntToObject.html">Ops.LongAndIntToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code long}
 * <td ><a href="Ops.LongAndLongToInt.html">Ops.LongAndLongToInt</a>
 * <td ><a href="Ops.BinaryLongOp.html">Ops.BinaryLongOp</a>
 * <td ><a href="Ops.LongAndLongToDouble.html">Ops.LongAndLongToDouble</a>
 * <td ><a href="Ops.LongAndLongToObject.html">Ops.LongAndLongToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code double}
 * <td ><a href="Ops.LongAndDoubleToInt.html">Ops.LongAndDoubleToInt</a>
 * <td ><a href="Ops.LongAndDoubleToLong.html">Ops.LongAndDoubleToLong</a>
 * <td ><a href="Ops.LongAndDoubleToDouble.html">Ops.LongAndDoubleToDouble</a>
 * <td ><a href="Ops.LongAndDoubleToObject.html">Ops.LongAndDoubleToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >Object
 * <td ><a href="Ops.LongAndObjectToInt.html">Ops.LongAndObjectToInt</a>
 * <td ><a href="Ops.LongAndObjectToLong.html">Ops.LongAndObjectToLong</a>
 * <td ><a href="Ops.LongAndObjectToDouble.html">Ops.LongAndObjectToDouble</a>
 * <td ><a href="Ops.LongAndObjectToObject.html">Ops.LongAndObjectToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <tr>
 * <td >
 * <td >{@code double}
 * <td ><em>&lt;none&gt;</em>
 * <td ><a href="Ops.DoubleToInt.html">Ops.DoubleToInt</a>
 * <td ><a href="Ops.DoubleToLong.html">Ops.DoubleToLong</a>
 * <td ><a href="Ops.DoubleOp.html">Ops.DoubleOp</a>
 * <td ><a href="Ops.DoubleToObject.html">Ops.DoubleToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code int}
 * <td ><a href="Ops.DoubleAndIntToInt.html">Ops.DoubleAndIntToInt</a>
 * <td ><a href="Ops.DoubleAndIntToLong.html">Ops.DoubleAndIntToLong</a>
 * <td ><a href="Ops.DoubleAndIntToDouble.html">Ops.DoubleAndIntToDouble</a>
 * <td ><a href="Ops.DoubleAndIntToObject.html">Ops.DoubleAndIntToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code long}
 * <td ><a href="Ops.DoubleAndLongToInt.html">Ops.DoubleAndLongToInt</a>
 * <td ><a href="Ops.DoubleAndLongToLong.html">Ops.DoubleAndLongToLong</a>
 * <td ><a href="Ops.DoubleAndLongToDouble.html">Ops.DoubleAndLongToDouble</a>
 * <td ><a href="Ops.DoubleAndLongToObject.html">Ops.DoubleAndLongToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code double}
 * <td ><a href="Ops.DoubleAndDoubleToInt.html">Ops.DoubleAndDoubleToInt</a>
 * <td ><a href="Ops.DoubleAndDoubleToLong.html">Ops.DoubleAndDoubleToLong</a>
 * <td ><a href="Ops.BinaryDoubleOp.html">Ops.BinaryDoubleOp</a>
 * <td ><a href="Ops.DoubleAndDoubleToObject.html">Ops.DoubleAndDoubleToObject</a>
 * <tr>
 * <td >
 * <td >
 * <td >Object
 * <td ><a href="Ops.DoubleAndObjectToInt.html">Ops.DoubleAndObjectToInt</a>
 * <td ><a href="Ops.DoubleAndObjectToLong.html">Ops.DoubleAndObjectToLong</a>
 * <td ><a href="Ops.DoubleAndObjectToDouble.html">Ops.DoubleAndObjectToDouble</a>
 * <td ><a href="Ops.DoubleAndObjectToObject.html">Ops.DoubleAndObjectToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <tr>
 * <td >
 * <td >Object
 * <td ><em>&lt;none&gt;</em>
 * <td ><a href="Ops.ObjectToInt.html">Ops.ObjectToInt</a>
 * <td ><a href="Ops.ObjectToLong.html">Ops.ObjectToLong</a>
 * <td ><a href="Ops.ObjectToDouble.html">Ops.ObjectToDouble</a>
 * <td ><a href="Ops.Op.html">Ops.Op</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code int}
 * <td ><a href="Ops.ObjectAndIntToInt.html">Ops.ObjectAndIntToInt</a>
 * <td ><a href="Ops.ObjectAndIntToLong.html">Ops.ObjectAndIntToLong</a>
 * <td ><a href="Ops.ObjectAndIntToDouble.html">Ops.ObjectAndIntToDouble</a>
 * <td ><a href="Ops.ObjectAndIntToObject.html">Ops.ObjectAndIntToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code long}
 * <td ><a href="Ops.ObjectAndLongToInt.html">Ops.ObjectAndLongToInt</a>
 * <td ><a href="Ops.ObjectAndLongToLong.html">Ops.ObjectAndLongToLong</a>
 * <td ><a href="Ops.ObjectAndLongToDouble.html">Ops.ObjectAndLongToDouble</a>
 * <td ><a href="Ops.ObjectAndLongToObject.html">Ops.ObjectAndLongToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >{@code double}
 * <td ><a href="Ops.ObjectAndDoubleToInt.html">Ops.ObjectAndDoubleToInt</a>
 * <td ><a href="Ops.ObjectAndDoubleToLong.html">Ops.ObjectAndDoubleToLong</a>
 * <td ><a href="Ops.ObjectAndDoubleToDouble.html">Ops.ObjectAndDoubleToDouble</a>
 * <td ><a href="Ops.ObjectAndDoubleToObject.html">Ops.ObjectAndDoubleToObject</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >Object
 * <td ><a href="Ops.ObjectAndObjectToInt.html">Ops.ObjectAndObjectToInt</a>
 * <td ><a href="Ops.ObjectAndObjectToLong.html">Ops.ObjectAndObjectToLong</a>
 * <td ><a href="Ops.ObjectAndObjectToDouble.html">Ops.ObjectAndObjectToDouble</a>
 * <td ><a href="Ops.BinaryOp.html">Ops.BinaryOp</a>
 * <td >
 * <tr>
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td >
 * <td > </table>
 *
 * <p>In addition to stated signatures, implementations of these
 * interfaces must work safely in parallel. In general, this means
 * methods should operate only on their arguments, and should not rely
 * on ThreadLocals, unsafely published globals, or other unsafe
 * constructions. Additionally, they should not block waiting for
 * synchronization.
 *
 * <p>This class is normally best used via {@code import static}.
 */
public class Ops {
    private Ops() {} // disable construction

    // Thanks to David Biesack for the above html table
    // You want to read/edit this with a wide editor panel

    public static interface Op<A,R>                      { R       op(A a);}
    public static interface BinaryOp<A,B,R>              { R       op(A a, B b);}
    public static interface Predicate<A>                 { boolean op(A a);}
    public static interface BinaryPredicate<A,B>         { boolean op(A a, B b);}
    public static interface Procedure<A>                 { void    op(A a);}
    public static interface Generator<R>                 { R       op();}
    public static interface Reducer<A> extends BinaryOp<A,A,A> {}

    public static interface IntOp                        { int     op(int a);}
    public static interface BinaryIntOp                  { int     op(int a, int b);}
    public static interface IntPredicate                 { boolean op(int a);}
    public static interface IntProcedure                 { void    op(int a);}
    public static interface IntGenerator                 { int     op();}
    public static interface BinaryIntPredicate           { boolean op(int a, int b);}
    public static interface IntReducer extends BinaryIntOp {}
    public static interface IntComparator                { int     compare(int a, int b);}

    public static interface LongOp                       { long    op(long a);}
    public static interface BinaryLongOp                 { long    op(long a, long b);}
    public static interface LongPredicate                { boolean op(long a);}
    public static interface BinaryLongPredicate          { boolean op(long a, long b);}
    public static interface LongProcedure                { void    op(long a);}
    public static interface LongGenerator                { long    op();}
    public static interface LongReducer extends BinaryLongOp {}
    public static interface LongComparator               { int     compare(long a, long b);}

    public static interface DoubleOp                     { double  op(double a);}
    public static interface BinaryDoubleOp               { double  op(double a, double b);}
    public static interface DoublePredicate              { boolean op(double a);}
    public static interface BinaryDoublePredicate        { boolean op(double a, double b);}
    public static interface DoubleProcedure              { void    op(double a);}
    public static interface DoubleGenerator              { double  op();}
    public static interface DoubleReducer extends BinaryDoubleOp {}
    public static interface DoubleComparator             { int     compare(double a, double b);}

    public static interface Action                       { void    op();}

    // mixed mode ops
    public static interface IntToLong                    { long    op(int a);}
    public static interface IntToDouble                  { double  op(int a);}
    public static interface IntToObject<R>               { R       op(int a);}
    public static interface LongToInt                    { int     op(long a);}
    public static interface LongToDouble                 { double  op(long a);}
    public static interface LongToObject<R>              { R       op(long a);}
    public static interface DoubleToInt                  { int     op(double a);}
    public static interface DoubleToLong                 { long    op(double a);}
    public static interface DoubleToObject<R>            { R       op(double a);}
    public static interface ObjectToInt<A>               { int     op(A a);}
    public static interface ObjectToLong<A>              { long    op(A a);}
    public static interface ObjectToDouble<A>            { double  op(A a);}

    public static interface IntAndIntProcedure           { void    op(int a, int b);}
    public static interface IntAndIntToLong              { long    op(int a, int b);}
    public static interface IntAndIntToDouble            { double  op(int a, int b);}
    public static interface IntAndIntToObject<R>         { R       op(int a, int b);}
    public static interface IntAndLongProcedure          { void    op(int a, long b);}
    public static interface IntAndLongPredicate          { boolean op(int a, long b);}
    public static interface IntAndLongToInt              { int     op(int a, long b);}
    public static interface IntAndLongToLong             { long    op(int a, long b);}
    public static interface IntAndLongToDouble           { double  op(int a, long b);}
    public static interface IntAndLongToObject<R>        { R       op(int a, long b);}
    public static interface IntAndDoubleProcedure        { void    op(int a, double b);}
    public static interface IntAndDoublePredicate        { boolean op(int a, double b);}
    public static interface IntAndDoubleToInt            { int     op(int a, double b);}
    public static interface IntAndDoubleToLong           { long    op(int a, double b);}
    public static interface IntAndDoubleToDouble         { double  op(int a, double b);}
    public static interface IntAndDoubleToObject<R>      { R       op(int a, double b);}
    public static interface IntAndObjectProcedure<A>     { void    op(int a, A b);}
    public static interface IntAndObjectPredicate<A>     { boolean op(int a, A b);}
    public static interface IntAndObjectToInt<A>         { int     op(int a, A b);}
    public static interface IntAndObjectToLong<A>        { long    op(int a, A b);}
    public static interface IntAndObjectToDouble<A>      { double  op(int a, A b);}
    public static interface IntAndObjectToObject<A,R>    { R       op(int a, A b);}
    public static interface LongAndIntProcedure          { void    op(long a, int b);}
    public static interface LongAndIntPredicate          { boolean op(long a, int b);}
    public static interface LongAndIntToInt              { int     op(long a, int b);}
    public static interface LongAndIntToLong             { long    op(long a, int b);}
    public static interface LongAndIntToDouble           { double  op(long a, int b);}
    public static interface LongAndIntToObject<R>        { R       op(long a, int b);}
    public static interface LongAndLongProcedure         { void    op(long a, long b);}
    public static interface LongAndLongToInt             { int     op(long a, long b);}
    public static interface LongAndLongToDouble          { double  op(long a, long b);}
    public static interface LongAndLongToObject<R>       { R       op(long a, long b);}
    public static interface LongAndDoubleProcedure       { void    op(long a, double b);}
    public static interface LongAndDoublePredicate       { boolean op(long a, double b);}
    public static interface LongAndDoubleToInt           { int     op(long a, double b);}
    public static interface LongAndDoubleToLong          { long    op(long a, double b);}
    public static interface LongAndDoubleToDouble        { double  op(long a, double b);}
    public static interface LongAndDoubleToObject<R>     { R       op(long a, double b);}
    public static interface LongAndObjectProcedure<A>    { void    op(long a, A b);}
    public static interface LongAndObjectPredicate<A>    { boolean op(long a, A b);}
    public static interface LongAndObjectToInt<A>        { int     op(long a, A b);}
    public static interface LongAndObjectToLong<A>       { long    op(long a, A b);}
    public static interface LongAndObjectToDouble<A>     { double  op(long a, A b);}
    public static interface LongAndObjectToObject<A,R>   { R       op(long a, A b);}
    public static interface DoubleAndIntProcedure        { void    op(double a, int b);}
    public static interface DoubleAndIntPredicate        { boolean op(double a, int b);}
    public static interface DoubleAndIntToInt            { int     op(double a, int b);}
    public static interface DoubleAndIntToLong           { long    op(double a, int b);}
    public static interface DoubleAndIntToDouble         { double  op(double a, int b);}
    public static interface DoubleAndIntToObject<R>      { R       op(double a, int b);}
    public static interface DoubleAndLongProcedure       { void    op(double a, long b);}
    public static interface DoubleAndLongPredicate       { boolean op(double a, long b);}
    public static interface DoubleAndLongToInt           { int     op(double a, long b);}
    public static interface DoubleAndLongToLong          { long    op(double a, long b);}
    public static interface DoubleAndLongToDouble        { double  op(double a, long b);}
    public static interface DoubleAndLongToObject<R>     { R       op(double a, long b);}
    public static interface DoubleAndDoubleProcedure     { void    op(double a, double b);}
    public static interface DoubleAndDoubleToInt         { int     op(double a, double b);}
    public static interface DoubleAndDoubleToLong        { long    op(double a, double b);}
    public static interface DoubleAndDoubleToObject<R>   { R       op(double a, double b);}
    public static interface DoubleAndObjectProcedure<A>  { void    op(double a, A b);}
    public static interface DoubleAndObjectPredicate<A>  { boolean op(double a, A b);}
    public static interface DoubleAndObjectToInt<A>      { int     op(double a, A b);}
    public static interface DoubleAndObjectToLong<A>     { long    op(double a, A b);}
    public static interface DoubleAndObjectToDouble<A>   { double  op(double a, A b);}
    public static interface DoubleAndObjectToObject<A,R> { R       op(double a, A b);}
    public static interface ObjectAndIntProcedure<A>     { void    op(A a, int b);}
    public static interface ObjectAndIntPredicate<A>     { boolean op(A a, int b);}
    public static interface ObjectAndIntToInt<A>         { int     op(A a, int b);}
    public static interface ObjectAndIntToLong<A>        { long    op(A a, int b);}
    public static interface ObjectAndIntToDouble<A>      { double  op(A a, int b);}
    public static interface ObjectAndIntToObject<A,R>    { R       op(A a, int b);}
    public static interface ObjectAndLongProcedure<A>    { void    op(A a, long b);}
    public static interface ObjectAndLongPredicate<A>    { boolean op(A a, long b);}
    public static interface ObjectAndLongToInt<A>        { int     op(A a, long b);}
    public static interface ObjectAndLongToLong<A>       { long    op(A a, long b);}
    public static interface ObjectAndLongToDouble<A>     { double  op(A a, long b);}
    public static interface ObjectAndLongToObject<A,R>   { R       op(A a, long b);}
    public static interface ObjectAndDoubleProcedure<A>  { void    op(A a, double b);}
    public static interface ObjectAndDoublePredicate<A>  { boolean op(A a, double b);}
    public static interface ObjectAndDoubleToInt<A>      { int     op(A a, double b);}
    public static interface ObjectAndDoubleToLong<A>     { long    op(A a, double b);}
    public static interface ObjectAndDoubleToDouble<A>   { double  op(A a, double b);}
    public static interface ObjectAndDoubleToObject<A,R> { R       op(A a, double b);}
    public static interface ObjectAndObjectProcedure<A,B>{ void    op(A a, B b);}
    public static interface ObjectAndObjectToInt<A,B>    { int     op(A a, B b);}
    public static interface ObjectAndObjectToLong<A,B>   { long    op(A a, B b);}
    public static interface ObjectAndObjectToDouble<A,B> { double  op(A a, B b);}
}
