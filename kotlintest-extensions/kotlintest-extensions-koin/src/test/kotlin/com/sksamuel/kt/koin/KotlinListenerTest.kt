package com.sksamuel.kt.koin

import io.kotlintest.*
import io.kotlintest.koin.KoinListener
import io.kotlintest.specs.FunSpec
import org.koin.core.context.GlobalContext
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.mock.declareMock
import org.mockito.BDDMockito

class KotlinListenerTest : FunSpec(), KoinTest {

  override fun listeners() = listOf(KoinListener(koinModule))

  private val genericService by inject<GenericService>()

  init {
    test("Should have autowired the service correctly") {
      genericService.foo() shouldBe "Bar"
    }

    test("Should allow mocking correctly") {
      declareMock<GenericRepository> {
        BDDMockito.given(foo()).willReturn("DootyDoot")
      }

      genericService.foo() shouldBe "DootyDoot"
    }
  }

  override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
    GlobalContext.getOrNull() shouldBe null     // We should finish koin after test execution
  }

  override fun isolationMode() = IsolationMode.InstancePerTest
}