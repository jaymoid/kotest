package io.kotest.assertions.eq

import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.failure
import io.kotest.assertions.show.show
import io.kotest.matchers.shouldBe
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty1

internal fun Any?.isDataClass(): Boolean = this != null && this::class.isData

internal object DataClassEq : Eq<Any> {

   private const val dataClassDiffText = "data class diff for "

   override fun equals(actual: Any, expected: Any): Throwable? {
      return if (test(actual, expected)) null else {
         failure(
            Expected(expected.show()),
            Actual(actual.show()),
            formatDifferences(dataClassDiff(actual, expected)) + "\n\n"
         )
      }
   }

   private fun formatDifferences(dataClassDifference: DataClassDifference, depth: Int = 0): String {
      val noOfDifferences = dataClassDifference.differences.size
      return dataClassDifference.differences.mapIndexed { index, (property, difference) ->
         val propertyName = property.name.split('.').last()
         val isLastChar = index + 1 == noOfDifferences
         val prefixStr = "│  ".repeat(depth) + if (isLastChar) '└' else '├'
         val diffMsg = when (difference) {
            is StandardDifference -> difference.differenceError.message
            is DataClassDifference -> formatDifferences(difference,depth+1)
         }
         "$prefixStr $propertyName: $diffMsg"
      }.joinToString(separator = "\n", prefix = dataClassDiffText + dataClassDifference.dataClassName + "\n")
   }

   private fun dataClassDiff(actual: Any?, expected: Any?): DataClassDifference {
      require(actual != null && expected != null) { "Actual and expected values cannot be null in a data class comparison"}

         val expectedClass = expected::class
         val dataClassConstructorMembers = expectedClass::constructors.get().last().parameters

         val members = expectedClass::members.get().associateBy(KCallable<*>::name)
         val propsInEquals = dataClassConstructorMembers.map { members[it.name] }

         val differences: List<Pair<KProperty1<Any, *>, Difference>> = propsInEquals.map { prop ->
            // https://discuss.kotlinlang.org/t/type-projection-clash-when-accessing-property-delegate-instance/8331
            // https://youtrack.jetbrains.com/issue/KT-16432?_ga=2.265298440.640424854.1589134567-1685779670.1523969764
            val property = (prop as KProperty1<Any, *>)
            val actualPropertyValue = property(actual)
            val expectedPropertyValue = property(expected)
            when {
               actualPropertyValue.isDataClass() && expectedPropertyValue.isDataClass() ->
                   Pair(property, dataClassDiff(actualPropertyValue, expectedPropertyValue))
               else ->
                  runCatching { actualPropertyValue shouldBe expectedPropertyValue }
                     .fold(onSuccess = { null }, onFailure = {t -> Pair(property, StandardDifference(t)) })
            }
         }.filterNotNull()

         return DataClassDifference(expectedClass.simpleName ?: "dunno classname", differences )
   }

   private fun test(a: Any?, b: Any?): Boolean {
      return makeComparable(a) == makeComparable(b)
   }
}

sealed class Difference
data class StandardDifference( val differenceError: Throwable) : Difference()
data class DataClassDifference(val dataClassName: String,
                               val differences: List<Pair<KProperty1<Any, *>, Difference>>) : Difference()
