package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import android.os.Handler
import java.util.*

@SuppressLint("MissingPermission")
internal class BluetoothConnectThread(device: BluetoothDevice, handler: Handler) : Thread() {

    val TAG = "BluetoothConnectThread"

    // SPP(Serial Port Profile)
    val SPP = "00001101-0000-1000-8000-00805F9B34FB"

    lateinit var BTConnectedThread : BluetoothConnectedThread
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        // SPP(Serial Port Profile)のUUIDをセットしてシリアル通信を行う
        device.createRfcommSocketToServiceRecord(UUID.fromString(SPP))
        // device.createRfcommSocketToServiceRecord(device.uuids[0].uuid) // デバイスから取得することも可能？？
    }

    private val handler = handler

    override fun run() {

        // Bluetooth通信を行うスレッドを開始
        mmSocket!!.connect()
        BTConnectedThread = BluetoothConnectedThread(mmSocket!!, handler)
        BTConnectedThread.start()

    }

    // Bluetooth接続解除メソッド
    fun release() {
        try {
            BTConnectedThread.cancel()
            while (true){
                // スレッドが完了してからソケットを終了させる
                if(!BTConnectedThread.isAlive){
                    mmSocket?.close()
                    break
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the client socket", e)
        }
    }

    // メッセージ送信処理
    fun msgWrite(message : String){
        BTConnectedThread.msgWrite(message)
    }
}