package com.exemplo.goprodualremote

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import java.net.HttpURLConnection
import java.net.URL

class Preview(ctx: Context, private val port: Int) {
    val player: ExoPlayer = ExoPlayer.Builder(ctx).build()

    // A câmara tem de estar alcançável em "ip" (Wi-Fi da GoPro ou rede comum).
    fun start(ip: String) {
        Thread {
            try {
                val u = URL("http://$ip:8080/gopro/camera/stream/start?port=$port")
                (u.openConnection() as HttpURLConnection).apply { connectTimeout = 4000; readTimeout = 4000 }.inputStream.close()
            } catch (_: Exception) {}
        }.start()
        player.setMediaItem(MediaItem.Builder().setUri("udp://@:$port").setMimeType(MimeTypes.VIDEO_MP2T).build())
        player.prepare(); player.playWhenReady = true
    }

    fun stop(ip: String) {
        player.stop()
        Thread { try { (URL("http://$ip:8080/gopro/camera/stream/stop").openConnection() as HttpURLConnection).inputStream.close() } catch (_: Exception) {} }.start()
    }
}
