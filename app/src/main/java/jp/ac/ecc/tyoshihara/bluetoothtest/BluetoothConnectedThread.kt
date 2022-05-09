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
        // 中止命令が呼び出されるまで処理を継続させる
        while (running) {
            mmInStream.read(mmBuffer)

            if(activity.localClassName.equals("MainActivity")) {
                val mainActivity : MainActivity = activity as MainActivity
                mainActivity.runOnUiThread {
                    // 受信したデータを画面に表示させる
                    val wkCharas = Character.toChars(mmBuffer[0].toInt()) //文字コードから文字に変換
                    val wkText = mainActivity.serialText.text.toString() + wkCharas[0]
                    mainActivity.serialText.text = wkText
                }
           }
        }
    }

    // 通信中止メソッド
    fun cancel() {
        try {
            // 無限ループを抜け出しスレッドを完了させる
            running = false
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the connect socket", e)
        }
    }
}