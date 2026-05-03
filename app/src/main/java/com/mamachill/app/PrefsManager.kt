package com.mamachill.app

import android.content.Context

object PrefsManager {
    private const val PREFS = "mamachill_prefs"
    private const val KEY_API_KEY = "elevenlabs_api_key"
    private const val KEY_VOICE_ID = "voice_id"
    private const val KEY_VOICE_NAME = "voice_name"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getApiKey(ctx: Context): String = prefs(ctx).getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(ctx: Context, key: String) = prefs(ctx).edit().putString(KEY_API_KEY, key).apply()

    fun getVoiceId(ctx: Context): String = prefs(ctx).getString(KEY_VOICE_ID, "") ?: ""
    fun setVoiceId(ctx: Context, id: String) = prefs(ctx).edit().putString(KEY_VOICE_ID, id).apply()

    fun getVoiceName(ctx: Context): String = prefs(ctx).getString(KEY_VOICE_NAME, "") ?: ""
    fun setVoiceName(ctx: Context, name: String) = prefs(ctx).edit().putString(KEY_VOICE_NAME, name).apply()

    fun hasVoice(ctx: Context) = getVoiceId(ctx).isNotEmpty()
}
