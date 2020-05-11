package io.kotest.matchers

import io.kotest.core.spec.style.StringSpec

class ShouldKtTest: StringSpec ({
   "diff should show differences" {
      data class Foo(val a: String, val b: String, val c: String){
      }
      Foo("a", "b", "c") shouldBe Foo("a", "a", "a")
   }
})
