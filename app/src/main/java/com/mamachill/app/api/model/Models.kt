package com.mamachill.app.api.model

import com.google.gson.annotations.SerializedName

data class VoiceResponse(
    @SerializedName("voice_id") val voiceId: String,
    @SerializedName("name") val name: String
)

data class VoicesListResponse(
    @SerializedName("voices") val voices: List<VoiceItem>
)

data class VoiceItem(
    @SerializedName("voice_id") val voiceId: String,
    @SerializedName("name") val name: String
)

data class TtsRequest(
    @SerializedName("text") val text: String,
    @SerializedName("model_id") val modelId: String = "eleven_flash_v2_5",
    @SerializedName("voice_settings") val voiceSettings: VoiceSettings = VoiceSettings()
)

data class VoiceSettings(
    @SerializedName("stability") val stability: Float = 0.5f,
    @SerializedName("similarity_boost") val similarityBoost: Float = 0.75f
)
