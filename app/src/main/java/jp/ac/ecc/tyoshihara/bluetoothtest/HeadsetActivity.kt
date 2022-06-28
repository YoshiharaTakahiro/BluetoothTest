package jp.ac.ecc.tyoshihara.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.IntentFilter
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import com.google.android.material.floatingactionbutton.FloatingActionButton

@SuppressLint("MissingPermission")
class HeadsetActivity : AppCompatActivity() {

    private var device: BluetoothDevice? = null
    private lateinit var bluetoothManager : BluetoothManager
    private var bluetoothAdapter : BluetoothAdapter? = null

    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothA2dp: BluetoothA2dp? = null

    private lateinit var headsetReceiver : HeadsetReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_headset)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter!!.cancelDiscovery() // デバイス検知はボタンきっかけでのみ行う

        val headsetTx = findViewById<TextView>(R.id.headsetText)
        val a2dpTx = findViewById<TextView>(R.id.a2dpText)

        // 接続ボタンは非活性状態にする
        val connectBt = findViewById<Button>(R.id.connectButton)
        val disconnectBt = findViewById<Button>(R.id.disconnectButton)
        connectBt.isEnabled = false
        disconnectBt.isEnabled = false

        // 動画コントロールボタンは初期値非活性
        val playBt = findViewById<FloatingActionButton>(R.id.playButton)
        val pauseBt = findViewById<FloatingActionButton>(R.id.pauseButton)
        val stopBt = findViewById<FloatingActionButton>(R.id.stopButton)
        playBt.isEnabled = false
        pauseBt.isEnabled = false
        stopBt.isEnabled = false

        val a2dpCheckBt = findViewById<Button>(R.id.a2dpCheckButton)
        a2dpCheckBt.isEnabled = false

        // メインスレッドのハンドラー生成
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when(msg.what){
                    HeadsetReceiver.HEADSET_AUDIO_ON,
                    HeadsetReceiver.HEADSET_AUDIO_OFF -> {
                        headsetTx.text = msg.obj.toString()
                    }
                    HeadsetReceiver.A2DP_PLAY_ON,
                    HeadsetReceiver.A2DP_PLAY_OFF-> {
                        a2dpTx.text = msg.obj.toString()
                    }
                    HeadsetReceiver.HEADSET_DISCONNECT -> {
                        headsetTx.text = "STATE_DISCONNECTED"
                        connectBt.isEnabled = false
                        disconnectBt.isEnabled = false
                    }
                    HeadsetReceiver.A2DP_DISCONNECT -> {
                        a2dpTx.text = "STATE_DISCONNECTED"
                        playBt.isEnabled = false
                        pauseBt.isEnabled = false
                        stopBt.isEnabled = false
                        a2dpCheckBt.isEnabled = false
                    }
                }
            }
        }

        val deviceText = findViewById<TextView>(R.id.deviceText)
        device = intent.getParcelableExtra<BluetoothDevice>("device")
        device?.also{
            deviceText.text = it.name
        }

        // ブロードキャストレシーバ
        headsetReceiver = HeadsetReceiver(handler)
        // HeadSetプロファイル関連(ヘッドセット＋ハンズフリープロファイル)
        registerReceiver(headsetReceiver, IntentFilter(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT))
        registerReceiver(headsetReceiver, IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED))
        registerReceiver(headsetReceiver, IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))
        // A2DPプロファイル関連(音声・動画再生プロファイル)
        registerReceiver(headsetReceiver, IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED))
        registerReceiver(headsetReceiver, IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED))


        // ローカル動画ファイルの読み取り
        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.samplevideo)
        videoView.setVideoURI(videoPath)


        playBt.setOnClickListener {
            videoView.start()
        }
        pauseBt.setOnClickListener {
            if(videoView.canPause()){
                videoView.pause()
            }
        }
        stopBt.setOnClickListener {
            videoView.stopPlayback()
            videoView.setVideoURI(videoPath)
        }

        a2dpCheckBt.setOnClickListener {
            bluetoothA2dp?.also{
                Toast.makeText(this, it.isA2dpPlaying(device).toString(), Toast.LENGTH_SHORT).show()
            }
        }

        // ヘッドセットのプロファイルリスナー生成
        val profileListener = object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = proxy as BluetoothHeadset
                }
                if(profile == BluetoothProfile.A2DP){
                    bluetoothA2dp = proxy as BluetoothA2dp
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null
                }
                if(profile == BluetoothProfile.A2DP){
                    bluetoothA2dp = null
                }
            }
        }

        // プロファイル設定
        bluetoothAdapter?.also{
            it.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)
            it.getProfileProxy(this, profileListener, BluetoothProfile.A2DP)
        }

        // 接続状況確認ボタン
        val stateBt = findViewById<Button>(R.id.stateButton)
        stateBt.setOnClickListener {
            bluetoothHeadset?.also {
                val state = it.getConnectionState(device)
                when(state){
                    BluetoothHeadset.STATE_CONNECTING -> headsetTx.text = "STATE_CONNECTING"
                    BluetoothHeadset.STATE_CONNECTED -> {
                        headsetTx.text = "STATE_CONNECTED"
                        connectBt.isEnabled = false
                        disconnectBt.isEnabled = true
                    }
                    BluetoothHeadset.STATE_DISCONNECTING -> headsetTx.text = "STATE_DISCONNECTING"
                    BluetoothHeadset.STATE_DISCONNECTED -> {
                        headsetTx.text = "STATE_DISCONNECTED"
                        connectBt.isEnabled = false
                        disconnectBt.isEnabled = false
                    }
                }
            }

            bluetoothA2dp?.also {
                val state = it.getConnectionState(device)
                when(state) {
                    BluetoothA2dp.STATE_CONNECTING -> a2dpTx.text = "STATE_CONNECTING"
                    BluetoothA2dp.STATE_CONNECTED -> {
                        a2dpTx.text = "STATE_CONNECTED"
                        playBt.isEnabled = true
                        pauseBt.isEnabled = true
                        stopBt.isEnabled = true
                        a2dpCheckBt.isEnabled = true
                    }
                    BluetoothA2dp.STATE_DISCONNECTING -> a2dpTx.text = "STATE_DISCONNECTING"
                    BluetoothA2dp.STATE_DISCONNECTED -> {
                        a2dpTx.text = "STATE_DISCONNECTED"
                        playBt.isEnabled = false
                        pauseBt.isEnabled = false
                        stopBt.isEnabled = false
                        a2dpCheckBt.isEnabled = false
                    }
                }
            }
        }

        // 接続ボタンでprofileを通じて接続を行う
        connectBt.setOnClickListener {
            bluetoothHeadset?.also {
                it.startVoiceRecognition(device) // なんか違う
            }
        }

        disconnectBt.setOnClickListener {
            bluetoothHeadset?.also {
                it.stopVoiceRecognition(device) // なんか違う
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // プロファイル解除
        bluetoothAdapter?.also{
            it.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
            it.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
        }

        unregisterReceiver(headsetReceiver)
    }
}