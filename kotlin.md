# 约定
    suspending function 挂起函数
    continuation object 延续对象
    continuation 延续
    resume 恢复
    dispatcher 分发器
    
# [Why using Kotlin Coroutines?](https://kt.academy/article/cc-why)
为什么我们需要学习Kotlin协程呢？我们已经有像RxJava和ReActor这样完善的JVM库，此外Java自身也支持多线程，尽管很多人
还是使用普通的旧回调。显而易见，在执行异步操作时我们已经有了很多选择。
Kotlin协程是多平台的，这意味着它可以跨所有Kotlin平台（JVM、JS、iOS）使用。
在Android平台上，使用Kotlin协程，使线程切换更方便。
```
fun onCreate() {
  thread {
      val news = getNewsFromApi()
      val sortedNews = news
          .sortedByDescending { it.publishedAt }
      runOnUiThread {
          view.showNews(sortedNews)
      }
  }
}
```
线程切换的风险

    1、没有机制来取消这些线程，可能面临内存泄漏
    
    2、创建线程的开销是很昂贵的
    
    3、频繁切换线程是很让人困惑的，并且难以管理
    
    4、代码不可避免的膨胀且复杂
    
基于上面的问题，使用Callback是一种解决方法。

```
fun onCreate() {
  getNewsFromApi { news ->
      val sortedNews = news
          .sortedByDescending { it.publishedAt }
      view.showNews(sortedNews)
  }
}
```
或者使用RxJava或其他reactive streams
```
fun onCreate() {
  disposables += getNewsFromApi()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .map { news ->
          news.sortedByDescending { it.publishedAt }
      }
      .subscribe { sortedNews ->
          view.showNews(sortedNews)
      }
}
```
但Callback会带来回调地狱，RxJava的代码看起来不够简洁，有很多方法要学习。

Kotlin协程介绍的它的核心功能是，它有在某一执行点挂起协程并在未来恢复的能力。
```
fun showNews() {
  viewModelScope.launch {
      val config = async { getConfigFromApi() }
      val news = async { getNewsFromApi(config.await()) }
      val user = async { getUserFromApi() }
      view.showNews(user.await(), news.await())
  }
}
```

# [How does suspension work in Kotlin coroutines?](https://kt.academy/article/cc-suspension)

挂起函数是Kotlin协程的特点，挂起能力是Kotlin协程概念构建的最基本特征。

协程挂起意味着在中间停止。与暂停视频游戏类似：你存档关闭游戏，然后你和电脑可以关注一些其他不同的事情。当你过段时间以后
想继续的话，你打开游戏，读取存档，然后你可以从上次离开的位置继续游戏。这是一个协程的类比。当协程挂起时，它返回一个延续
（Continuation），我们可以使用它来在暂停点继续，这和游戏存档有点像。

注意这和线程不同，线程只有阻塞，不能保存。协程更强大，当挂起时，它不消费任何资源。协程可以在不同的线程中恢复，理论上
一个延续可以被序列化、反序列化后恢复。

恢复：

挂起函数是可以挂起协程的函数，这意味着它们必须在协程中或者其他挂起函数中调用。最后它们一起事情来挂起。main是启动点，
我们运行它时Kotlin将在一个协程中启动它。
```
suspend fun main() {
    println("Before")

    println("After")
}
// Before
// After
```
上面会打印出Before和After，如果我们在两条打印语句中间挂起，那么会怎么样呢？为了测试这一点，我们可以使用Kotlin标准库
提供的suspendCoroutine函数。
```
suspend fun main() {
    println("Before")

    suspendCoroutine<Unit> { }

    println("After")
}
// Before
```
执行上面代码，你将看不到After打印出来，main函数永远不会停止运行。协程在Before打印后挂起，我们的游戏被暂停且不会恢复，
所以，我们怎样才能可以恢复呢？我们之前提到的延续跑哪儿了呢？

看下suspendCoroutine的调用，注意到它以一个lambda表达式结束。作为参数传递的函数将在挂起之前被调用，这个函数获得一个
延续作为参数。
```
suspend fun main() {
    println("Before")

    suspendCoroutine<Unit> { continuation ->
        println("Before too")
    }

    println("After")
}
// Before
// Before too
```
这种函数调用另一函数不是什么新鲜事，像let、apply和useLines等类似。suspendCoroutine也是如此设计，这使它可以在挂起
之前使用延续。在suspendCoroutine调用后，将会太迟了。因此，作为参数传递给suspendCoroutine函数的lambda表达式，仅
在挂起前被调用。lambda被用来保存延续或计划是否回复它。

我们可以使用延续来立即恢复：
```
suspend fun main() {
    println("Before")

    suspendCoroutine<Unit> { continuation ->
        continuation.resume(Unit)
    }

    println("After")
}
// Before
// After
```
注意After被打印出来了，因为我们在suspendCoroutine中调用了resume。

注：
自Kotlin 1.3以后，Continuation的定义改变了。仅有一个带Result参数的resumeWith函数， resume和resumeWithException
函数被移除作为标准库中的扩展函数。

我们可以在协程中启动一个不同的线程，在线程休眠一段时间后，再恢复。
```
suspend fun main() {
    println("Before")

    suspendCoroutine<Unit> { continuation ->
        thread {
            println("Suspended")
            Thread.sleep(1000)
            continuation.resume(Unit)
            println("Resumed")
        }
    }

    println("After")
}
// Before
// Suspended
// (1 second delay)
// After
// Resumed
```
需要强调的是，挂起函数不是协程，只是可以挂起协程的函数。

# [Coroutines under the hood](https://kt.academy/article/cc-under-the-hood#definition-2)

1、挂起函数就像状态机，在函数的开始和每次挂起函数调用后，有一个可能的状态

2、标识状态的数值和局部数据，都保存在一个延续对象中

3、一个函数的延续装饰一个它的调用者的延续，因此，所有这些延续代表了一个调用栈，这个调用栈用于在我们恢复或者一个恢复函数执行完成时

# CPS (Continuation-passing style)

CPS是挂起函数的实现方式，这意味着延续作为参数从一个函数传递到另一个函数中。按照惯例，延续作为函数的最后一个参数。
```
suspend fun getUser(): User?
suspend fun setUser(user: User)
suspend fun checkAvailability(flight: Flight): Boolean

// under the hood is
fun getUser(continuation: Continuation<*>): Any?
fun setUser(user: User, continuation: Continuation<*>): Any
fun checkAvailability(
  flight: Flight,
  continuation: Continuation<*>
): Any
```
你可能已经注意到底层的函数的返回值和定义的不一样，这是因为挂起函数可能挂起，或者返回声明的类型。挂起时返回一个
COROUTINE_SUSPENDED标志

```
val continuation = continuation as? MyFunctionContinuation ?: MyFunctionContinuation(continuation)
```
函数为了保存它的状态需要它自己的延续，我们命名为MyFunctionContinuation，在函数体开始，myFunction将用它自己的延续
（MyFunctionContinuation）封装一下continuation。仅当continuation未封装时处理。

```
suspend fun myFunction() {
  println("Before")
  delay(1000) // suspending
  println("After")
}

fun myFunction(continuation: Continuation<*>): Any
```
函数可能在两个地方启动：开始（第一次调用）或者挂起后（从延续中恢复）。为区分状态，我们添加一个label字段，起始被设置为0，
它在每个挂起点前被设置为下一个状态，这样我们可以在恢复时从挂起点后开始。
```
// A simplified picture of how myFunction looks under the hood
fun myFunction(continuation: Continuation<Unit>): Any {
    val continuation = continuation as? MyFunctionContinuation ?: MyFunctionContinuation(continuation)

    if (continuation.label == 0) {
        println("Before")
        continuation.label = 1
        // label在每一个挂起点之前被设置为下一个状态
        if (delay(1000, continuation) == COROUTINE_SUSPENDED){
            return COROUTINE_SUSPENDED
        }
    }
    if (continuation.label == 1) {
        println("After")
        return Unit
    }
    error("Impossible")
}
```
之前提到的重点在上面代码中出现了，delay挂起时，返回COROUTINE_SUSPENDED，然后myFunction返回COROUTINE_SUSPENDED，
同样调用myFunction的也返回COROUTINE_SUSPENDED，直至到调用栈的栈顶。这就是挂起如何结束这些函数，并留下线程为其他runnable
可用。

现在让我们来看看continuation，以匿名对象的方式实现。简单来说，像以下代码：
```
cont = object : ContinuationImpl(continuation) {
    var result: Any? = null
    var label = 0

    override fun invokeSuspend(`$result$: Any?): Any? {
        this.result = $result`;
        return myFunction(this);
    }
};
```
为了提高可读性，我们把它命名为一个MyFunctionContinuation类。
```
class MyFunctionContinuation(val completion: Continuation<Unit>) : Continuation<Unit> {
    override val context: CoroutineContext
    get() = completion.context

    var label = 0
    var result: Result<Any>? = null

    override fun resumeWith(result: Result<Unit>) {
        this.result = result
        val res = try {
            val r = myFunction(this)
            if (r == COROUTINE_SUSPENDED) return
            Result.success(r as Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
        completion.resumeWith(res)
    }
}
```
调用栈

挂起时会释放线程，调用栈因此也就清空了。延续扮演了调用栈的角色，每个延续保留状态、局部变量、参数以及调用该函数的延续的引用，
一个延续引用另一个延续。

# [What is CoroutineContext and how does it work?](https://kt.academy/article/cc-coroutine-context)
```
public interface CoroutineScope {
    public val coroutineContext: CoroutineContext
}
public interface Continuation<in T> {
    public val context: CoroutineContext
    public fun resumeWith(result: Result<T>)
}
```
协程作用域和延续中都有CoroutineContext，那么这个CoroutineContext是什么呢？

CoroutineContext是一个代表元素或元素集合的接口，概念上有点类似map或set集合：它是一个像Job、CoroutineName以及
CouroutineDispatcher等Element实例的索引集合（indexed set），独特的是每一个Element也是一个CoroutineContext，
因此，集合中的每一个元素本身也是一个集合。

这个概念很好理解，例如一个马克杯，它是一个元素，同时也是一个仅有一个元素的集合。当你添加另一个马克杯时，你有一个包含两个
元素的集合。

为了方便上下文规范和修改，每一个CoroutineContext元素是CoroutineContext本身。
```
launch(CoroutineName("Name1")) { ... }
launch(CoroutineName("Name2") + Job()) { ... }
```
每一个集合中的元素有一个唯一的Key用于标识它们，这些Key通过引用来比较。

例如CoroutineName和Job，都实现了实现CoroutineContext接口的CoroutineContext.Element接口，

查找CoroutineContext中的元素

    既然CoroutineContext像一个集合，那么我们可以通过get或[]用一个具体的key来查找一个元素。像map中一样，当element
在context中时，就返回element，不在的话则返回null。
```
fun main() {
    val ctx: CoroutineContext = CoroutineName("A name")

    val coroutineName: CoroutineName? = ctx[CoroutineName]
    // or ctx.get(CoroutineName)
    println(coroutineName?.name) // A name
    val job: Job? = ctx[Job] // or ctx.get(Job)
    println(job) // null
}
```
CoroutineName不是一个类型或者类，它是Kotlin的一个特性，它使用的类的名字作为伴生对象的引用。因此，ctx[CoroutineName]
是ctx[CoroutineName.Key]的缩写.
```
data class CoroutineName(
    val name: String
) : AbstractCoroutineContextElement(CoroutineName) {

    override fun toString(): String = "CoroutineName($name)"

    companion object Key : CoroutineContext.Key<CoroutineName>
}
```
kotlin.coroutines库中的常见做法是将伴生对象用作具有相同名称的元素的Key，这样更易于记住。

Key可以指向类（CoroutineName）或接口（Job）。

添加contexts

    CoroutineContext真正有用的能力是合并两个CoroutineContext到一起。当两个不同Key的元素被添加后，生成的上下文
对两个Key都有响应。
```
fun main() {
    val ctx1: CoroutineContext = CoroutineName("Name1")
    println(ctx1[CoroutineName]?.name) // Name1
    println(ctx1[Job]?.isActive) // null

    val ctx2: CoroutineContext = Job()
    println(ctx2[CoroutineName]?.name) // null
    println(ctx2[Job]?.isActive) // true, because "Active"
    // is the default state of a job created this way

    val ctx3 = ctx1 + ctx2
    println(ctx3[CoroutineName]?.name) // Name1
    println(ctx3[Job]?.isActive) // true
}
```
当另一个同样Key的元素被添加后，像map一样，新的元素会替换旧的。
```
fun main() {
    val ctx1: CoroutineContext = CoroutineName("Name1")
    println(ctx1[CoroutineName]?.name) // Name1

    val ctx2: CoroutineContext = CoroutineName("Name2")
    println(ctx2[CoroutineName]?.name) // Name2

    val ctx3 = ctx1 + ctx2
    println(ctx3[CoroutineName]?.name) // Name2
}
```
空的协程context
```
fun main() {
    val empty: CoroutineContext = EmptyCoroutineContext
    println(empty[CoroutineName]) // null
    println(empty[Job]) // null

    val ctxName = empty + CoroutineName("Name1") + empty
    println(ctxName[CoroutineName]) // CoroutineName(Name1)
}
```
移除context
通过调用minusKey，可以把元素从context中移除。
```
fun main() {
    val ctx = CoroutineName("Name1") + Job()
    println(ctx[CoroutineName]?.name) // Name1
    println(ctx[Job]?.isActive) // true

    val ctx2 = ctx.minusKey(CoroutineName)
    println(ctx2[CoroutineName]?.name) // null
    println(ctx2[Job]?.isActive) // true

    val ctx3 = (ctx + CoroutineName("Name2"))
        .minusKey(CoroutineName)
    println(ctx3[CoroutineName]?.name) // null
    println(ctx3[Job]?.isActive) // true
}
```
折叠context
如果我们想对每一个context做一些处理，我们可以使用fold方法， 像其他集合的fold方法一样。
```
fun main() {
    val ctx = CoroutineName("Name1") + Job()

    ctx.fold("") { acc, element -> "$acc$element " }
        .also(::println)
    // CoroutineName(Name1) JobImpl{Active}@dbab622e

    val empty = emptyList<CoroutineContext>()
    ctx.fold(empty) { acc, element -> acc + element }
        .joinToString()
        .also(::println)
    // CoroutineName(Name1), JobImpl{Active}@dbab622e
}
```
# [Kotlin Coroutines dispatchers](https://kt.academy/article/cc-dispatchers)

Kotlin协程库提供的一个重要功能就是让我们决定协程在哪个线程上运行。这是通过Dispatcher实现的。

默认分发器（Default Dispatcher）

    设计是用于CPU密集计算操作的。它有一个线程池，大小为代码运行的机器上的核数。
limitedParallelism---限制默认分发器同一个线程下并行执行数

IO分发器
    设计用于因IO操作阻塞线程时。IO分发器允许同时超过50个线程运行。
    
默认分发器和IO分发器共享同一个线程池。线程被复用，通常不需要再重新分发。例如，现在运行在Default Dispatcher上，然后
执行 withContext(Dispatchers.IO) { ... }，通常你会保持在同一线程内，但线程数不是算在Default Dispatcher下，而是
在IO Dispatcher下，因为Default Dispatcher和IO Dispatcher的线程限制是独立的，互不影响的。
```
import kotlinx.coroutines.*

suspend fun main(): Unit = coroutineScope {
    launch(Dispatchers.Default) {
        println(Thread.currentThread().name)
        withContext(Dispatchers.IO) {
            println(Thread.currentThread().name)
        }
    }
}
// DefaultDispatcher-worker-2
// DefaultDispatcher-worker-2
```
Dispatchers.IO线程限定是64时. 若有大量阻塞线程的服务可能会使其他一直在等待，为了解决这种情况，我们使用limitedParallelism.

固定线程数的Dispatcher
```
val NUMBER_OF_THREADS = 20
val dispatcher = Executors
    .newFixedThreadPool(NUMBER_OF_THREADS)
    .asCoroutineDispatcher()
```
使用ExecutorService.asCoroutineDispatcher()最大的问题是，我们需要自己调用close函数，否则会出现线程泄漏情况。
另一个问题是，你将一直保留无用的线程，且无法和其他服务共享线程。

单线程的Dispatcher
```
val dispatcher = Executors.newSingleThreadExecutor()
    .asCoroutineDispatcher()
```
不过这同样存在无用线程无法复用、以及需要手动close的问题，我们可以使用IO Dispatcher或Default Dispatcher来解决。
```
var i = 0

suspend fun main(): Unit = coroutineScope {
    val dispatcher = Dispatchers.Default
        .limitedParallelism(1)
    
    repeat(10000) {
        launch(dispatcher) {
            i++
        }
    }
    delay(1000)
    println(i) // 10000
}
```
使用Loom项目的虚拟线程

JVM平台引入了Loom Project，创新的提出了虚拟线程，比普通线程更轻量级。

Loom Dispatcher，需要JVM 19以后，且开启--enable-preview
```
val LoomDispatcher = Executors
    .newVirtualThreadPerTaskExecutor()
    .asCoroutineDispatcher()
```
或
```
object LoomDispatcher : ExecutorCoroutineDispatcher() {

    override val executor: Executor = Executor { command ->
        Thread.startVirtualThread(command)
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable
    ) {
        executor.execute(block)
    }

    override fun close() {
        error("Cannot be invoked on Dispatchers.LOOM")
    }
}

val Dispatchers.Loom: CoroutineDispatcher
    get() = LoomDispatcher
```
Immediate main dispatching

有一个与协程分发相关的消耗，当调用withContext时，协程需要挂起，可能在队列中等待，然后再恢复。

当我们已经在当前线程上时，这是一个虽小但不必要的消耗。
```
suspend fun showUser(user: User) =
    withContext(Dispatchers.Main) {
        userNameElement.text = user.name
        // ...
    }
```
上面的代码中，当showUser已经在主线程中调用时，withContext(Dispatchers.Main)会带来不必要的重分发和排队。为了避免
这种情况，我们使用Dispatchers.Main.immediate，仅当需要分发时分发。

延续拦截器（Continuation interceptor）

分发是基于Kotlin语言内置的延续拦截机制。有一个名为ContinuationInterceptor的协程context，它有个interceptContinuation
方法在协程挂起时用来修改延续。还有一个releaseInterceptedContinuation方法，来在延续结束时调用。
```
public interface ContinuationInterceptor :
    CoroutineContext.Element {

    companion object Key :
        CoroutineContext.Key<ContinuationInterceptor>

    fun <T> interceptContinuation(
        continuation: Continuation<T>
    ): Continuation<T>

    fun releaseInterceptedContinuation(
        continuation: Continuation<*>
    ) {
    }

    //...
}
```
拥有封装continuation的能力带来了极大控制，Dispatcher用interceptContinuation将continuation包装为DispatchedContinuation，
这些延续运行在指定的线程池。以上就是Dispatcher如何工作的。
