package com.mamachill.app.api

import com.mamachill.app.api.model.TtsRequest
import com.mamachill.app.api.model.VoiceResponse
import com.mamachill.app.api.model.VoicesListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ElevenLabsService {

    @Multipart
    @POST("v1/voices/add")
    suspend fun createIvcVoice(
        @Header("xi-api-key") apiKey: String,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<VoiceResponse>

    @Streaming
    @POST("v1/text-to-speech/{voice_id}")
    suspend fun textToSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String,
        @Body request: TtsRequest,
        @Query("output_format") outputFormat: String = "mp3_44100_128"
    ): Response<ResponseBody>

    @GET("v1/voices")
    suspend fun getVoices(
        @Header("xi-api-key") apiKey: String
    ): Response<VoicesListResponse>
}
