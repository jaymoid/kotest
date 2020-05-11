package com.sksamuel.kotest.property.shrinkreport

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.ExceptionReport
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll

class ShrinkReportTest: StringSpec({

   "Should include both shrunk and preshrunk" {
      checkAll(PropTestConfig(seed = 1,reportedException = ExceptionReport.FIRST ), Arb.int(10..20)) { i ->
         somefun(i) shouldBe true
      }
   }

   "Some other test" {
      assertSoftly{
         'a' shouldBe 'b'
         'b' shouldBe 'c'
      }
   }
})


fun somefun(i:Int) =
   if (i==0)
      otherFun()
   else
      i == 8

fun otherFun() =
   true
