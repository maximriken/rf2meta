package net.l2meta.scanner

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.Display
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Скріншот екрана через сервіс спеціальних можливостей (Android 11+).
 * Android дозволяє не частіше ~1 скріншота на 1/3 секунди.
 */
object Screenshots {

    suspend fun capture(service: AccessibilityService): Bitmap =
        suspendCancellableCoroutine { cont ->
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val buffer = result.hardwareBuffer
                        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        // Апаратний bitmap не можна читати по пікселях — копіюємо у звичайний.
                        val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                        hw?.recycle()
                        buffer.close()
                        if (!cont.isActive) return
                        if (bmp != null) cont.resume(bmp)
                        else cont.resumeWithException(IllegalStateException("не вдалося прочитати скріншот"))
                    }

                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) {
                            cont.resumeWithException(
                                IllegalStateException("скріншот не вдався (код $errorCode)")
                            )
                        }
                    }
                },
            )
        }
}
