package net.l2meta.scanner

import android.content.Context
import java.util.Locale

/** Хто виставив запис: продавець чи скупник. */
enum class Side { SELL, BUY }

/** Один рядок із «Списку продаваемых/покупаемых товаров». */
data class Offer(
    val item: String,
    val price: Long,
    val quantity: Long,
    val player: String,
    val side: Side,
)

/** Підсумок по одному предмету. */
data class ItemStats(
    val item: String,
    val minSell: Long?,
    val maxSell: Long?,
    val maxBuy: Long?,
    val sellers: Int,
    val buyers: Int,
) {
    /** Скупка мінус продаж: скільки адени можна заробити на одній штуці. */
    val diffAdena: Long?
        get() = if (minSell != null && maxBuy != null) maxBuy - minSell else null

    /** Те саме у відсотках від ціни продажу. */
    val diffPercent: Double?
        get() = if (minSell != null && maxBuy != null && minSell > 0)
            (maxBuy - minSell) * 100.0 / minSell else null
}

object MarketAnalyzer {

    /** Ключ для об'єднання однакових предметів зі списків продажу і скупки. */
    fun key(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    fun analyze(offers: List<Offer>): List<ItemStats> =
        offers.groupBy { key(it.item) }.map { (_, list) ->
            val sells = list.filter { it.side == Side.SELL }
            val buys = list.filter { it.side == Side.BUY }
            ItemStats(
                item = list.first().item.trim(),
                minSell = sells.minOfOrNull { it.price },
                maxSell = sells.maxOfOrNull { it.price },
                maxBuy = buys.maxOfOrNull { it.price },
                sellers = sells.map { it.player }.distinct().size,
                buyers = buys.map { it.player }.distinct().size,
            )
        }
}

/** Експорт результатів у два CSV-файли для Google Таблиць. */
object CsvExporter {

    /** Повертає назви створених файлів. */
    fun export(ctx: Context, stats: List<ItemStats>, timestamp: String): List<String> {
        val both = stats
            .filter { it.minSell != null && it.maxBuy != null }
            .sortedByDescending { it.diffAdena ?: Long.MIN_VALUE }
        val onlySell = stats
            .filter { it.minSell != null && it.maxBuy == null }
            .sortedBy { it.item }

        val arbitrage = buildString {
            appendRow(
                "Предмет", "Мін. ціна продажу", "Макс. ціна скупки",
                "Різниця (адена)", "Різниця (%)", "К-сть продавців", "К-сть скупників",
            )
            for (s in both) {
                appendRow(
                    s.item, s.minSell.toString(), s.maxBuy.toString(),
                    s.diffAdena.toString(),
                    // Крапка як десятковий роздільник (локаль US).
                    String.format(Locale.US, "%.2f", s.diffPercent ?: 0.0),
                    s.sellers.toString(), s.buyers.toString(),
                )
            }
        }

        val sellOnly = buildString {
            appendRow("Предмет", "Мін. ціна продажу", "Макс. ціна продажу", "К-сть продавців")
            for (s in onlySell) {
                appendRow(s.item, s.minSell.toString(), s.maxSell.toString(), s.sellers.toString())
            }
        }

        val f1 = "arbitrage_$timestamp.csv"
        val f2 = "only_sell_$timestamp.csv"
        FileStore.saveCsv(ctx, f1, arbitrage)
        FileStore.saveCsv(ctx, f2, sellOnly)
        return listOf(f1, f2)
    }

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }

    private fun escape(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + v.replace("\"", "\"\"") + "\""
        else v
}
