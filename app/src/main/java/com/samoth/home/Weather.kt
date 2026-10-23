package com.samoth.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

enum class Sky { Clear, PartlyCloudy, Cloudy, Fog, Rain, Storm, Snow }

data class WeatherNow(
    val temp: Int,
    val min: Int,
    val max: Int,
    val sky: Sky,
    val isDay: Boolean,
    val label: String,
)

/** Météo du jour via Open-Meteo (gratuit, sans clé). */
object Weather {
    // La Roquette-sur-Siagne (06550)
    private const val LAT = 43.5808
    private const val LON = 6.9561

    suspend fun fetch(): WeatherNow? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=Europe%2FParis"
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "SamothHome/${BuildConfig.VERSION_CODE}")
            }
            val body = try {
                if (c.responseCode !in 200..299) return@runCatching null
                c.inputStream.bufferedReader().use { it.readText() }
            } finally {
                c.disconnect()
            }
            val o = JSONObject(body)
            val cur = o.getJSONObject("current")
            val daily = o.getJSONObject("daily")
            val code = cur.optInt("weather_code", 0)
            val (sky, label) = describe(code)
            WeatherNow(
                temp = cur.optDouble("temperature_2m").roundToInt(),
                min = daily.getJSONArray("temperature_2m_min").optDouble(0).roundToInt(),
                max = daily.getJSONArray("temperature_2m_max").optDouble(0).roundToInt(),
                sky = sky,
                isDay = cur.optInt("is_day", 1) == 1,
                label = label,
            )
        }.getOrNull()
    }

    /** Codes météo WMO → type de ciel et libellé court. */
    private fun describe(code: Int): Pair<Sky, String> = when (code) {
        0 -> Sky.Clear to "Ensoleillé"
        1 -> Sky.Clear to "Plutôt ensoleillé"
        2 -> Sky.PartlyCloudy to "Éclaircies"
        3 -> Sky.Cloudy to "Couvert"
        45, 48 -> Sky.Fog to "Brouillard"
        51, 53, 55, 56, 57 -> Sky.Rain to "Bruine"
        61, 63, 66, 80, 81 -> Sky.Rain to "Pluie"
        65, 67, 82 -> Sky.Rain to "Forte pluie"
        71, 73, 75, 77, 85, 86 -> Sky.Snow to "Neige"
        95, 96, 99 -> Sky.Storm to "Orage"
        else -> Sky.Cloudy to "Nuageux"
    }
}
