package io.kotest.property.internal

import io.kotest.assertions.failure
import io.kotest.assertions.show.show
import io.kotest.property.ExceptionReport
import io.kotest.property.PropTestConfig

/**
 * Generates an [AssertionError] for a property test without arg details and then throws it.
 */
internal fun throwPropertyTestAssertionError(
   e: Throwable, // the underlying failure reason,
   attempts: Int,
   seed: Long
): Unit = throw propertyAssertionError(e, attempts, seed, emptyList())

/**
 * Generates an [AssertionError] for a property test with arg details and then throws it.
 *
 * @param values the failed values
 * @param shrinks the reduced (shrunk) values
 * @param e the underlying failure reason
 * @param attempts the iteration count at the time of failure
 */
internal fun throwPropertyTestAssertionError(
   values: List<Any?>,
   shrinks: Pair<Throwable?,List<Any?>>,
   e: Throwable,
   attempts: Int,
   seed: Long,
   config: PropTestConfig
) {

   val reportedEx = when (config.reportedException) {
      ExceptionReport.FIRST -> e
      ExceptionReport.SHRUNK -> shrinks.first ?: RuntimeException("No shrunken exception, fix your types James!")
   }
   val failedInputs = when (config.reportedException) {
      ExceptionReport.FIRST -> values.zip(shrinks.second).map {(orig, _) -> PropertyFailureInput(orig, orig) }
      ExceptionReport.SHRUNK -> values.zip(shrinks.second).map {(orig, shrunk) -> PropertyFailureInput(orig, shrunk) }
   }
   throw propertyAssertionError(reportedEx, attempts, seed, failedInputs)
}

/**
 * Maps a failed property test arg to its shrunk value if any.
 */
data class PropertyFailureInput<T>(val original: T?, val shrunk: T?)

/**
 * Generates an [AssertionError] for a failed property test.
 *
 * @param e the test failure cause
 * @param attempt the iteration count at the time of failure
 * @param inputs the inputs that the test failed for
 */
internal fun propertyAssertionError(
   e: Throwable,
   attempt: Int,
   seed: Long,
   inputs: List<PropertyFailureInput<out Any?>>
): Throwable {
   return failure(propertyTestFailureMessage(attempt, inputs, seed, e), e)
}

/**
 * Generates a property test failure message with details of the args that failed, any shrinks
 * that took place, and the exception throw by the failing test.
 */
internal fun propertyTestFailureMessage(
   attempt: Int,
   inputs: List<PropertyFailureInput<out Any?>>,
   seed: Long,
   cause: Throwable
): String {
   val sb = StringBuilder()
   sb.append("Property failed after $attempt attempts\n")
   if (inputs.isNotEmpty()) {
      sb.append("\n")
      inputs.withIndex().forEach {
         val input = if (it.value.shrunk == it.value.original) {
            "\tArg ${it.index}: ${it.value.shrunk.show().value}"
         } else {
            "\tArg ${it.index}: ${it.value.shrunk.show().value} (shrunk from ${it.value.original})"
         }
         sb.append(input)
         sb.append("\n")
      }
   }
   sb.append("\n")

   // don't bother to include the exception type if it's AssertionError
   val causedBy = when (cause::class.simpleName) {
      "AssertionError" -> "Caused by: ${cause.message?.trim()}"
      else -> "Caused by ${cause::class.simpleName}: ${cause.message?.trim()}"
   }
   sb.append(causedBy)
   sb.append("\n\n")
   sb.append("Repeat this test by using seed $seed\n")
   return sb.toString()
}
