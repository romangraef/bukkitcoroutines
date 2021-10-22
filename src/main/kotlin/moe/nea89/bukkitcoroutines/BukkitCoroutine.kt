package moe.nea89.bukkitcoroutines

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


/**
 * Base class for coroutine operations
 */
@OptIn(ExperimentalContracts::class)
data class BukkitCoroutine<T>(val plugin: Plugin, val future: CompletableFuture<T>) {
    /**
     * Calling this function will cause the coroutine to continue execution on an async thread provided by the Bukkit scheduler.
     *
     * If this is called from an async thread, this returns immediately.
     */
    suspend fun switchToAsync(): Unit = suspendCoroutine<Unit> {
        plugin.continueOnAsync(it)
    }

    /**
     * Calling this function will cause the coroutine to continue execution on the main thread provided by the Bukkit scheduler.
     *
     * If this is called from the main thread, this returns immediately.
     */
    suspend fun switchToMain(): Unit = suspendCoroutine {
        plugin.continueOnMain(it)
    }

    /**
     * See [BukkitCoroutine.waitTicksAndContinueInSameThread].
     *
     * Since the Bukkit scheduler only supports waiting in minecraft tick intervals, the waiting duration gets rounded down to the nearest 20th of a second.
     *
     * @param duration waiting duration
     */
    @ExperimentalTime
    suspend fun waitAndContinueInSameThread(duration: Duration): Unit = suspendCoroutine {
        val ticks = duration.toLong(DurationUnit.MILLISECONDS) * 20 / 1000
        plugin.waitAndContinueInSameThread(it, ticks)
    }

    /**
     * Wait a `duration` and continue in the same asyncness (continues on main if called on main, otherwise continues on any async thread).
     *
     * @param ticks waiting duration in ticks
     */
    suspend fun waitTicksAndContinueInSameThread(ticks: Long): Unit = suspendCoroutine {
        plugin.waitAndContinueInSameThread(it, ticks)
    }

    /**
     * Executes the given `block` such that no matter what context switches are done within the `block`, after this function returns, execution is resumed in the same asyncness.
     */
    suspend fun <R> keepThread(block: suspend BukkitCoroutine<T>.() -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val isMainThread = Bukkit.isPrimaryThread()
        return try {
            block()
        } finally {
            if (isMainThread) {
                switchToMain()
            } else {
                switchToAsync()
            }
        }
    }

    /**
     * Executes the given `block` on the main thread and afterwards continues on the same asyncness.
     *
     * If you already are on main, this calls the block in place without invoking the scheduler.
     */
    suspend fun <R> callOnMain(block: () -> R): R = suspendCoroutine {
        plugin.callOnMain(it, block)
    }

    /**
     * Executes the given `block` in an async schedule and afterwards continues on the same asyncness.
     *
     * If you already are in an async task, this calls the block in place without invoking the scheduler
     */
    suspend fun <R> callOnAsync(block: () -> R): R = suspendCoroutine {
        plugin.callOnAsync(it, block)
    }

}
