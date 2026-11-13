package net.l2meta.scanner

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Прямокутник на екрані в частках від ширини/висоти (0..1),
 * щоб координати не залежали від роздільної здатності.
 * Значення виміряні на скріні 1640×720.
 */
data class Area(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    companion object {
        fun px(x0: Int, y0: Int, x1: Int, y1: Int) =
            Area(x0 / 1640f, y0 / 720f, x1 / 1640f, y1 / 720f)
    }
}

/** Точка для натискання в частках екрана. */
data class Spot(val fx: Float, val fy: Float) {
    fun x(bmpWidth: Int) = fx * bmpWidth
    fun y(bmpHeight: Int) = fy * bmpHeight

    companion object {
        fun px(x: Int, y: Int) = Spot(x / 1640f, y / 720f)
    }
}

/** Прості перевірки кольору тексту у вікнах гри. */
object Colors {
    fun isOrange(c: Int): Boolean {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return r > 180 && g in 100..195 && b < 100
    }

    fun isBlue(c: Int): Boolean {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return b > 180 && g > 130 && b > r + 40
    }

    fun isWhite(c: Int): Boolean =
        Color.red(c) > 200 && Color.green(c) > 200 && Color.blue(c) > 200

    /** Яка частка пікселів у прямокутнику має потрібний колір (0..1). */
    fun share(bmp: Bitmap, area: Area, test: (Int) -> Boolean): Float {
        val x0 = (area.x0 * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val x1 = (area.x1 * bmp.width).toInt().coerceIn(x0 + 1, bmp.width)
        val y0 = (area.y0 * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val y1 = (area.y1 * bmp.height).toInt().coerceIn(y0 + 1, bmp.height)
        val w = x1 - x0
        val row = IntArray(w)
        var hit = 0
        var total = 0
        for (y in y0 until y1) {
            bmp.getPixels(row, 0, w, x0, y, w, 1)
            for (c in row) {
                total++
                if (test(c)) hit++
            }
        }
        return if (total == 0) 0f else hit.toFloat() / total
    }
}

/**
 * Вікно NPC «Adventure Guildsman».
 */
object Guildsman {

    // --- Головне меню ---
    /** Рядок «Информация по заданиям» (блакитний текст). */
    private val MAIN_FIRST_LINE = Area.px(570, 96, 850, 118)
    /** Рядок «Список продаваемых товаров» (помаранчевий текст). */
    private val MAIN_SELL_LINE = Area.px(570, 262, 884, 284)
    /** Куди натискати, щоб відкрити «Список продаваемых товаров». */
    val SELL_LIST_LINK = Spot.px(700, 273)

    // --- Вікно категорій продажу ---
    /** Білий заголовок «Список продаваемых товаров:». */
    private val SELL_HEADER = Area.px(570, 96, 890, 118)

    fun isMainMenu(bmp: Bitmap): Boolean =
        Colors.share(bmp, MAIN_FIRST_LINE, Colors::isBlue) > 0.03f &&
            Colors.share(bmp, MAIN_SELL_LINE, Colors::isOrange) > 0.06f

    fun isSellCategories(bmp: Bitmap): Boolean =
        Colors.share(bmp, SELL_HEADER, Colors::isWhite) > 0.06f &&
            Colors.share(bmp, SELL_HEADER, Colors::isBlue) < 0.01f
}
