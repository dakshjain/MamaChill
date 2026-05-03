package com.mamachill.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private var selectedAudioFile: File? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var countDownTimer: CountDownTimer? = null
    private var isRecording = false

    private val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val file = copyUriToTempFile(uri)
            selectedAudioFile = file
            binding.tvSelectedFile.text = uri.lastPathSegment?.substringAfterLast('/') ?: "audio file"
            binding.tvSelectedFile.visibility = View.VISIBLE
            stopRecordingUi()
        }
    }

    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else toast("Microphone permission is required to record")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Clone Your Voice"

        binding.etApiKey.setText(PrefsManager.getApiKey(this))

        val currentVoice = PrefsManager.getVoiceName(this)
        if (currentVoice.isNotEmpty()) {
            binding.tvCurrentVoice.text = "Current voice: $currentVoice"
            binding.tvCurrentVoice.visibility = View.VISIBLE
        }

        binding.btnPickAudio.setOnClickListener {
            if (isRecording) stopRecording()
            audioPicker.launch("audio/*")
        }

        binding.btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        binding.btnStopRecord.setOnClickListener { stopRecording() }

        binding.btnClone.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            val voiceName = binding.etVoiceName.text.toString().trim()

            if (apiKey.isEmpty()) { toast("Enter your ElevenLabs API key"); return@setOnClickListener }
            if (voiceName.isEmpty()) { toast("Enter a name for your voice"); return@setOnClickListener }
            if (selectedAudioFile == null) { toast("Pick or record an audio sample first"); return@setOnClickListener }

            if (isRecording) stopRecording()
            PrefsManager.setApiKey(this, apiKey)
            cloneVoice(apiKey, voiceName)
        }
    }

    private fun startRecording() {
        recordingFile = File(cacheDir, "voice_record_${System.currentTimeMillis()}.m4a")
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setMaxDuration(15_000)
            setOutputFile(recordingFile!!.absolutePath)
            prepare()
            start()
        }
        isRecording = true

        binding.btnRecord.text = "Recording..."
        binding.cardRecording.visibility = View.VISIBLE
        binding.tvSelectedFile.visibility = View.GONE

        countDownTimer = object : CountDownTimer(15_000, 1_000) {
            override fun onTick(remaining: Long) {
                binding.tvRecordingStatus.text = "Recording... ${remaining / 1000 + 1}s"
            }
            override fun onFinish() { stopRecording() }
        }.start()

        mediaRecorder?.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stopRecording()
        }
    }

    private fun stopRecording() {
        countDownTimer?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false

        val file = recordingFile
        if (file != null && file.exists() && file.length() > 0) {
            selectedAudioFile = file
            binding.tvSelectedFile.text = "Recorded sample (${file.length() / 1024}KB)"
            binding.tvSelectedFile.visibility = View.VISIBLE
            toast("Recording saved — ready to clone")
        } else {
            toast("Recording failed, please try again")
        }

        stopRecordingUi()
    }

    private fun stopRecordingUi() {
        binding.btnRecord.text = "Record Voice (15s)"
        binding.cardRecording.visibility = View.GONE
    }

    private fun cloneVoice(apiKey: String, voiceName: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val file = selectedAudioFile!!
                val mimeType = if (file.extension == "m4a") "audio/m4a" else "audio/*"
                val namePart = voiceName.toRequestBody("text/plain".toMediaType())
                val filePart = MultipartBody.Part.createFormData(
                    "files", file.name,
                    file.asRequestBody(mimeType.toMediaType())
                )

                val response = withContext(Dispatchers.IO) {
                    ElevenLabsClient.service.createIvcVoice(apiKey, namePart, filePart)
                }

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
        binding.btnRecord.isEnabled = !loading
        binding.btnClone.text = if (loading) "Cloning..." else "Clone Voice"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRecording()
        countDownTimer?.cancel()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
