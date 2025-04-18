package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.Column
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.RowExpression
import org.jetbrains.kotlinx.dataframe.RowFilter
import org.jetbrains.kotlinx.dataframe.Selector
import org.jetbrains.kotlinx.dataframe.aggregation.ColumnsForAggregateSelector
import org.jetbrains.kotlinx.dataframe.columns.ColumnReference
import org.jetbrains.kotlinx.dataframe.impl.aggregation.aggregators.Aggregators
import org.jetbrains.kotlinx.dataframe.impl.aggregation.columnValues
import org.jetbrains.kotlinx.dataframe.impl.aggregation.comparableColumns
import org.jetbrains.kotlinx.dataframe.impl.aggregation.internal
import org.jetbrains.kotlinx.dataframe.impl.aggregation.modes.aggregateAll
import org.jetbrains.kotlinx.dataframe.impl.aggregation.modes.aggregateFor
import org.jetbrains.kotlinx.dataframe.impl.aggregation.modes.aggregateOf
import org.jetbrains.kotlinx.dataframe.impl.aggregation.numberColumns
import org.jetbrains.kotlinx.dataframe.impl.aggregation.remainingColumnsSelector
import org.jetbrains.kotlinx.dataframe.impl.aggregation.withExpr
import org.jetbrains.kotlinx.dataframe.impl.columns.toColumns
import org.jetbrains.kotlinx.dataframe.impl.columns.toColumnsOf
import org.jetbrains.kotlinx.dataframe.impl.columns.toComparableColumns
import org.jetbrains.kotlinx.dataframe.impl.columns.toNumberColumns
import org.jetbrains.kotlinx.dataframe.impl.emptyPath
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf

// region count

public fun <T> PivotGroupBy<T>.count(): DataFrame<T> = aggregate { count() default 0 }
public fun <T> PivotGroupBy<T>.count(predicate: RowFilter<T>): DataFrame<T> = aggregate { count(predicate) default 0 }

// endregion

// region matches

public fun <T> PivotGroupBy<T>.matches(): DataFrame<T> = matches(yes = true, no = false)
public fun <T, R> PivotGroupBy<T>.matches(yes: R, no: R): DataFrame<T> = aggregate { yes default no }

// endregion

// region frames

public fun <T> PivotGroupBy<T>.frames(): DataFrame<T> = aggregate { this }

// endregion

// region with

public inline fun <T, reified V> PivotGroupBy<T>.with(noinline expression: RowExpression<T, V>): DataFrame<T> {
    val type = typeOf<V>()
    return aggregate { internal().withExpr(type, emptyPath(), expression) }
}

// endregion

// region values

public fun <T> PivotGroupBy<T>.values(dropNA: Boolean = false, distinct: Boolean = false, separate: Boolean = false): DataFrame<T> = values(dropNA, distinct, separate, remainingColumnsSelector())

public fun <T> PivotGroupBy<T>.values(
    vararg columns: Column,
    dropNA: Boolean = false,
    distinct: Boolean = false,
    separate: Boolean = false
): DataFrame<T> = values(dropNA, distinct, separate) { columns.toColumns() }
public fun <T> PivotGroupBy<T>.values(
    vararg columns: String,
    dropNA: Boolean = false,
    distinct: Boolean = false,
    separate: Boolean = false
): DataFrame<T> = values(dropNA, distinct, separate) { columns.toColumns() }
public fun <T> PivotGroupBy<T>.values(
    vararg columns: KProperty<*>,
    dropNA: Boolean = false,
    distinct: Boolean = false,
    separate: Boolean = false
): DataFrame<T> = values(dropNA, distinct, separate) { columns.toColumns() }
public fun <T> PivotGroupBy<T>.values(
    dropNA: Boolean = false,
    distinct: Boolean = false,
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, *>
): DataFrame<T> =
    aggregate(separate = separate) { internal().columnValues(columns, false, dropNA, distinct) }

// endregion

// region reducers

public data class ReducedPivotGroupBy<T>(
    @PublishedApi internal val pivot: PivotGroupBy<T>,
    @PublishedApi internal val reducer: Selector<DataFrame<T>, DataRow<T>?>
)

@PublishedApi
internal fun <T> PivotGroupBy<T>.reduce(reducer: Selector<DataFrame<T>, DataRow<T>?>): ReducedPivotGroupBy<T> = ReducedPivotGroupBy(this, reducer)

public fun <T> PivotGroupBy<T>.first(): ReducedPivotGroupBy<T> = reduce { firstOrNull() }

public fun <T> PivotGroupBy<T>.first(predicate: RowFilter<T>): ReducedPivotGroupBy<T> = reduce { firstOrNull(predicate) }

public fun <T> PivotGroupBy<T>.last(): ReducedPivotGroupBy<T> = reduce { lastOrNull() }

public fun <T> PivotGroupBy<T>.last(predicate: RowFilter<T>): ReducedPivotGroupBy<T> = reduce { lastOrNull(predicate) }

public fun <T, R : Comparable<R>> PivotGroupBy<T>.minBy(rowExpression: RowExpression<T, R>): ReducedPivotGroupBy<T> = reduce { minByOrNull(rowExpression) }
public fun <T, C : Comparable<C>> PivotGroupBy<T>.minBy(column: ColumnReference<C?>): ReducedPivotGroupBy<T> = reduce { minByOrNull(column) }
public fun <T> PivotGroupBy<T>.minBy(column: String): ReducedPivotGroupBy<T> = minBy(column.toColumnAccessor().cast<Comparable<Any?>>())
public fun <T, C : Comparable<C>> PivotGroupBy<T>.minBy(column: KProperty<C?>): ReducedPivotGroupBy<T> = minBy(column.toColumnAccessor())

public fun <T, R : Comparable<R>> PivotGroupBy<T>.maxBy(rowExpression: RowExpression<T, R>): ReducedPivotGroupBy<T> = reduce { maxByOrNull(rowExpression) }
public fun <T, C : Comparable<C>> PivotGroupBy<T>.maxBy(column: ColumnReference<C?>): ReducedPivotGroupBy<T> = reduce { maxByOrNull(column) }
public fun <T> PivotGroupBy<T>.maxBy(column: String): ReducedPivotGroupBy<T> = maxBy(column.toColumnAccessor().cast<Comparable<Any?>>())
public fun <T, C : Comparable<C>> PivotGroupBy<T>.maxBy(column: KProperty<C?>): ReducedPivotGroupBy<T> = maxBy(column.toColumnAccessor())

// region values

public fun <T> ReducedPivotGroupBy<T>.values(
    separate: Boolean = false
): DataFrame<T> = values(separate, pivot.remainingColumnsSelector())

public fun <T> ReducedPivotGroupBy<T>.values(
    vararg columns: Column,
    separate: Boolean = false
): DataFrame<T> = values(separate) { columns.toColumns() }

public fun <T> ReducedPivotGroupBy<T>.values(
    vararg columns: String,
    separate: Boolean = false
): DataFrame<T> = values(separate) { columns.toColumns() }

public fun <T> ReducedPivotGroupBy<T>.values(
    vararg columns: KProperty<*>,
    separate: Boolean = false
): DataFrame<T> = values(separate) { columns.toColumns() }

public fun <T> ReducedPivotGroupBy<T>.values(
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, *>
): DataFrame<T> = pivot.aggregate(separate = separate) { internal().columnValues(columns, reducer) }

// endregion

// region with

public inline fun <T, reified V> ReducedPivotGroupBy<T>.with(noinline expression: RowExpression<T, V>): DataFrame<T> {
    val type = typeOf<V>()
    return pivot.aggregate {
        val value = reducer(this)?.let {
            val value = expression(it, it)
            if (value is Column) it[value]
            else value
        }
        internal().yield(emptyPath(), value, type)
    }
}

// endregion

// endregion

// region min

public fun <T> PivotGroupBy<T>.min(separate: Boolean = false): DataFrame<T> = minFor(separate, comparableColumns())

public fun <T, R : Comparable<R>> PivotGroupBy<T>.minFor(
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, R?>
): DataFrame<T> =
    Aggregators.min.aggregateFor(this, separate, columns)
public fun <T> PivotGroupBy<T>.minFor(vararg columns: String, separate: Boolean = false): DataFrame<T> = minFor(separate) { columns.toComparableColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.minFor(
    vararg columns: ColumnReference<R?>,
    separate: Boolean = false
): DataFrame<T> = minFor(separate) { columns.toColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.minFor(
    vararg columns: KProperty<R?>,
    separate: Boolean = false
): DataFrame<T> = minFor(separate) { columns.toColumns() }

public fun <T, R : Comparable<R>> PivotGroupBy<T>.min(columns: ColumnsSelector<T, R?>): DataFrame<T> = Aggregators.min.aggregateAll(this, columns)
public fun <T> PivotGroupBy<T>.min(vararg columns: String): DataFrame<T> = min { columns.toComparableColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.min(vararg columns: ColumnReference<R?>): DataFrame<T> = min { columns.toColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.min(vararg columns: KProperty<R?>): DataFrame<T> = min { columns.toColumns() }

public fun <T, R : Comparable<R>> PivotGroupBy<T>.minOf(rowExpression: RowExpression<T, R>): DataFrame<T> = aggregate { minOf(rowExpression) }

// endregion

// region max

public fun <T> PivotGroupBy<T>.max(separate: Boolean = false): DataFrame<T> = maxFor(separate, comparableColumns())

public fun <T, R : Comparable<R>> PivotGroupBy<T>.maxFor(
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, R?>
): DataFrame<T> =
    Aggregators.max.aggregateFor(this, separate, columns)
public fun <T> PivotGroupBy<T>.maxFor(vararg columns: String, separate: Boolean = false): DataFrame<T> = maxFor(separate) { columns.toComparableColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.maxFor(
    vararg columns: ColumnReference<R?>,
    separate: Boolean = false
): DataFrame<T> = maxFor(separate) { columns.toColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.maxFor(
    vararg columns: KProperty<R?>,
    separate: Boolean = false
): DataFrame<T> = maxFor(separate) { columns.toColumns() }

public fun <T, R : Comparable<R>> PivotGroupBy<T>.max(columns: ColumnsSelector<T, R?>): DataFrame<T> = Aggregators.max.aggregateAll(this, columns)
public fun <T> PivotGroupBy<T>.max(vararg columns: String): DataFrame<T> = max { columns.toComparableColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.max(vararg columns: ColumnReference<R?>): DataFrame<T> = max { columns.toColumns() }
public fun <T, R : Comparable<R>> PivotGroupBy<T>.max(vararg columns: KProperty<R?>): DataFrame<T> = max { columns.toColumns() }

public fun <T, R : Comparable<R>> PivotGroupBy<T>.maxOf(rowExpression: RowExpression<T, R>): DataFrame<T> = aggregate { maxOf(rowExpression) }

// endregion

// region sum

public fun <T> PivotGroupBy<T>.sum(separate: Boolean = false): DataFrame<T> = sumFor(separate, numberColumns())

public fun <T, R : Number> PivotGroupBy<T>.sumFor(
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, R?>
): DataFrame<T> =
    Aggregators.sum.aggregateFor(this, separate, columns)
public fun <T> PivotGroupBy<T>.sumFor(vararg columns: String, separate: Boolean = false): DataFrame<T> = sumFor(separate) { columns.toNumberColumns() }
public fun <T, C : Number> PivotGroupBy<T>.sumFor(
    vararg columns: ColumnReference<C?>,
    separate: Boolean = false
): DataFrame<T> = sumFor(separate) { columns.toColumns() }
public fun <T, C : Number> PivotGroupBy<T>.sumFor(vararg columns: KProperty<C?>, separate: Boolean = false): DataFrame<T> = sumFor(separate) { columns.toColumns() }

public fun <T, C : Number> PivotGroupBy<T>.sum(columns: ColumnsSelector<T, C?>): DataFrame<T> =
    Aggregators.sum.aggregateAll(this, columns)
public fun <T> PivotGroupBy<T>.sum(vararg columns: String): DataFrame<T> = sum { columns.toNumberColumns() }
public fun <T, C : Number> PivotGroupBy<T>.sum(vararg columns: ColumnReference<C?>): DataFrame<T> = sum { columns.toColumns() }
public fun <T, C : Number> PivotGroupBy<T>.sum(vararg columns: KProperty<C?>): DataFrame<T> = sum { columns.toColumns() }

public inline fun <T, reified R : Number> PivotGroupBy<T>.sumOf(crossinline expression: RowExpression<T, R>): DataFrame<T> =
    Aggregators.sum.aggregateOf(this, expression)

// endregion

// region mean

public fun <T> PivotGroupBy<T>.mean(separate: Boolean = false, skipNA: Boolean = skipNA_default): DataFrame<T> = meanFor(skipNA, separate, numberColumns())

public fun <T, C : Number> PivotGroupBy<T>.meanFor(
    skipNA: Boolean = skipNA_default,
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, C?>
): DataFrame<T> = Aggregators.mean(skipNA).aggregateFor(this, separate, columns)
public fun <T> PivotGroupBy<T>.meanFor(
    vararg columns: String,
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default
): DataFrame<T> = meanFor(skipNA, separate) { columns.toNumberColumns() }
public fun <T, C : Number> PivotGroupBy<T>.meanFor(
    vararg columns: ColumnReference<C?>,
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
): DataFrame<T> = meanFor(skipNA, separate) { columns.toColumns() }
public fun <T, C : Number> PivotGroupBy<T>.meanFor(
    vararg columns: KProperty<C?>,
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
): DataFrame<T> = meanFor(skipNA, separate) { columns.toColumns() }

public fun <T, R : Number> PivotGroupBy<T>.mean(skipNA: Boolean = skipNA_default, columns: ColumnsSelector<T, R?>): DataFrame<T> =
    Aggregators.mean(skipNA).aggregateAll(this, columns)
public fun <T> PivotGroupBy<T>.mean(vararg columns: String, skipNA: Boolean = skipNA_default): DataFrame<T> = mean(skipNA) { columns.toColumnsOf() }
public fun <T, R : Number> PivotGroupBy<T>.mean(vararg columns: ColumnReference<R?>, skipNA: Boolean = skipNA_default): DataFrame<T> = mean(skipNA) { columns.toColumns() }
public fun <T, R : Number> PivotGroupBy<T>.mean(vararg columns: KProperty<R?>, skipNA: Boolean = skipNA_default): DataFrame<T> = mean(skipNA) { columns.toColumns() }

public inline fun <T, reified R : Number> PivotGroupBy<T>.meanOf(
    skipNA: Boolean = skipNA_default,
    crossinline expression: RowExpression<T, R?>
): DataFrame<T> =
    Aggregators.mean(skipNA).aggregateOf(this, expression)

// endregion

// region median

public fun <T> PivotGroupBy<T>.median(separate: Boolean = false): DataFrame<T> = medianFor(separate, comparableColumns())

public fun <T, C : Comparable<C>> PivotGroupBy<T>.medianFor(
    separate: Boolean = false,
    columns: ColumnsForAggregateSelector<T, C?>
): DataFrame<T> = Aggregators.median.aggregateFor(this, separate, columns)
public fun <T> PivotGroupBy<T>.medianFor(vararg columns: String, separate: Boolean = false): DataFrame<T> = medianFor(separate) { columns.toComparableColumns() }
public fun <T, C : Comparable<C>> PivotGroupBy<T>.medianFor(
    vararg columns: ColumnReference<C?>,
    separate: Boolean = false
): DataFrame<T> = medianFor(separate) { columns.toColumns() }
public fun <T, C : Comparable<C>> PivotGroupBy<T>.medianFor(
    vararg columns: KProperty<C?>,
    separate: Boolean = false
): DataFrame<T> = medianFor(separate) { columns.toColumns() }

public fun <T, C : Comparable<C>> PivotGroupBy<T>.median(columns: ColumnsSelector<T, C?>): DataFrame<T> = Aggregators.median.aggregateAll(this, columns)
public fun <T> PivotGroupBy<T>.median(vararg columns: String): DataFrame<T> = median { columns.toComparableColumns() }
public fun <T, C : Comparable<C>> PivotGroupBy<T>.median(
    vararg columns: ColumnReference<C?>
): DataFrame<T> = median { columns.toColumns() }
public fun <T, C : Comparable<C>> PivotGroupBy<T>.median(vararg columns: KProperty<C?>): DataFrame<T> = median { columns.toColumns() }

public inline fun <T, reified R : Comparable<R>> PivotGroupBy<T>.medianOf(
    crossinline expression: RowExpression<T, R?>
): DataFrame<T> = Aggregators.median.aggregateOf(this, expression)

// endregion

// region std

public fun <T> PivotGroupBy<T>.std(
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default
): DataFrame<T> = stdFor(separate, skipNA, ddof, numberColumns())

public fun <T, R : Number> PivotGroupBy<T>.stdFor(
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default,
    columns: ColumnsForAggregateSelector<T, R?>
): DataFrame<T> =
    Aggregators.std(skipNA, ddof).aggregateFor(this, separate, columns)
public fun <T> PivotGroupBy<T>.stdFor(
    vararg columns: String,
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default
): DataFrame<T> = stdFor(separate, skipNA, ddof) { columns.toColumnsOf() }
public fun <T, C : Number> PivotGroupBy<T>.stdFor(
    vararg columns: ColumnReference<C?>,
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default
): DataFrame<T> = stdFor(separate, skipNA, ddof) { columns.toColumns() }
public fun <T, C : Number> PivotGroupBy<T>.stdFor(
    vararg columns: KProperty<C?>,
    separate: Boolean = false,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default
): DataFrame<T> = stdFor(separate, skipNA, ddof) { columns.toColumns() }

public fun <T> PivotGroupBy<T>.std(
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default,
    columns: ColumnsSelector<T, Number?>
): DataFrame<T> = Aggregators.std(skipNA, ddof).aggregateAll(this, columns)
public fun <T> PivotGroupBy<T>.std(
    vararg columns: ColumnReference<Number?>,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default
): DataFrame<T> = std(skipNA, ddof) { columns.toColumns() }
public fun <T> PivotGroupBy<T>.std(vararg columns: String, skipNA: Boolean = skipNA_default, ddof: Int = ddof_default): DataFrame<T> = std(skipNA, ddof) { columns.toColumnsOf() }
public fun <T> PivotGroupBy<T>.std(
    vararg columns: KProperty<Number?>,
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default
): DataFrame<T> = std(skipNA, ddof) { columns.toColumns() }

public inline fun <T, reified R : Number> PivotGroupBy<T>.stdOf(
    skipNA: Boolean = skipNA_default,
    ddof: Int = ddof_default,
    crossinline expression: RowExpression<T, R?>
): DataFrame<T> = Aggregators.std(skipNA, ddof).aggregateOf(this, expression)

// endregion
