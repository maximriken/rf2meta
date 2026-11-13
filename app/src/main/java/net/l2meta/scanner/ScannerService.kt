package net.l2meta.scanner

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Головний сервіс програми (Спеціальні можливості).
 *
 * Натискання «Скан»: якщо відкрите вікно Adventure Guildsman — програма сама
 * натискає «Список продаваемых товаров» і перевіряє, що відкрилось вікно категорій.
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

    /**
     * Натискання «Скан»: у вікні Adventure Guildsman натиснути
     * «Список продаваемых товаров». Екран лише перевіряється, файли не зберігаються.
     */
    private fun onScanClick() = runTask {
        val screen = capture()
        when {
            Guildsman.isSellCategories(screen) ->
                toast("«Список продаваемых товаров» уже відкрито")

            Guildsman.isMainMenu(screen) -> {
                val spot = Guildsman.SELL_LIST_LINK
                Gestures.tap(this, spot.x(screen.width), spot.y(screen.height))
                if (waitFor { Guildsman.isSellCategories(it) }) {
                    toast("✅ Відкрито «Список продаваемых товаров»")
                } else {
                    toast("❌ Не вдалося відкрити «Список продаваемых товаров»")
                }
            }

            else -> toast("Спочатку відкрийте вікно Adventure Guildsman")
        }
    }

    /** Чекає, поки на екрані з'явиться потрібне вікно (до timeoutMs). */
    private suspend fun waitFor(timeoutMs: Long = 3000, check: (Bitmap) -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            delay(500) // Android дозволяє скріншот не частіше ніж раз на ~1/3 с
            if (check(capture())) return true
        }
        return false
    }

    private suspend fun capture(): Bitmap = Screenshots.capture(this)

    /** Запускає дію; повторне натискання під час роботи — зупинка. */
    private fun runTask(block: suspend () -> Unit) {
        if (job?.isActive == true) {
            job?.cancel()
            overlay?.setText("Скан")
            toast("Зупинено")
            return
        }
        job = scope.launch {
            try {
                overlay?.setText("…")
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast("Помилка: ${e.message}")
            } finally {
                overlay?.setText("Скан")
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
