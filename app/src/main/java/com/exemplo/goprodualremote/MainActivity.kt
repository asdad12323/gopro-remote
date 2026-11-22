package com.exemplo.goprodualremote

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*

private val Ink = Color(0xFF0E1116)
private val Panel = Color(0xFF171C24)
private val Line = Color(0xFF263040)
private val Txt = Color(0xFFE9EEF5)
private val Mut = Color(0xFF8B97A6)
private val Acc = Color(0xFF2EA8FF)
private val Rec = Color(0xFFFF4D4F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wifi = Wifi(this); val prev = Preview(this)
        val prefs = getSharedPreferences("cams", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Acc)) {
                Surface(color = Ink) { Remote(wifi, prev, prefs) }
            }
        }
    }
}

@Composable
fun Pill(text: String, on: Boolean, modifier: Modifier = Modifier, click: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(if (on) Acc else Panel)
        .border(1.dp, if (on) Acc else Line, RoundedCornerShape(50)).clickable { click() }
        .padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (on) Ink else Txt, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun Tile(text: String, on: Boolean = false, modifier: Modifier = Modifier, click: () -> Unit) {
    Box(modifier.height(56.dp).clip(RoundedCornerShape(14.dp)).background(Panel)
        .border(1.dp, if (on) Acc else Line, RoundedCornerShape(14.dp)).clickable { click() },
        contentAlignment = Alignment.Center) {
        Text(text, color = if (on) Acc else Txt, fontSize = 13.sp)
    }
}

@Composable
fun Remote(wifi: Wifi, prev: Preview, prefs: SharedPreferences) {
    val api = remember { Hero4Api() }
    val scope = rememberCoroutineScope()
    var cam by remember { mutableIntStateOf(0) }
    var online by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var st by remember { mutableStateOf(Status(0, false, 0)) }
    var mode by remember { mutableIntStateOf(0) }
    var res by remember { mutableIntStateOf(9) }
    var live by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var beep by remember { mutableStateOf(false) }
    fun nm(i: Int) = prefs.getString("n$i", "Câmara ${i + 1}") ?: ""
    fun io(f: () -> Unit) { scope.launch(Dispatchers.IO) { f() } }

    fun connect() {
        if (prefs.getString("s$cam", "").isNullOrBlank()) { edit = true; return }
        busy = true; msg = "A ligar ao Wi-Fi da câmara…"; live = false; prev.stopLocal()
        wifi.join(prefs.getString("s$cam", "")!!, prefs.getString("p$cam", "")!!,
            { online = true; busy = false; msg = "" }, { online = false; busy = false; msg = it })
    }
    fun pick(i: Int) {
        if (i == cam && (online || busy)) return
        prev.stopLocal(); wifi.leave(); online = false; live = false; cam = i; connect()
    }
    LaunchedEffect(online, cam) {
        while (online) {
            withContext(Dispatchers.IO) { api.status() }?.let { st = it }
            delay(2000)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(nm(0), cam == 0) { pick(0) }
            Pill(nm(1), cam == 1) { pick(1) }
            Spacer(Modifier.weight(1f))
            Pill("Definir Wi-Fi", false) { edit = true }
        }

        Box(Modifier.fillMaxWidth().aspectRatio(16 / 9f).clip(RoundedCornerShape(20.dp))
            .background(Color.Black).border(1.dp, Line, RoundedCornerShape(20.dp))) {
            if (live) AndroidView(factory = { PlayerView(it).apply { useController = false; player = prev.player } },
                modifier = Modifier.fillMaxSize())
            else Text(if (online) "Preview desligado" else if (msg.isEmpty()) "Sem ligação à câmara" else msg,
                Modifier.align(Alignment.Center).padding(24.dp), color = Mut, fontSize = 14.sp)
            if (online) {
                Row(Modifier.align(Alignment.TopStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (st.rec) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Rec)); Spacer(Modifier.width(8.dp))
                        Text("%02d:%02d".format(st.seconds / 60, st.seconds % 60), color = Txt, fontWeight = FontWeight.SemiBold)
                    }
                }
                val b = st.battery.coerceIn(0, 3)
                Text("▮".repeat(b) + "▯".repeat(3 - b), Modifier.align(Alignment.TopEnd).padding(14.dp), color = if (b == 0) Rec else Txt)
            }
        }

        if (!online) Button(onClick = { connect() }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)) { Text(if (busy) "A ligar…" else "Ligar à ${nm(cam)}") }
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf("Vídeo", "Foto", "Multi").forEachIndexed { i, t ->
                    Pill(t, mode == i) { mode = i; io { api.mode(i) } }; Spacer(Modifier.width(8.dp))
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(96.dp).clip(CircleShape).border(4.dp, Txt, CircleShape).clickable {
                    val on = !st.rec; if (mode == 0) st = st.copy(rec = on); io { api.shutter(on || mode != 0) }
                }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(if (st.rec) 34.dp else 72.dp)
                        .clip(RoundedCornerShape(if (st.rec) 8.dp else 40.dp))
                        .background(if (mode == 0) Rec else Txt))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(if (live) "Parar preview" else "Preview", live, Modifier.weight(1f)) {
                    if (live) { prev.stop(api); live = false } else { prev.start(api); live = true }
                }
                Tile("Marcar", false, Modifier.weight(1f)) { io { api.tag() } }
                Tile(if (beep) "Parar bip" else "Localizar", beep, Modifier.weight(1f)) { beep = !beep; io { api.locate(beep) } }
                Tile("Desligar", false, Modifier.weight(1f)) { prev.stopLocal(); live = false; io { api.sleep() }; wifi.leave(); online = false }
            }
            Text("Resolução de vídeo", color = Mut, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720p" to 12, "1080p" to 9, "2.7K" to 4, "4K" to 1).forEach { (t, v) ->
                    Pill(t, res == v) { res = v; io { api.setting(2, v) } }
                }
            }
            Pill("Trocar para ${nm(1 - cam)}", false) { pick(1 - cam) }
        }
    }

    if (edit) {
        var n by remember { mutableStateOf(nm(cam)) }
        var s by remember { mutableStateOf(prefs.getString("s$cam", "") ?: "") }
        var p by remember { mutableStateOf(prefs.getString("p$cam", "") ?: "") }
        AlertDialog(onDismissRequest = { edit = false },
            title = { Text("Wi-Fi da ${nm(cam)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(n, { n = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(s, { s = it }, label = { Text("Nome da rede (SSID)") }, singleLine = true)
                    OutlinedTextField(p, { p = it }, label = { Text("Palavra-passe do Wi-Fi") }, singleLine = true)
                    Text("Na câmara: Wireless → Wi-Fi ligado. Grava-se uma vez.", color = Mut, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = {
                prefs.edit().putString("n$cam", n).putString("s$cam", s.trim()).putString("p$cam", p).apply(); edit = false
            }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { edit = false }) { Text("Cancelar") } })
    }
}
