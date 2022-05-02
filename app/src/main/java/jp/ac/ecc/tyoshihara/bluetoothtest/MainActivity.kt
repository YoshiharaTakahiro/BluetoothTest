package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    val REQUEST_ENABLE_BT = 100

    lateinit var bluetoothAdapter : BluetoothAdapter
    lateinit var device: BluetoothDevice

    private lateinit var BTConnectThred: BluetoothConnectThread

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
                Log.i(TAG, "deviceName:"+deviceName)
                Log.i(TAG, "macAddress:"+deviceHardwareAddress)
                Log.i(TAG, "uudi:"+device.uuids[0].uuid.toString())
                Toast.makeText(this, "ペアリング済のデバイス("+device.name+")が見つかりました", Toast.LENGTH_SHORT).show()
            }
        }

        val connectBt = findViewById<Button>(R.id.connectBt)
        connectBt.setOnClickListener {

            if(::device.isInitialized){
                // 接続開始
                serialText.text = ""
                Toast.makeText(this, "ペアリング済のデバイス("+device.name+")に接続しました", Toast.LENGTH_LONG).show()
                connectBt.isEnabled = false
                BTConnectThred = BluetoothConnectThread(device, this)
                BTConnectThred.start()
            }else{
                Toast.makeText(this, "ペアリングチェックを先にしてください。", Toast.LENGTH_LONG).show()
            }
        }

        val releaseBt = findViewById<Button>(R.id.releaseBt)
        releaseBt.setOnClickListener {
            if(::BTConnectThred.isInitialized){
                // 接続解除
                Toast.makeText(this, "デバイス(" + device.name + ")に接続を終了します", Toast.LENGTH_LONG).show()
                connectBt.isEnabled = true
                BTConnectThred.cancel()
            }else{
                Toast.makeText(this, "接続状態ではありません", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BTConnectThred.cancel()
    }
}