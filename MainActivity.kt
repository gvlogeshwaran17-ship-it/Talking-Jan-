package com.talkingjan.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var characterView: JanCharacterView
    private lateinit var tvStatus: TextView
    private lateinit var voiceEngine: VoiceEngine
    private var micActive = false

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceEngine()
        } else {
            tvStatus.text = "🔇 Mic permission needed to talk"
            Toast.makeText(this, "Enable microphone so Jan can repeat you!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        characterView = findViewById(R.id.janCharacterView)
        tvStatus = findViewById(R.id.tvStatus)

        voiceEngine = VoiceEngine { state, level ->
            runOnUiThread {
                characterView.setVoiceState(state, level)
                tvStatus.text = when (state) {
                    VoiceEngine.State.IDLE -> "🎤 Talk to Jan!"
                    VoiceEngine.State.LISTENING -> "👂 Jan is listening..."
                    VoiceEngine.State.SPEAKING -> "🗣️ Jan is talking!"
                }
            }
        }

        characterView.onInteraction = { msg ->
            tvStatus.text = msg
        }

        findViewById<Button>(R.id.btnDrink).setOnClickListener {
            characterView.triggerReaction(JanCharacterView.Reaction.DRINK, 1400, "☕")
            SoundEffects.playGulp()
            tvStatus.text = "☕ Sipping coffee!"
        }

        findViewById<Button>(R.id.btnEat).setOnClickListener {
            characterView.triggerReaction(JanCharacterView.Reaction.EAT, 1400, "🍰")
            SoundEffects.playCrunch()
            tvStatus.text = "🍰 Yum! Tasty snack!"
        }

        findViewById<Button>(R.id.btnCymbal).setOnClickListener {
            characterView.triggerReaction(JanCharacterView.Reaction.DIZZY, 1200, "💥")
            SoundEffects.playCymbal()
            tvStatus.text = "🥁 Whoa! Loud crash!"
        }

        findViewById<Button>(R.id.btnDance).setOnClickListener {
            characterView.triggerReaction(JanCharacterView.Reaction.DANCE, 1800, "🎵")
            SoundEffects.playDanceBeat()
            tvStatus.text = "🎵 Dance party!"
        }

        findViewById<Button>(R.id.btnMic).setOnClickListener {
            if (micActive) {
                voiceEngine.stop()
                micActive = false
                tvStatus.text = "🔇 Mic paused (tap 🎙️)"
            } else {
                requestMicAndStart()
            }
        }

        requestMicAndStart()
    }

    private fun requestMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceEngine()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceEngine() {
        micActive = true
        voiceEngine.start()
        tvStatus.text = "🎤 Talk to Jan!"
    }

    override fun onPause() {
        super.onPause()
        voiceEngine.stop()
        micActive = false
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED && !micActive
        ) {
            startVoiceEngine()
        }
    }
}
