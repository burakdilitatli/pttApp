package com.example.pttapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okio.ByteString
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import android.media.AudioTrack
import android.media.AudioManager




class MainActivity : AppCompatActivity() {
    private val sampleRate = 16000

    // ➋ Kanal konfigürasyonu
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    // ➌ Veri formatı
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT


    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate, channelConfig, audioFormat
    )
    private val wsUrl = "ws://192.168.1.100:8080"

    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("PTT", "WS bağlantısı açıldı")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // ➌ Gelen paketleri ByteArray’e çevir ve çal
            val data = bytes.toByteArray()
            audioTrack.write(data, 0, data.size)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("PTT", "WS hatası", t)
        }
    }

    private lateinit var audioTrack: AudioTrack

    private lateinit var ws: WebSocket

    private val bufferSize = minBufferSize * 2
    private lateinit var audioRecord: AudioRecord
    private var isRecording = false
    val client = OkHttpClient()



    private fun mikrofonIzniniKontrolEt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        // AudioRecord başlatılıyor
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat, bufferSize
        )
        audioRecord.startRecording()
        isRecording = true

        // Yeni bir Thread içinde döngü
        Thread {
            val buf = ByteArray(bufferSize)
            while (isRecording) {
                // ← burada read ve send entegre ediliyor
                val read = audioRecord.read(buf, 0, buf.size)
                if (read > 0) {
                    val packet = buf.toByteString(0, read)
                    ws.send(packet)
                    Log.d("PTT", "Gönderilen byte: $read")
                }
            }
        }.start()
    }

    private fun stopRecording() {
        if (isRecording) {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
            Log.d("PTT", "Kayıt durdu")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mikrofonIzniniKontrolEt()

        // tek satırda WebSocket başlat
        val req = Request.Builder()
            .url(wsUrl)
            .build()
        ws = client.newWebSocket(req, wsListener)

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,    // çıkış kanalı
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack.play()

        val btn = findViewById<Button>(R.id.btnRecord)
        btn.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> startRecording()
                MotionEvent.ACTION_UP   -> stopRecording()
            }
            true
        }
    }

}

