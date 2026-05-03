package com.mamachill.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mamachill.app.api.ElevenLabsClient
import com.mamachill.app.databinding.ActivityVoiceSetupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class VoiceSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceSetupBinding
    private var selectedAudioUri: Uri? = null

    private val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        selectedAudioUri = uri
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "audio file"
        binding.tvSelectedFile.text = name
        binding.tvSelectedFile.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Clone Your Voice"

        // Pre-fill saved API key
        binding.etApiKey.setText(PrefsManager.getApiKey(this))

        // Show current voice if already cloned
        val currentVoice = PrefsManager.getVoiceName(this)
        if (currentVoice.isNotEmpty()) {
            binding.tvCurrentVoice.text = "Current voice: $currentVoice"
            binding.tvCurrentVoice.visibility = View.VISIBLE
        }

        binding.btnPickAudio.setOnClickListener {
            audioPicker.launch("audio/*")
        }

        binding.btnClone.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            val voiceName = binding.etVoiceName.text.toString().trim()

            if (apiKey.isEmpty()) { toast("Enter your ElevenLabs API key"); return@setOnClickListener }
            if (voiceName.isEmpty()) { toast("Enter a name for your voice"); return@setOnClickListener }
            if (selectedAudioUri == null) { toast("Pick an audio file first"); return@setOnClickListener }

            PrefsManager.setApiKey(this, apiKey)
            cloneVoice(apiKey, voiceName)
        }
    }

    private fun cloneVoice(apiKey: String, voiceName: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val tempFile = copyUriToTempFile(selectedAudioUri!!)

                val namePart = voiceName.toRequestBody("text/plain".toMediaType())
                val filePart = MultipartBody.Part.createFormData(
                    "files", tempFile.name,
                    tempFile.asRequestBody("audio/*".toMediaType())
                )

                val response = withContext(Dispatchers.IO) {
                    ElevenLabsClient.service.createIvcVoice(apiKey, namePart, filePart)
                }

                tempFile.delete()

                if (response.isSuccessful && response.body() != null) {
                    val voice = response.body()!!
                    PrefsManager.setVoiceId(this@VoiceSetupActivity, voice.voiceId)
                    PrefsManager.setVoiceName(this@VoiceSetupActivity, voiceName)
                    toast("Voice \"$voiceName\" cloned successfully!")
                    setResult(RESULT_OK)
                    finish()
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

    private suspend fun copyUriToTempFile(uri: Uri): File = withContext(Dispatchers.IO) {
        val tempFile = File(cacheDir, "voice_sample_${System.currentTimeMillis()}.mp3")
        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        tempFile
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnClone.isEnabled = !loading
        binding.btnPickAudio.isEnabled = !loading
        binding.btnClone.text = if (loading) "Cloning..." else "Clone Voice"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
