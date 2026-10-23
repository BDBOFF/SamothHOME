package com.samoth.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Une appli installée, telle qu'affichée par le lanceur. */
data class AppEntry(
    val pkg: String,
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val color: Color,
    val launches: Int,
)

/** Liste les applis installées, retient leur utilisation et les lance. */
class AppsRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val usage = context.getSharedPreferences("usage", Context.MODE_PRIVATE)

    // Le dessin des icônes coûte cher : on le garde en mémoire tant que l'appli n'a pas changé.
    private data class Visual(val stamp: Long, val label: String, val banner: ImageBitmap?, val icon: ImageBitmap?, val color: Color)
    private val cache = HashMap<String, Visual>()

    fun load(): List<AppEntry> {
        val found = LinkedHashMap<String, ResolveInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            runCatching { pm.queryIntentActivities(query, 0) }.getOrDefault(emptyList()).forEach { ri ->
                val p = ri.activityInfo?.packageName ?: return@forEach
                if (p != context.packageName && p !in found) found[p] = ri
            }
        }
        val apps = found.map { (pkg, ri) ->
            val v = visualFor(pkg, ri)
            AppEntry(pkg, v.label, v.banner, v.icon, v.color, usage.getInt(pkg, 0))
        }
        cache.keys.retainAll(found.keys)
        return sort(apps)
    }

    /** Les plus utilisées d'abord ; à égalité, Samoth TV en tête puis ordre alphabétique. */
    fun sort(apps: List<AppEntry>): List<AppEntry> = apps.sortedWith(
        compareByDescending<AppEntry> { it.launches }
            .thenBy { if (it.pkg == SAMOTH_TV) 0 else 1 }
            .thenBy { it.label.lowercase() }
    )

    fun launch(app: AppEntry): Boolean {
        val intent = pm.getLeanbackLaunchIntentForPackage(app.pkg)
            ?: pm.getLaunchIntentForPackage(app.pkg)
            ?: return false
        usage.edit().putInt(app.pkg, usage.getInt(app.pkg, 0) + 1).apply()
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    fun launches(pkg: String) = usage.getInt(pkg, 0)

    /** Favoris choisis par l'utilisateur, dans l'ordre (max 5). */
    fun pinned(): List<String> =
        (usage.getString(PINNED, "") ?: "").split(',').filter { it.isNotBlank() }

    fun isSeeded() = usage.getBoolean(SEEDED, false)
    fun markSeeded() = usage.edit().putBoolean(SEEDED, true).apply()

    fun savePinned(list: List<String>) {
        usage.edit().putString(PINNED, list.distinct().take(5).joinToString(",")).apply()
    }

    private fun visualFor(pkg: String, ri: ResolveInfo): Visual {
        val stamp = runCatching { pm.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)
        cache[pkg]?.let { if (it.stamp == stamp) return it }
        val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg).trim()
        val bannerDrawable: Drawable? = runCatching { ri.activityInfo.loadBanner(pm) }.getOrNull()
            ?: runCatching { pm.getApplicationBanner(pkg) }.getOrNull()
        val iconDrawable: Drawable? = runCatching { ri.loadIcon(pm) }.getOrNull()
        val bannerBmp = bannerDrawable?.let { runCatching { it.toBitmap(320, 180, Bitmap.Config.ARGB_8888) }.getOrNull() }
        val iconBmp = iconDrawable?.let { runCatching { it.toBitmap(192, 192, Bitmap.Config.ARGB_8888) }.getOrNull() }
        val color = when {
            pkg == SAMOTH_TV -> SAMOTH_RED
            bannerBmp != null -> dominantColor(bannerBmp)
            iconBmp != null -> dominantColor(iconBmp)
            else -> NEUTRAL
        }
        val v = Visual(stamp, label, bannerBmp?.asImageBitmap(), iconBmp?.asImageBitmap(), color)
        cache[pkg] = v
        return v
    }

    companion object {
        const val SAMOTH_TV = "com.nova.tv"
        private const val PINNED = "__pinned__"
        private const val SEEDED = "__seeded__"
        val SAMOTH_RED = Color(0xFFD10A20)
        val NEUTRAL = Color(0xFFB4B4BC)

        /** Couleur principale d'une image : la teinte la plus présente parmi les pixels vifs. */
        fun dominantColor(src: Bitmap): Color {
            val bmp = Bitmap.createScaledBitmap(src, 32, 18, true)
            val buckets = 12
            val weight = DoubleArray(buckets)
            val r = DoubleArray(buckets); val g = DoubleArray(buckets); val b = DoubleArray(buckets)
            val hsv = FloatArray(3)
            for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
                val c = bmp.getPixel(x, y)
                if (AColor.alpha(c) < 140) continue
                AColor.colorToHSV(c, hsv)
                val w = (hsv[1] * hsv[2] * hsv[2]).toDouble()
                if (w < 0.08) continue
                val i = ((hsv[0] / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
                weight[i] += w; r[i] += AColor.red(c) * w; g[i] += AColor.green(c) * w; b[i] += AColor.blue(c) * w
            }
            val best = weight.indices.maxByOrNull { weight[it] } ?: return NEUTRAL
            if (weight[best] < 1.0) return NEUTRAL
            val w = weight[best]
            AColor.RGBToHSV((r[best] / w).toInt(), (g[best] / w).toInt(), (b[best] / w).toInt(), hsv)
            hsv[1] = hsv[1].coerceAtLeast(0.55f)
            hsv[2] = hsv[2].coerceAtLeast(0.85f)
            return Color(AColor.HSVToColor(hsv))
        }
    }
}
