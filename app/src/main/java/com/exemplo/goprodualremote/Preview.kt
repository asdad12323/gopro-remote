package com.exemplo.goprodualremote

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class Preview(ctx: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(ctx).build()
    @Volatile private var alive = false

    fun start(api: Hero4Api) {
        alive = true
        Thread {
            api.get("/gp/gpControl/execute?p1=gpStream&c1=restart")
            Thread.sleep(1500)
            Handler(Looper.getMainLooper()).post {
                player.setMediaItem(MediaItem.fromUri("http://10.5.5.9:8080/live/amba.m3u8"))
                player.prepare(); player.playWhenReady = true
            }
            // A HERO4 corta o stream se não receber este pacote de "estou aqui"
            val s = DatagramSocket()
            val d = "_GPHD_:0:0:2:0.000000\n".toByteArray()
            val a = InetAddress.getByName("10.5.5.9")
            while (alive) {
                try { s.send(DatagramPacket(d, d.size, a, 8554)) } catch (_: Exception) {}
                Thread.sleep(2500)
            }
            s.close()
        }.start()
    }

    fun stopLocal() {
        alive = false
        Handler(Looper.getMainLooper()).post { player.stop() }
    }

    fun stop(api: Hero4Api) {
        stopLocal()
        Thread { api.get("/gp/gpControl/execute?p1=gpStream&c1=stop") }.start()
    }
}
