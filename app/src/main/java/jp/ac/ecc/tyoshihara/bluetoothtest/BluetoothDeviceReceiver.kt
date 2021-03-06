package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log

class BluetoothDeviceReceiver(handler: Handler) : BroadcastReceiver() {
    val TAG = "BluetoothDeviceReceiver"
    companion object {
        const val DEVICE_FOUND = 2
        const val DISCOVERY_START = 3
        const val DISCOVERY_END = 4
    }

    val handler = handler

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action : String? = intent.action

        when(action){
            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name
                Log.i(TAG, "deviceName:"+deviceName)

                deviceName?.also {
                    // 見つかったデバイス情報をメインスレッドに送る
                    val deviceMsg = handler.obtainMessage(DEVICE_FOUND)
                    deviceMsg.obj = device
                    deviceMsg.sendToTarget()
                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                val deviceMsg = handler.obtainMessage(DISCOVERY_START)
                deviceMsg.obj = "デバイス検索を開始します"
                deviceMsg.sendToTarget()
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                val deviceMsg = handler.obtainMessage(DISCOVERY_END)
                deviceMsg.obj = "デバイス検索を終了します"
                deviceMsg.sendToTarget()
            }
        }
    }
}