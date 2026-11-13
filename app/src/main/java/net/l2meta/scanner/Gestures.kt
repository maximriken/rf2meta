package net.l2meta.scanner

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Натискання і свайпи по екрану від імені користувача.
 * Координати — у пікселях екрана (як на скріншоті).
 */
object Gestures {

    suspend fun tap(service: AccessibilityService, x: Float, y: Float, durationMs: Long = 60): Boolean {
        val path = Path().apply { moveTo(x, y) }
        return dispatch(service, path, durationMs)
    }

    suspend fun swipe(
        service: AccessibilityService,
        fromX: Float, fromY: Float,
        toX: Float, toY: Float,
        durationMs: Long = 400,
    ): Boolean {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        return dispatch(service, path, durationMs)
    }

    private suspend fun dispatch(service: AccessibilityService, path: Path, durationMs: Long): Boolean =
        suspendCancellableCoroutine { cont ->
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            val accepted = service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
                null,
            )
            if (!accepted && cont.isActive) cont.resume(false)
        }
}
