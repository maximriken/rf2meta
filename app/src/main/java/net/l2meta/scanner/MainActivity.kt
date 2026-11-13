package net.l2meta.scanner

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        status = TextView(this).apply { textSize = 16f }

        val help = TextView(this).apply {
            textSize = 14f
            text = """
                Як увімкнути:
                1. Натисніть «Відкрити Спеціальні можливості».
                2. Знайдіть «L2Meta Сканер ринку» → увімкніть.
                3. Якщо перемикач сірий («Обмежене налаштування»):
                   Налаштування → Програми → L2Meta Сканер → ⋮ (угорі праворуч)
                   → «Дозволити обмежені налаштування», і повторіть крок 2.
                4. Зайдіть у гру — збоку з'явиться синя кнопка «Скан».
                   Її можна перетягувати пальцем.

                Зараз кнопка «Скан» зберігає скріншот у «Завантаження/L2Meta».
            """.trimIndent()
        }

        val openSettings = Button(this).apply {
            text = "Відкрити Спеціальні можливості"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val testCsv = Button(this).apply {
            text = "Тестовий CSV (перевірити Google Таблиці)"
            setOnClickListener { exportTestCsv() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(status)
            addView(openSettings)
            addView(testCsv)
            addView(help)
        })
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled())
            "✅ Сервіс увімкнено. Відкрийте гру."
        else
            "❌ Сервіс вимкнено. Увімкніть його в Спеціальних можливостях."
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.startsWith("$packageName/") }
    }

    /** Вигадані дані, щоб перевірити, як CSV відкривається в Google Таблицях. */
    private fun exportTestCsv() {
        val offers = listOf(
            Offer("Мажестик плейт тело", 12_000_000, 1, "Vasya", Side.SELL),
            Offer("Мажестик плейт тело", 13_500_000, 1, "Kolya", Side.SELL),
            Offer("Мажестик плейт тело", 15_000_000, 1, "Petya", Side.BUY),
            Offer("Халиша", 900_000, 5, "MorningStar", Side.SELL),
            Offer("Халиша", 850_000, 10, "Buyer1", Side.BUY),
            Offer("Камень, Огня", 50_000, 100, "Seller2", Side.SELL),
        )
        Thread {
            val msg = try {
                val files = CsvExporter.export(
                    this, MarketAnalyzer.analyze(offers), "TEST_" + ScannerService.timestamp()
                )
                "Збережено в Завантаження/L2Meta: " + files.joinToString()
            } catch (e: Exception) {
                "Помилка: ${e.message}"
            }
            runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }.start()
    }
}
