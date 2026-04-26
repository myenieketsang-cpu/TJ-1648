package com.example.sampleapp

import android.Manifest
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder
import android.net.InetAddresses
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private var isStreaming = false
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ipField = findViewById<EditText>(R.id.ipField)
        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {
            if (!isStreaming) {
                val targetIp = ipField.text.toString()
                startStreaming(targetIp)
                startButton.text = "Stop"
            } else {
                stopStreaming()
                startButton.text = "Start"
            }
        }
    }

    private fun startStreaming(ip: String) {
        isStreaming = true
        job = CoroutineScope(Dispatchers.IO).launch {
            val socket = DatagramSocket()
            val address = InetAddress.getByName(ip)

            val config = AudioPlaybackCaptureConfiguration.Builder(MediaRecorder.AudioSource.MEDIA)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .build()

            val recorder = android.media.AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(4096)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            recorder.startRecording()
            val buffer = ByteArray(4096)

            while (isStreaming) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val packet = DatagramPacket(buffer, read, address, 59200)
                    socket.send(packet)
                }
            }

            recorder.stop()
            recorder.release()
            socket.close()
        }
    }

    private fun stopStreaming() {
        isStreaming = false
        job?.cancel()
    }
}
