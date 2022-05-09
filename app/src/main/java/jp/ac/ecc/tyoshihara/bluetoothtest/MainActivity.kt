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

    private var bluetoothAdapter : BluetoothAdapter? = null
    private var device: BluetoothDevice? = null
    private var BTConnectThred: BluetoothConnectThread? = null

    lateinit var serialText : TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // デバイスがBluetoothに対応していない場合は以降の処理は行わない
            Toast.makeText(this, "この端末はBluetoothに対応していません", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
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

                val deviceName = device.name // デバイス名
                val deviceHardwareAddress = device.address // MACアドレス
                Log.i(TAG, "deviceName:"+deviceName)
                Log.i(TAG, "macAddress:"+deviceHardwareAddress)
                Log.i(TAG, "uudi:"+device.uuids[0].uuid.toString())
                Toast.makeText(this, "ペアリング済のデバイス("+device.name+")が見つかりました", Toast.LENGTH_SHORT).show()
            }
        }

        val connectBt = findViewById<Button>(R.id.connectBt)
        connectBt.setOnClickListener {

            device?.also{
                serialText.text = ""
                Toast.makeText(this, "ペアリング済のデバイス("+it.name+")に接続しました", Toast.LENGTH_LONG).show()
                connectBt.isEnabled = false // 二重接続を防ぐためボタンを無効化

                // Bluetooth接続を行うスレッドを開始
                BTConnectThred = BluetoothConnectThread(it, this)
                BTConnectThred?.also {
                    it.start()
                }
            }?: Toast.makeText(this, "ペアリングチェックを先にしてください。", Toast.LENGTH_SHORT).show()
        }

        val releaseBt = findViewById<Button>(R.id.releaseBt)
        releaseBt.setOnClickListener {

            BTConnectThred?.also {
                it.release()
                connectBt.isEnabled = true // 接続ボタンを有効化に戻す
                Toast.makeText(this, "デバイス(" + device?.name + ")の接続を終了します", Toast.LENGTH_LONG).show()
            }?: Toast.makeText(this, "接続状態ではありません", Toast.LENGTH_LONG).show()

            BTConnectThred = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BTConnectThred?.also {
            it.release()
        }
        BTConnectThred = null
        device = null
    }
}