package jp.ac.ecc.tyoshihara.bluetoothtest

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class BluetoothConnectedThread(mmSocket: BluetoothSocket, handler: Handler) : Thread() {

    val TAG = "BluetoothConnectedThread"

    companion object {
        const val SEND_MSG = 1
    }

    private val mmInStream: InputStream = mmSocket.inputStream
    private val mmOutStream: OutputStream = mmSocket.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    private val handler = handler

    private var running = true

    override fun run() {
        // 中止命令が呼び出されるまで処理を継続させる
        while (running) {

            // バイトデータが存在しうる場合のみ読み込みを行う
            if(mmInStream.available() != 0) {
                mmInStream.read(mmBuffer)

                val wkCharas = Character.toChars(mmBuffer[0].toInt()) //文字コードから文字に変換
                val readMsg = handler.obtainMessage(SEND_MSG)
                readMsg.obj = wkCharas[0] // Char配列なので先頭文字取得
                readMsg.sendToTarget()
            }
        }
    }

    // メッセージ送信処理
    fun msgWrite(message : String){
        val msgBytes = message.toByteArray()
        mmOutStream.write(msgBytes)
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