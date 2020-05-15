package io.kotest.assertions.eq

import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.failure
import io.kotest.assertions.show.show
import kotlin.reflect.KProperty1

internal object DataClassEq : Eq<Any> {
   override fun equals(actual: Any, expected: Any): Throwable? {
      return if (test(actual, expected)) null else {
         failure(Expected(expected.show()), Actual(actual.show()), dataClassDiff(actual, expected))
      }
   }

   private fun dataClassDiff(actual: Any?, expected: Any?): String =
      if (actual != null && expected != null) {
         val actualClass = actual::class
         val dataClassConstructorMembers = actualClass::constructors.get().last().parameters
         val propsInEquals = actualClass::members.get().filter { memberKProperty ->
            dataClassConstructorMembers.any { consKParam -> consKParam.name == memberKProperty.name }
         }

         // https://discuss.kotlinlang.org/t/type-projection-clash-when-accessing-property-delegate-instance/8331
         // https://youtrack.jetbrains.com/issue/KT-16432?_ga=2.265298440.640424854.1589134567-1685779670.1523969764
         propsInEquals.filter {prop ->
            val nuProp = (prop as KProperty1<Any, *>)
            nuProp(actual) != nuProp(expected)
         }.flatMap{
            val fieldName = it.name.split('.').last()
            val nuProp = (it as KProperty1<Any, *>)
            listOf(" - field $fieldName: " + nuProp(actual).show().value + " did not equal " + nuProp(expected).show().value )
         }.joinToString(separator = "\n", prefix = "Diff in ${actualClass.simpleName}:\n", postfix = "\n")
      } else {
         ""
      }

   private fun test(a: Any?, b: Any?): Boolean {
      return makeComparable(a) == makeComparable(b)
   }
}
