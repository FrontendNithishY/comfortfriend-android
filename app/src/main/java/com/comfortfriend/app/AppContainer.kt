package com.comfortfriend.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import com.comfortfriend.app.data.db.AppDatabase
import com.comfortfriend.app.data.prefs.SettingsStore
import com.comfortfriend.app.data.remote.GroqApi
import com.comfortfriend.app.data.remote.GroqClient
import com.comfortfriend.app.data.secure.SecureKeyStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Tiny manual dependency container (keeps the app dependency-light and easy to audit). */
class AppContainer(private val context: Context) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    val db: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "comfortfriend.db").build()
    }
    val settings = SettingsStore(context)
    val keys = SecureKeyStore(context)

    private val http by lazy { GroqClient.buildHttpClient() }
    private val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl(GroqClient.BASE_URL)
            .client(http)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GroqApi::class.java)
    }

    val data: com.comfortfriend.app.data.DataManager by lazy { com.comfortfriend.app.data.DataManager(db, json) }

    val groq: GroqClient by lazy { GroqClient(http, api, json, keys, ::isOnline) }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
