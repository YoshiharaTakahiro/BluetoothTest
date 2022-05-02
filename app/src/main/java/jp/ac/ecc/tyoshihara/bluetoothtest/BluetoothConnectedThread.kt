package jp.ac.ecc.tyoshihara.bluetoothtest

import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class BluetoothConnectedThread(mmSocket: BluetoothSocket, activity: AppCompatActivity) : Thread() {

    val TAG = "BluetoothConnectedThread"

    private val mmInStream: InputStream = mmSocket.inputStream
    // private val mmOutStream: OutputStream = mmSocket.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    private val activity = activity

    private var running = true

    override fun run() {
        // Keep listening to the InputStream until an exception occurs.
        while (running) {
            mmInStream.read(mmBuffer)

            activity.runOnUiThread {
                val serialText = activity.findViewById<TextView>(R.id.serialText)
                val wkCharas = Character.toChars(mmBuffer[0].toInt())
                val wkText = serialText.text.toString() + wkCharas[0]
                serialText.text = wkText
            }
        }
    }

    // Call this method from the main activity to shut down the connection.
    fun cancel() {
        try {
            // 無限ループを終了させてrunメソッドを終了させてスレッドを完結させる
            running = false
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the connect socket", e)
        }
    }
}