package jp.ac.ecc.tyoshihara.bluetoothtest

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log

class HeadsetReceiver(handler: Handler) : BroadcastReceiver() {

    val TAG = "HeadsetReceiver"

    companion object {
        const val HEADSET_AUDIO_ON = 1
        const val HEADSET_AUDIO_OFF = 2
        const val HEADSET_DISCONNECT = 3
        const val A2DP_PLAY_ON = 4
        const val A2DP_PLAY_OFF = 5
        const val A2DP_DISCONNECT = 6
    }
    val handler = handler

    override fun onReceive(context: Context, intent: Intent) {
        val action : String? = intent.action

        when(action){
            BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> {
                Log.d(TAG, "ACTION_VENDOR_SPECIFIC_HEADSET_EVENT")
            }
            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                when(state){
                    BluetoothHeadset.STATE_AUDIO_CONNECTING ->{
                        val msg = "STATE_AUDIO_CONNECTING"
                        val profileMsg = handler.obtainMessage(HEADSET_AUDIO_ON)
                        profileMsg.obj = msg
                        profileMsg.sendToTarget()
                        Log.d(TAG, msg)
                    }
                    BluetoothHeadset.STATE_AUDIO_CONNECTED ->{
                        val msg = "STATE_AUDIO_CONNECTED"
                        val profileMsg = handler.obtainMessage(HEADSET_AUDIO_OFF)
                        profileMsg.obj = msg
                        profileMsg.sendToTarget()
                        Log.d(TAG, msg)
                    }
                }
            }
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                when(state){
                    BluetoothHeadset.STATE_DISCONNECTED -> {
                        val profileMsg = handler.obtainMessage(HEADSET_DISCONNECT)
                        profileMsg.sendToTarget()
                    }
                }
            }
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                when(state){
                    BluetoothA2dp.STATE_PLAYING ->{
                        val msg = "STATE_PLAYING"
                        val profileMsg = handler.obtainMessage(A2DP_PLAY_ON)
                        profileMsg.obj = msg
                        profileMsg.sendToTarget()
                        Log.d(TAG, msg)
                    }
                    BluetoothA2dp.STATE_NOT_PLAYING -> {
                        val msg = "STATE_NOT_PLAYING"
                        val profileMsg = handler.obtainMessage(A2DP_PLAY_OFF)
                        profileMsg.obj = msg
                        profileMsg.sendToTarget()
                        Log.d(TAG, msg)
                    }
                }
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                when(state){
                    BluetoothHeadset.STATE_DISCONNECTED -> {
                        val profileMsg = handler.obtainMessage(A2DP_DISCONNECT)
                        profileMsg.sendToTarget()
                    }
                }
            }
        }
    }
}