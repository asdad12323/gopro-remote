package com.exemplo.goprodualremote

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    private val perm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        perm.launch(if (Build.VERSION.SDK_INT >= 31)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        val ble = GoProBle(this)
        val pv = listOf(Preview(this, 8554), Preview(this, 8555))
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface { App(ble, pv) } } }
    }
}

@Composable
fun App(ble: GoProBle, pv: List<Preview>) {
    var two by remember { mutableStateOf(false) }
    var sel by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { ble.scan() }) { Text("Procurar GoPro") }
        }
        ble.cams.forEach { c ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${c.label}: ${c.status.value}", Modifier.weight(1f))
                if (!c.connected.value) OutlinedButton(onClick = { ble.connect(c) }) { Text("Ligar") }
            }
        }
        Text("Live preview")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!two, { two = false }, { Text("1 live view") })
            FilterChip(two, { two = true }, { Text("2 live views") })
        }
        if (!two) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(sel == 0, { sel = 0 }, { Text("Câmara 1") })
            FilterChip(sel == 1, { sel = 1 }, { Text("Câmara 2") })
        }
        if (two) Button(onClick = { ble.connectedCams().take(2).forEach { ble.shutter(it, !it.recording.value) } }) { Text("Gravar / parar ambas") }
        (if (two) listOf(0, 1) else listOf(sel)).forEach { Slot(it, ble, pv[it]) }
    }
}

@Composable
fun Slot(i: Int, ble: GoProBle, p: Preview) {
    val c = ble.connectedCams().getOrNull(i)
    var ip by remember { mutableStateOf("10.5.5.9") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Câmara ${i + 1}: " + (c?.label ?: "sem câmara ligada"))
        AndroidView(factory = { PlayerView(it).apply { useController = false; player = p.player } },
            modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f))
        OutlinedTextField(ip, { ip = it }, label = { Text("IP da câmara") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { p.start(ip) }) { Text("Preview") }
            OutlinedButton(onClick = { p.stop(ip) }) { Text("Parar") }
            OutlinedButton(onClick = { ble.apOn(c) }) { Text("Ligar Wi-Fi") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { ble.shutter(c, !(c?.recording?.value ?: false)) }) { Text(if (c?.recording?.value == true) "Parar" else "Gravar") }
            OutlinedButton(onClick = { ble.photo(c) }) { Text("Foto") }
            OutlinedButton(onClick = { ble.presetVideo(c) }) { Text("Modo vídeo") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { ble.tag(c) }) { Text("Marcar momento") }
            OutlinedButton(onClick = { ble.sleep(c) }) { Text("Desligar") }
        }
    }
}
