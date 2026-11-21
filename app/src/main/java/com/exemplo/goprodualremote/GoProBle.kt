package com.exemplo.goprodualremote

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.util.UUID

private val GOPRO_SVC = UUID.fromString("0000fea6-0000-1000-8000-00805f9b34fb")
private val CMD = UUID.fromString("b5f90072-aa8d-11e3-9046-0002a5d5c51b")
private val CMD_RSP = UUID.fromString("b5f90073-aa8d-11e3-9046-0002a5d5c51b")
private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

class Cam(val device: BluetoothDevice, val label: String) {
    var gatt: BluetoothGatt? = null
    var cmd: BluetoothGattCharacteristic? = null
    val connected = mutableStateOf(false)
    val recording = mutableStateOf(false)
    val status = mutableStateOf("Encontrada")
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class GoProBle(private val ctx: Context) {
    val cams = mutableStateListOf<Cam>()
    private val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val ui = Handler(Looper.getMainLooper())

    fun connectedCams() = cams.filter { it.connected.value }

    fun scan() {
        val s = adapter?.bluetoothLeScanner ?: return
        val f = ScanFilter.Builder().setServiceUuid(ParcelUuid(GOPRO_SVC)).build()
        s.startScan(listOf(f), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), object : ScanCallback() {
            override fun onScanResult(t: Int, r: ScanResult) {
                if (cams.none { it.device.address == r.device.address })
                    ui.post { cams.add(Cam(r.device, r.device.name ?: r.device.address)) }
            }
        })
        ui.postDelayed({ s.stopScan(object : ScanCallback() {}) }, 15000)
    }

    fun connect(c: Cam) {
        c.status.value = "A ligar..."
        if (c.device.bondState == BluetoothDevice.BOND_NONE) c.device.createBond()
        c.gatt = c.device.connectGatt(ctx, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, st: Int, ns: Int) {
                if (ns == BluetoothProfile.STATE_CONNECTED) g.discoverServices()
                else ui.post { c.connected.value = false; c.status.value = "Desligada" }
            }
            override fun onServicesDiscovered(g: BluetoothGatt, st: Int) {
                val all = g.services.flatMap { it.characteristics }
                c.cmd = all.firstOrNull { it.uuid == CMD }
                val rsp = all.firstOrNull { it.uuid == CMD_RSP }
                if (c.cmd == null || rsp == null) { ui.post { c.status.value = "Não é uma GoPro compatível" }; return }
                g.setCharacteristicNotification(rsp, true)
                rsp.getDescriptor(CCCD)?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(it)
                }
            }
            override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, st: Int) {
                ui.post { c.connected.value = true; c.status.value = "Ligada" }
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }

    fun send(c: Cam?, vararg b: Int) {
        val ch = c?.cmd ?: return
        ch.value = ByteArray(b.size) { b[it].toByte() }
        c.gatt?.writeCharacteristic(ch)
    }

    fun shutter(c: Cam?, on: Boolean) { send(c, 0x03, 0x01, 0x01, if (on) 1 else 0); c?.recording?.value = on }
    fun tag(c: Cam?) = send(c, 0x01, 0x18)
    fun sleep(c: Cam?) = send(c, 0x01, 0x05)
    fun apOn(c: Cam?) = send(c, 0x03, 0x17, 0x01, 0x01)
    fun presetVideo(c: Cam?) = send(c, 0x04, 0x3E, 0x02, 0x03, 0xE8)
    fun presetPhoto(c: Cam?) = send(c, 0x04, 0x3E, 0x02, 0x03, 0xE9)
    fun photo(c: Cam?) { presetPhoto(c); ui.postDelayed({ shutter(c, true) }, 800) }
}
