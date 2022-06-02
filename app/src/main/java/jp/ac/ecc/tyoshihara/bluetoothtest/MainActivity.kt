package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    val REQUEST_ENABLE_BT = 100

    private var bluetoothAdapter : BluetoothAdapter? = null
    private var device: BluetoothDevice? = null
    private var BTConnectThred: BluetoothConnectThread? = null

    private lateinit var bluetoothReceiver : BluetoothDeviceReceiver
    private lateinit var serialText : TextView
    private lateinit var deviceText : TextView
    private lateinit var searchBt : Button

    // Bluetoothデバイスリスト
    private var deviceArray : ArrayList<BluetoothDevice> = arrayListOf()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceText = findViewById(R.id.deviceText)
        serialText = findViewById(R.id.serialText)

        // メインスレッドのハンドラー生成
        val handler = object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when(msg.what){
                    // Bluetooth通信スレッドの受信受け取り処理
                    BluetoothConnectedThread.SEND_MSG -> {
                        serialText.text = serialText.text.toString() + msg.obj.toString()
                    }

                    // Bluetoothデバイス検知開始
                    BluetoothDeviceReceiver.DISCOVERY_START -> {
                        // serialText.text = serialText.text.toString() + msg.obj.toString() + "\n"
                    }

                    // Bluetoothデバイス見つかった
                    BluetoothDeviceReceiver.DEVICE_FOUND -> {
                        val device = msg.obj as BluetoothDevice
                        serialText.text = serialText.text.toString() + "検知デバイス：" + device.name + "\n"
                        deviceArray.add(device)
                    }

                    // Bluetoothデバイス検知終了
                    BluetoothDeviceReceiver.DISCOVERY_END -> {
                        serialText.text = serialText.text.toString() + msg.obj.toString() + "\n"
                        searchBt.isEnabled = true
                    }
                }
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // デバイスがBluetoothに対応していない場合は以降の処理は行わない
            Toast.makeText(this, "この端末はBluetoothに対応していません", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        bluetoothAdapter!!.cancelDiscovery() // デバイス検知はボタンきっかけでのみ行う

        // Bluetooth有効化チェック
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // ブロードキャストレシーバー生成(位置情報を内部的に利用するためパーミッション必要）
        bluetoothReceiver = BluetoothDeviceReceiver(handler)
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        // Bluetoothデバイス検索
        searchBt = findViewById(R.id.searchBt)
        searchBt.setOnClickListener {

            // デバイス検知を開始(検知後の処理はブロードキャストレシーバで行う)
            bluetoothAdapter!!.startDiscovery()
            deviceArray.clear() // デバイスリストクリア

            searchBt.isEnabled = false //二重処理防止
            serialText.text = "デバイス検索中\n"
        }

        // ペアリングチェック
        val checkBt = findViewById<Button>(R.id.checkBt)
        checkBt.setOnClickListener {

            Toast.makeText(this, "ペアリング済のデバイスを確認します", Toast.LENGTH_SHORT).show()
            serialText.text = "ペアリングチェック開始\n"
            deviceArray.clear() // デバイスリストクリア

            // ペア済のBluetooth機器の情報取得
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->

                val deviceName = device.name // デバイス名
                val deviceHardwareAddress = device.address // MACアドレス
                Log.i(TAG, "deviceName:"+deviceName)
                Log.i(TAG, "macAddress:"+deviceHardwareAddress)
                Log.i(TAG, "uudi:"+device.uuids[0].uuid.toString())

                serialText.text = serialText.text.toString() + "ペア済デバイス：" + deviceName + "\n"
                // デバイスリストに追加
                deviceArray.add(device)
            }
            serialText.text = serialText.text.toString() + "ペアリングチェック終了"
        }

        // Bluetooth接続 開始処理
        val connectBt = findViewById<Button>(R.id.connectBt)
        connectBt.setOnClickListener {

            device?.also{
                serialText.text = ""
                Toast.makeText(this, "デバイス("+it.name+")に接続しました", Toast.LENGTH_SHORT).show()
                connectBt.isEnabled = false // 二重接続を防ぐためボタンを無効化

                // Bluetooth接続を行うスレッドを開始
                BTConnectThred = BluetoothConnectThread(it, handler)
                BTConnectThred?.also {
                    it.start()
                }
            }?: Toast.makeText(this, "デバイスを選択してください", Toast.LENGTH_SHORT).show() // nullの時
        }

        // Bluetooth接続 解除処理
        val releaseBt = findViewById<Button>(R.id.releaseBt)
        releaseBt.setOnClickListener {

            BTConnectThred?.also {
                it.release() // スレッド解除処理　呼び出し
                deviceText.text = getString(R.string.nonDevice)
                connectBt.isEnabled = true // 接続ボタンを有効化に戻す
                Toast.makeText(this, "デバイス(" + device?.name + ")の接続を終了します", Toast.LENGTH_SHORT).show()
            }?: Toast.makeText(this, "接続状態ではありません", Toast.LENGTH_SHORT).show() // nullの時

            // スレッド、選択デバイスを初期化
            BTConnectThred = null
            device = null
        }

        // Bluetoothデバイス一覧表示
        val bluetoothFAB = findViewById<FloatingActionButton>(R.id.bluetoothListButton)
        bluetoothFAB.setOnClickListener {

            device?.also {
                Toast.makeText(this, it.name + "に接続中です", Toast.LENGTH_SHORT).show()
            }?:run{
                // デバイス選択されていない時のみ、リストから選べるようにする

                // ダイアログ用にデバイス名の配列を生成
                val deviceNameArray : Array<String?> = arrayOfNulls(deviceArray.size)
                for((index, device) in deviceArray.withIndex()){
                    deviceNameArray.set(index, device.name)
                }

                // デバイスリストが無い場合はダイアログを出さない
                if(deviceArray.isEmpty()){
                    Toast.makeText(this, "Bluetooth機器がありません", Toast.LENGTH_SHORT).show()
                    return@run
                }

                val builder = AlertDialog.Builder(this)
                builder.setTitle("ペアリングを行うデバイスを選択してください")
                    .setItems(deviceNameArray,
                        DialogInterface.OnClickListener { dialog, which ->
                            // whichに選択したインデックス番号が格納されている
                            device = deviceArray.get(which)
                            device?.also {
                                it.createBond() // ペアリングを実施
                                deviceText.text = it.name + "："
                            }
                            return@OnClickListener
                        })
                builder.create().show()
            }
        }


        // メッセージ表示領域クリア
        val clearBt = findViewById<Button>(R.id.clearBt)
        clearBt.setOnClickListener {
            serialText.text = ""
        }

        // Bluetooth通信　メッセージ送信
        val writeEt = findViewById<EditText>(R.id.writeEt)
        val writeBt = findViewById<Button>(R.id.writeBt)
        writeBt.setOnClickListener {
            // スレッドにメッセージを送る
            BTConnectThred?.also {
                it.msgWrite(writeEt.text.toString())
                writeEt.editableText.clear()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Bluetooth接続に関する処理を終了させる
        BTConnectThred?.also {
            it.release()
        }
        BTConnectThred = null
        device = null
        // ブロードキャストレシーバを受信を停止
        unregisterReceiver(bluetoothReceiver)
    }

}