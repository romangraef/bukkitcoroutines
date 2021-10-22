package moe.nea89.bukkitcoroutines

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.Callable
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
internal fun Plugin.waitAndContinueInSameThread(cont: Continuation<Unit>, ticks: Long) {
    if (Bukkit.isPrimaryThread()) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, Runnable {
            cont.resume(Unit)
        }, ticks)
    } else {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, Runnable {
            cont.resume(Unit)
        }, ticks)
    }
}

internal fun <T> Plugin.callOnAsync(cont: Continuation<T>, block: () -> T) {
    if (Bukkit.isPrimaryThread()) {
        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            try {
                val res = block()
                Bukkit.getScheduler().callSyncMethod(this, Callable {
                    cont.resume(res)
                })
            } catch (e: Throwable) {
                Bukkit.getScheduler().callSyncMethod(this, Callable {
                    cont.resumeWithException(e)
                })
            }
        })
    } else {
        try {
            cont.resume(block())
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
    }
}

internal fun <T> Plugin.callOnMain(cont: Continuation<T>, block: () -> T) {
    if (Bukkit.isPrimaryThread())
        try {
            cont.resume(block())
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
    else
        try {
            Bukkit.getScheduler().callSyncMethod(this) {
                block()
            }.get()
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
}

internal fun Plugin.continueOnAsync(cont: Continuation<Unit>, force: Boolean = true) {
    if (Bukkit.isPrimaryThread() || force)
        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            cont.resume(Unit)
        })
    else
        cont.resume(Unit)
}

internal fun Plugin.continueOnMain(cont: Continuation<Unit>, force: Boolean = true) {
    if (Bukkit.isPrimaryThread() && !force) {
        cont.resume(Unit)
    } else {
        Bukkit.getScheduler().callSyncMethod(this) {
            cont.resume(Unit)
        }
    }
}
