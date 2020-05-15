package com.sksamuel.kotest.matchers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equality.shouldBeEqualToUsingFields
import io.kotest.matchers.shouldBe
import java.util.UUID
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

open class Bar(val z: String) {
}

data class Foo(val a: Long, val b: String, val c: String): Bar("$a$c" + UUID.randomUUID()) {
   val d: String = "$a$b$c"
   var other: Int? = null

   companion  object {
      val x = 123L
   }

   constructor(a: Long, b: String, c: String, other: Int): this(a,b,c) {
      this.other = other
   }
}

data class BooFoo(val foo: Foo, val a: Long)


class DataClassMatchersTest : StringSpec({
   "highlight differences in data classes" {

      val actual = Foo(123L, "b", "c")
      val expected = Foo(123L, "a", "a")

      val actualClass = actual::class
      if (actualClass.isData) {
         println("mem prop" + actualClass::memberProperties.get())
         println("dec mem prop" + actualClass::declaredMemberProperties.get())

         if (actualClass::memberProperties.get() == actualClass::declaredMemberProperties.get())
            println("mem props == dec mem props")
         else
            println("mem props != dec mem props")

         val cons =  actualClass::constructors.get()
         println(cons.last())
         println(cons.last().typeParameters) // empty
         println(cons.last().parameters.forEach { kParameter: KParameter ->
            println(kParameter)
//            kParameter.
         })

         val dataClassConstructorMembers = cons.last().parameters
         val fooclass = Foo::class

         val propsInEqualsBad = Foo::class.declaredMemberProperties.filter{memberKProperty ->
            dataClassConstructorMembers.any{ consKParam -> consKParam.name == memberKProperty.name }
         }

         val propsInEquals = actualClass.declaredMemberProperties.filter{memberKProperty ->
            dataClassConstructorMembers.any{ consKParam -> consKParam.name == memberKProperty.name }
         }

         println(propsInEquals)
         // https://discuss.kotlinlang.org/t/type-projection-clash-when-accessing-property-delegate-instance/8331
         // https://youtrack.jetbrains.com/issue/KT-16432?_ga=2.265298440.640424854.1589134567-1685779670.1523969764
         val nonMatchers = propsInEquals.filter {prop ->
            val nuProp = (prop as KProperty1<Any, *>)
            prop(actual) != prop(expected)
         }

         println(nonMatchers)
         nonMatchers.forEach{
            val fieldName = it.name.split('.').last()
            val nuProp = (it as KProperty1<Any, *>)
            println("Diff in field $fieldName: " + nuProp(actual) + " did not equal " + nuProp(expected))
         }
      }

      actual shouldBe Foo(123L, "a", "a")
   }

   "Test out data class diff" {
      Foo(321L, "b", "c", 123) shouldBe Foo(123L, "a", "a")
   }

   "Test out data class diff nester" {
      BooFoo(Foo(321L, "b", "c", 123),8) shouldBe BooFoo(Foo(123L, "a", "a"), 9)
   }
})
