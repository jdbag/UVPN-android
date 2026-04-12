package com.uvpn.app.util

import com.google.gson.Gson
import com.uvpn.app.model.IpInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object IpRepository {

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    @Suppress("UNCHECKED_CAST")
    suspend fun fetch(): IpInfo = withContext(Dispatchers.IO) {
        // Primary
        try {
            val r = http.newCall(Request.Builder().url("https://ipapi.co/json/").build()).execute()
            val d = Gson().fromJson(r.body?.string(), Map::class.java) as Map<String, Any?>
            if (d["ip"] != null) return@withContext IpInfo(
                ip      = d["ip"].toString(),
                country = d["country_name"]?.toString() ?: "--",
                city    = d["city"]?.toString() ?: "--",
                isp     = (d["org"]?.toString() ?: "--").replace(Regex("^AS\\d+\\s"), "")
            )
        } catch (_: Exception) {}

        // Fallback 1
        try {
            val url = "http://ip-api.com/json/?fields=status,query,country,city,isp"
            val r = http.newCall(Request.Builder().url(url).build()).execute()
            val d = Gson().fromJson(r.body?.string(), Map::class.java) as Map<String, Any?>
            if (d["query"] != null) return@withContext IpInfo(
                ip      = d["query"].toString(),
                country = d["country"]?.toString() ?: "--",
                city    = d["city"]?.toString() ?: "--",
                isp     = d["isp"]?.toString() ?: "--"
            )
        } catch (_: Exception) {}

        // Fallback 2
        try {
            val r = http.newCall(Request.Builder().url("https://api.ipify.org?format=json").build()).execute()
            val d = Gson().fromJson(r.body?.string(), Map::class.java) as Map<String, Any?>
            return@withContext IpInfo(ip = d["ip"]?.toString() ?: "N/A")
        } catch (_: Exception) {}

        IpInfo(ip = "Check network")
    }
}
