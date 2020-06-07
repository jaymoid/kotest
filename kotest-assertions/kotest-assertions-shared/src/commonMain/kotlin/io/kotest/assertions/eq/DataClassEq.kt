package io.kotest.assertions.eq

import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.failure
import io.kotest.assertions.show.show
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal val <T : Any> KClass<T>.dataClassProperties: List<KCallable<*>>
   get() {
      val dataClassConstructorParams = ::constructors.get().last().parameters
      val membersByName = ::members.get().associateBy(KCallable<*>::name)
      return dataClassConstructorParams.map { membersByName[it.name] }.filterNotNull()
   }

internal object DataClassEq : Eq<Any> {

   override fun equals(actual: Any, expected: Any): Throwable? =
      if (test(actual, expected))
         null
      else
         failure(
            Expected(expected.show()),
            Actual(actual.show()),
            dataClassDiff(actual, expected)?.let { diff -> formatDifferences(diff) + "\n\n" } ?: ""
         )

   private fun test(a: Any?, b: Any?): Boolean = makeComparable(a) == makeComparable(b)

   private fun formatDifferences(dataClassDiff: DataClassDifference, depth: Int = 0): String {
      val noOfDifferences = dataClassDiff.differences.size
      return dataClassDiff.differences.mapIndexed { index, (property, difference) ->
         val diffMsg = when (difference) {
            is StandardDifference -> difference.differenceError.message
            is DataClassDifference -> formatDifferences(difference, depth + 1)
         }
         buildString {
            append("│  ".repeat(depth))
            append(if (index + 1 == noOfDifferences) '└' else '├')
            append(" ${property.name}: $diffMsg")
         }
      }.joinToString(separator = "\n", prefix = "data class diff for ${dataClassDiff.dataClassName}\n")
   }

   private fun dataClassDiff(actual: Any?, expected: Any?): DataClassDifference? {
      require(actual != null && expected != null) { "Actual and expected values cannot be null in a data class comparison" }
      val dataClassProperties = expected::class.dataClassProperties
      val differences = computeDifferences(dataClassProperties, actual, expected)
      return if (differences.isEmpty()) null else DataClassDifference(
         expected::class.qualifiedName ?: "unknown classname",
         differences
      )
   }

   private fun computeDifferences(
      propertiesInEquals: List<KCallable<*>>,
      actual: Any,
      expected: Any
   ): List<Pair<KProperty1<Any, *>, PropertyDifference>> =
      propertiesInEquals.map { prop ->
         // https://discuss.kotlinlang.org/t/type-projection-clash-when-accessing-property-delegate-instance/8331
         // https://youtrack.jetbrains.com/issue/KT-16432?_ga=2.265298440.640424854.1589134567-1685779670.1523969764
         val property = (prop as KProperty1<Any, *>)
         val actualPropertyValue = property(actual)
         val expectedPropertyValue = property(expected)
         if (actualPropertyValue.isDataClass() && expectedPropertyValue.isDataClass())
            dataClassDiff(actualPropertyValue, expectedPropertyValue)?.let { diff -> Pair(property, diff) }
         else
            eq(actualPropertyValue, expectedPropertyValue)?.let { t -> Pair(property, StandardDifference(t)) }

      }.filterNotNull()
}

sealed class PropertyDifference
data class StandardDifference(val differenceError: Throwable) : PropertyDifference()
data class DataClassDifference(
   val dataClassName: String,
   val differences: List<Pair<KProperty1<Any, *>, PropertyDifference>>
) : PropertyDifference()