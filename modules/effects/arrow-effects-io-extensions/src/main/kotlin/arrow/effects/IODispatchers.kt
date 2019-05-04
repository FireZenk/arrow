package arrow.effects

import arrow.undocumented
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

@undocumented
// FIXME use expected and actual for multiplatform
object IODispatchers {
  // FIXME use ForkJoinPool.commonPool() in Java 8
  val CommonPool: CoroutineContext = Pool(ForkJoinPool())
}

fun ExecutorService.asCoroutineContext(): CoroutineContext = Pool(this)

private class Pool(val pool: ExecutorService) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
  override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
    ExecutorServiceContinuation(pool, continuation.context.fold(continuation) { cont, element ->
      if (element != this@Pool && element is ContinuationInterceptor)
        element.interceptContinuation(cont) else cont
    })
}

private class ExecutorServiceContinuation<T>(
  val pool: ExecutorService,
  val cont: Continuation<T>
) : Continuation<T> {
  override val context: CoroutineContext = cont.context

  override fun resumeWith(result: Result<T>) {
    pool.execute { cont.resumeWith(result) }
  }
}
