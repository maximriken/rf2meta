package net.l2meta.scanner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Головний сервіс програми (Спеціальні можливості).
 * Він показує кнопку «Скан» поверх гри, робить скріншоти і натискає по екрану.
 *
 * Етап 1: кнопка «Скан» просто зберігає скріншот у «Завантаження/L2Meta».
 * Етап 2: тут буде повне сканування ринку (MarketScanner).
 */
class ScannerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlay: OverlayButton? = null
    private var job: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayButton(this) { onScanClick() }.also { it.show() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        overlay?.hide()
        overlay = null
        super.onDestroy()
    }

    private fun onScanClick() {
        // Повторне натискання під час роботи — зупинка.
        if (job?.isActive == true) {
            job?.cancel()
            overlay?.setText("Скан")
            toast("Зупинено")
            return
        }
        job = scope.launch {
            val btn = overlay ?: return@launch
            try {
                btn.setText("…")
                // Ховаємо кнопку, щоб її не було на скріншоті.
                btn.setVisible(false)
                delay(200)
                val bitmap = try {
                    Screenshots.capture(this@ScannerService)
                } finally {
                    btn.setVisible(true)
                }
                val name = "screen_" + timestamp()
                withContext(Dispatchers.IO) {
                    FileStore.savePng(this@ScannerService, bitmap, name)
                }
                toast("Збережено: Завантаження/L2Meta/$name.png (${bitmap.width}×${bitmap.height})")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                toast("Помилка: ${e.message}")
            } finally {
                btn.setText("Скан")
            }
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    companion object {
        fun timestamp(): String =
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    }
}
