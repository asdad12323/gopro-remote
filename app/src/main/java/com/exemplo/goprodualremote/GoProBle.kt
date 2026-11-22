package com.exemplo.goprodualremote

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// (nome do ficheiro mantido para substituir o antigo) API Wi-Fi da HERO4
data class Status(val battery: Int, val rec: Boolean, val seconds: Int)

class Hero4Api {
    fun get(path: String): String? = try {
        val c = URL("http://10.5.5.9$path").openConnection() as HttpURLConnection
        c.connectTimeout = 3000; c.readTimeout = 3000
        c.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) { null }

    fun shutter(on: Boolean) = get("/gp/gpControl/command/shutter?p=${if (on) 1 else 0}")
    fun mode(m: Int) = get("/gp/gpControl/command/mode?p=$m")          // 0 vídeo, 1 foto, 2 multi
    fun tag() = get("/gp/gpControl/command/storage/tag_moment")
    fun locate(on: Boolean) = get("/gp/gpControl/command/system/locate?p=${if (on) 1 else 0}")
    fun sleep() = get("/gp/gpControl/command/system/sleep")
    fun setting(id: Int, value: Int) = get("/gp/gpControl/setting/$id/$value")

    fun status(): Status? = try {
        val s = JSONObject(get("/gp/gpControl/status") ?: return null).getJSONObject("status")
        Status(s.optInt("2"), s.optInt("10") == 1, s.optInt("13"))
    } catch (e: Exception) { null }
}
