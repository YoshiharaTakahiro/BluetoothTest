package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

@SuppressLint("MissingPermission")
internal class BluetoothConnectThread(device: BluetoothDevice, activity: AppCompatActivity) : Thread() {

    val TAG = "BluetoothConnectThread"

    lateinit var BTConnectedThread : BluetoothConnectedThread
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
    }

    private val activity = activity

    override fun run() {
        // Cancel discovery because it otherwise slows down the connection.
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.cancelDiscovery()

        mmSocket!!.connect()
        BTConnectedThread = BluetoothConnectedThread(mmSocket!!, activity)
        BTConnectedThread.start()

    }

    // Closes the client socket and causes the thread to finish.
    fun cancel() {
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
}