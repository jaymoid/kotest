package io.kotest.assertions.eq

import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.failure
import io.kotest.assertions.show.show
import io.kotest.matchers.shouldBe
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty1

internal object DataClassEq : Eq<Any> {

   private const val dataClassDiffText = "data class diff for "

   override fun equals(actual: Any, expected: Any): Throwable? {
      return if (test(actual, expected)) null else {
         failure(Expected(expected.show()), Actual(actual.show()), dataClassDiff(actual, expected))
      }
   }

   private fun dataClassDiff(actual: Any?, expected: Any?): String =
      if (actual != null && expected != null) {
         val actualClass = actual::class
         val dataClassConstructorMembers = actualClass::constructors.get().last().parameters

         val members = actualClass::members.get().associateBy(KCallable<*>::name)
         val propsInEquals = dataClassConstructorMembers.map { members[it.name] }

         val filtered = propsInEquals.map { prop ->
            // https://discuss.kotlinlang.org/t/type-projection-clash-when-accessing-property-delegate-instance/8331
            // https://youtrack.jetbrains.com/issue/KT-16432?_ga=2.265298440.640424854.1589134567-1685779670.1523969764
            val nuProp = (prop as KProperty1<Any, *>)
            val failMsg: String? = runCatching { nuProp(actual) shouldBe nuProp(expected) }
               .fold(onSuccess = { null }, onFailure = { it.message })
            Pair(nuProp, failMsg)
         }.filter{ it.second != null }

         val filteredSize = filtered.size
         filtered.mapIndexed {index, (property, failMsg) ->
            val fieldName = property.name.split('.').last()
            val isLastChar = (index +1) == filteredSize
            val startChar = if (isLastChar) '└' else '├'
            "$startChar $fieldName: ${formatDataClassDiff(failMsg, isLastChar)}"
         }.joinToString(separator = "\n", prefix = dataClassDiffText + actualClass.qualifiedName + "\n", postfix = "\n\n")
      } else {
         ""
      }

   private fun formatDataClassDiff(failMsg: String?, isLast: Boolean): String {
      val startChar = if (isLast) ' ' else '│'
      return failMsg?.run {
         if (startsWith(dataClassDiffText)) {
            lines()
               .mapIndexed { lineNo, line -> if (lineNo == 0) line else "$startChar  " + line }
               .dropLast(2)
               .joinToString(separator = "\n", prefix = "")
         } else this
      } ?: ""
   }

   private fun test(a: Any?, b: Any?): Boolean {
      return makeComparable(a) == makeComparable(b)
   }
}
