package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.w3c.dom.CharacterData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    val REQUEST_ENABLE_BT = 100

    lateinit var bluetoothAdapter : BluetoothAdapter
    lateinit var device: BluetoothDevice

    private lateinit var connectedThread: ConnectedThread


    lateinit var serialText : TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "この端末はBluetoothに対応していません", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        serialText = findViewById(R.id.serialText)

        val checkBt = findViewById<Button>(R.id.checkBt)
        checkBt.setOnClickListener {

            // ペア済のBluetooth機器の情報取得
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                // ペア済のデバイスを設定
                this.device = device

                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                Toast.makeText(this, "name:"+deviceName+" address:"+deviceHardwareAddress, Toast.LENGTH_SHORT).show()

                val deviceUuid = device.uuids[0].uuid
                Toast.makeText(this, "uuid:"+deviceUuid.toString(), Toast.LENGTH_LONG).show()

                // 接続開始！！
                ConnectThread(device).start()

            }
        }





    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket!!.connect()
            connectedThread = ConnectedThread(mmSocket!!)
            connectedThread.start()

            /*
            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                // 通信開始！！
                connectedThread = ConnectedThread(socket)
                connectedThread.start()

            }
            */
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                // Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()
            //val intList : ArrayList<Int> = ArrayList()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    // Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                this@MainActivity.runOnUiThread{

                    val wkCharas = Character.toChars(mmBuffer[0].toInt())
                    val wkText = serialText.text.toString() + wkCharas[0]
                    serialText.text = wkText
                }

                // Send the obtained bytes to the UI activity.
                /*
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
                 */
            }

            this@MainActivity.runOnUiThread{
                serialText.text = "おわり"
            }
        }

        /*
        // スマホからBluetooth通信機器への情報送信
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                // Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()
        }
        */

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                // Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        connectedThread.cancel()
    }
}