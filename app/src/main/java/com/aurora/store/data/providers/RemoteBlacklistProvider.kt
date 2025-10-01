/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.providers

import android.content.Context
import android.util.Log
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import com.aurora.store.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteBlacklistProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext private val context: Context,
    private val blacklistProvider: BlacklistProvider
) {

    private val TAG = RemoteBlacklistProvider::class.java.simpleName
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val isRemoteBlacklistEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_REMOTE_BLACKLIST_ENABLED, true)

    var remoteBlacklistUrl: String
        get() = Preferences.getString(
            context, 
            Preferences.PREFERENCE_REMOTE_BLACKLIST_URL,
            "https://api.github.com/repos/alltechdev/alltech.dev/contents/blacklist.json?ref=main"
        )
        set(value) = Preferences.putString(context, Preferences.PREFERENCE_REMOTE_BLACKLIST_URL, value)

    private var lastUpdateTime: Long
        get() = Preferences.getLong(context, Preferences.PREFERENCE_REMOTE_BLACKLIST_LAST_UPDATE)
        set(value) = Preferences.putLong(context, Preferences.PREFERENCE_REMOTE_BLACKLIST_LAST_UPDATE, value)

    fun shouldUpdate(): Boolean {
        // Always update when app opens
        return true
    }

    suspend fun fetchAndUpdateBlacklist(): Boolean = withContext(Dispatchers.IO) {
        if (!isRemoteBlacklistEnabled) {
            Log.d(TAG, "Remote blacklist is disabled")
            return@withContext false
        }

        try {
            Log.d(TAG, "Fetching blacklist from: $remoteBlacklistUrl")
            
            val request = Request.Builder()
                .url(remoteBlacklistUrl)
                .addHeader("User-Agent", "Aurora-Store")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch blacklist: HTTP ${response.code}")
                return@withContext false
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Empty response from remote blacklist")
                return@withContext false
            }

            val remoteBlacklist = try {
                // Check if this is a GitHub API response or direct JSON
                if (remoteBlacklistUrl.contains("api.github.com")) {
                    // Parse GitHub API response
                    val apiResponse = json.parseToJsonElement(responseBody).jsonObject
                    val contentEncoded = apiResponse["content"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("No content field in GitHub API response")
                    
                    // Decode base64 content
                    val decodedContent = String(Base64.getDecoder().decode(contentEncoded.replace("\n", "")))
                    Log.d(TAG, "Decoded blacklist content: $decodedContent")
                    json.decodeFromString<List<String>>(decodedContent).toMutableSet()
                } else {
                    // Direct JSON response
                    json.decodeFromString<List<String>>(responseBody).toMutableSet()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse remote blacklist JSON", e)
                return@withContext false
            }

            // Check if blacklist has changed before updating
            val currentBlacklist = blacklistProvider.blacklist
            if (currentBlacklist != remoteBlacklist) {
                // Update local blacklist with remote data
                blacklistProvider.blacklist = remoteBlacklist
                lastUpdateTime = System.currentTimeMillis()
                
                Log.d(TAG, "Successfully updated blacklist with ${remoteBlacklist.size} entries (changed)")
                Log.d(TAG, "Blacklist entries: ${remoteBlacklist.take(5)}")  // Show first 5 entries
                
                // Emit event to notify UI that blacklist has been updated
                AuroraApp.events.send(BusEvent.BlacklistUpdated)
            } else {
                Log.d(TAG, "Blacklist unchanged, skipping update")
            }
            return@withContext true

        } catch (e: IOException) {
            Log.e(TAG, "Network error while fetching blacklist", e)
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while fetching blacklist", e)
            return@withContext false
        }
    }

    fun enableRemoteBlacklist(url: String = remoteBlacklistUrl) {
        remoteBlacklistUrl = url
        Preferences.putBoolean(context, Preferences.PREFERENCE_REMOTE_BLACKLIST_ENABLED, true)
        
        // Fetch immediately when enabled
        CoroutineScope(Dispatchers.IO).launch {
            fetchAndUpdateBlacklist()
        }
    }

    fun disableRemoteBlacklist() {
        Preferences.putBoolean(context, Preferences.PREFERENCE_REMOTE_BLACKLIST_ENABLED, false)
    }

    suspend fun forceUpdate(): Boolean {
        return fetchAndUpdateBlacklist()
    }
}