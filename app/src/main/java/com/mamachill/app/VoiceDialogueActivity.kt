package com.mamachill.app

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mamachill.app.api.ElevenLabsClient
import com.mamachill.app.api.model.TtsRequest
import com.mamachill.app.databinding.ActivityVoiceDialogueBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VoiceDialogueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceDialogueBinding
    private var generatedFilePath: String = ""
    private var previewPlayer: MediaPlayer? = null

    private val voiceSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) updateVoiceHeader()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceDialogueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Voice Dialogue"

        updateVoiceHeader()

        binding.btnChangeVoice.setOnClickListener {
            voiceSetupLauncher.launch(Intent(this, VoiceSetupActivity::class.java))
        }

        binding.btnGenerate.setOnClickListener {
            val text = binding.etDialogue.text.toString().trim()
            if (text.isEmpty()) { toast("Enter your alarm dialogue"); return@setOnClickListener }
            if (!PrefsManager.hasVoice(this)) {
                toast("Clone a voice first")
                voiceSetupLauncher.launch(Intent(this, VoiceSetupActivity::class.java))
                return@setOnClickListener
            }
            generateAudio(text)
        }

        binding.btnPreview.setOnClickListener { previewAudio() }

        binding.btnUseAsTone.setOnClickListener {
            if (generatedFilePath.isEmpty()) { toast("Generate audio first"); return@setOnClickListener }
            setResult(RESULT_OK, Intent().putExtra(EXTRA_AUDIO_PATH, generatedFilePath))
            finish()
        }
    }

    private fun updateVoiceHeader() {
        val voiceName = PrefsManager.getVoiceName(this)
        if (voiceName.isNotEmpty()) {
            binding.tvVoiceName.text = "Voice: $voiceName"
            binding.tvNoVoice.visibility = View.GONE
            binding.btnGenerate.isEnabled = true
        } else {
            binding.tvVoiceName.text = "No voice cloned"
            binding.tvNoVoice.visibility = View.VISIBLE
            binding.btnGenerate.isEnabled = false
        }
    }

    private fun generateAudio(text: String) {
        setLoading(true)
        val voiceId = PrefsManager.getVoiceId(this)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ElevenLabsClient.service.textToSpeech(
                        PrefsManager.API_KEY, voiceId, TtsRequest(text)
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val audioFile = withContext(Dispatchers.IO) {
                        val file = File(filesDir, "alarm_dialogue_${System.currentTimeMillis()}.mp3")
                        response.body()!!.byteStream().use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        file
                    }
                    generatedFilePath = audioFile.absolutePath
                    binding.tvGenerated.text = "Audio ready: ${audioFile.name}"
                    binding.tvGenerated.visibility = View.VISIBLE
                    binding.btnPreview.visibility = View.VISIBLE
                    binding.btnUseAsTone.visibility = View.VISIBLE
                    toast("Audio generated!")
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    toast("Failed: $error")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun previewAudio() {
        if (generatedFilePath.isEmpty()) return
        previewPlayer?.release()
        previewPlayer = MediaPlayer().apply {
            setDataSource(generatedFilePath)
            prepare()
            start()
            setOnCompletionListener { it.release(); previewPlayer = null }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnGenerate.isEnabled = !loading
        binding.btnGenerate.text = if (loading) "Generating..." else "Generate Audio"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        previewPlayer?.release()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
    }
}
