package io.kotest.property.internal

import io.kotest.assertions.show.show
import io.kotest.property.PropertyTesting
import io.kotest.property.RTree
import io.kotest.property.ShrinkingMode
import io.kotest.property.isEmpty
import kotlin.time.ExperimentalTime

/**
 * Accepts a value of type A and a function that varies in type A (fixed in any other types) and attempts
 * to shrink the value to find the smallest failing case.
 *
 * For each step in the shrinker, we test all the values. If they all pass then the shrinking ends.
 * Otherwise, the next batch is taken and the shrinks continue.
 *
 * Once all values from a shrink step pass, we return the previous value as the "smallest" failing case.
 */
@OptIn(ExperimentalTime::class)
internal suspend fun <A> doShrinking(
   initial: RTree<A>,
   mode: ShrinkingMode,
   test: suspend (A) -> Unit
): Pair<Throwable?, A> {

   if (initial.isEmpty())
      return (null to initial.value())

   val counter = Counter()
   val tested = mutableSetOf<A>()
   val sb = StringBuilder()
   sb.append("Attempting to shrink failed arg ${initial.value.show().value}\n")

   val candidate = doStep(initial, mode, tested, counter, test, sb)

   result(sb, candidate as Any, counter.count)
   return when (candidate) {
      is StepResult.CompletedShrinking -> Pair(null, initial.value())
      is StepResult.ExceptionalResult -> Pair(candidate.t, candidate.shrunkValue)
   }
}

class Counter {
   var count = 0
   fun inc() = count++
}


sealed class  StepResult<out A> {
   object CompletedShrinking: StepResult<Nothing>()
   data class ExceptionalResult<A>(val shrunkValue: A, val t: Throwable) : StepResult<A>()
}
/**
 * Performs shrinking on the given RTree. Recurses into the tree for failing cases.
 */
suspend fun <A> doStep(
   tree: RTree<A>,
   mode: ShrinkingMode,
   tested: MutableSet<A>,
   counter: Counter,
   test: suspend (A) -> Unit,
   sb: StringBuilder
): StepResult<A> {

   if (!mode.isShrinking(counter.count)) return StepResult.CompletedShrinking
   val candidates = tree.children.value

   candidates.asSequence()
      .filter { tested.add(it.value()) }
      .forEach { a ->
         counter.inc()
         try {
            test(a.value())
            if (PropertyTesting.shouldPrintShrinkSteps)
               sb.append("Shrink #${counter.count}: ${a.show().value} pass\n")
         } catch (t: Throwable) {
            if (PropertyTesting.shouldPrintShrinkSteps)
               sb.append("Shrink #${counter.count}: ${a.show().value} fail\n")
            // this result failed, so we'll recurse in to find further failures otherwise return this
            val nextShrink = doStep(a, mode, tested, counter, test, sb)
            return if (nextShrink == StepResult.CompletedShrinking) {
               StepResult.ExceptionalResult(a.value(), t)
            } else {
               nextShrink
            }
         }
      }

   return StepResult.CompletedShrinking
}

/**
 * Returns true if we should continue shrinking given the count.
 */
private fun ShrinkingMode.isShrinking(count: Int): Boolean = when (this) {
   ShrinkingMode.Off -> false
   ShrinkingMode.Unbounded -> true
   is ShrinkingMode.Bounded -> count < bound
}

private fun result(sb: StringBuilder, candidate: Any, count: Int) {
   when (count) {
      0 -> sb.append("Shrink result => ${candidate.show().value}\n")
      else -> sb.append("Shrink result (after $count shrinks) => ${candidate.show().value}\n")
   }
   if (PropertyTesting.shouldPrintShrinkSteps) {
      println(sb)
   }
}
