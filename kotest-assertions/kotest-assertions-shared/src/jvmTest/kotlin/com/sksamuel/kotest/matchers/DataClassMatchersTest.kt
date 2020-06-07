package com.sksamuel.kotest.matchers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

open class Bar(val z: String) {
}

data class Baz(val i: Int, val booFoo: BooFoo, val things: List<Any>)

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

      actual shouldBe expected
   }

   "Test out data class diff" {
      Foo(321L, "b", "c", 123) shouldBe Foo(123L, "a", "a")
   }

   "Test out data class diff nester" {
      BooFoo(Foo(321L, "b", "c", 123),8) shouldBe BooFoo(Foo(123L, "a", "a"), 9)
   }

   "Triple nest" {
      Baz(0x43, BooFoo(Foo(321L, "b", "c", 123),8), listOf("egg", 1)) shouldBe Baz(0x42,BooFoo(Foo(123L, "a", "a"), 9), listOf("cheese", 22))
   }

   "override test" {
     val first = OverriddenEquals(1,2)
     val second = OverriddenEquals(1,2)
      first shouldBe second
   }

   "inner does not differ" {
      BooFoo(Foo(123L, "a", "a"),8) shouldBe BooFoo(Foo(123L, "a", "a"), 9)
   }
})


data class OverriddenEquals(val a: Int, val b: Int) {
   override fun equals(other: Any?): Boolean =
      if (other is OverriddenEquals)
         this.a == other.b
      else
         false

}
