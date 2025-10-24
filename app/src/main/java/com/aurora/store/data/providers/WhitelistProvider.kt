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
import android.content.SharedPreferences
import android.util.Log
import com.aurora.extensions.isNAndAbove
import com.aurora.store.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext val context: Context,
) {

    private val PREFERENCE_WHITELIST = "PREFERENCE_WHITELIST"
    private val TAG = WhitelistProvider::class.java.simpleName

    var whitelist: MutableSet<String>
        set(value) {
            Log.d(TAG, "Setting whitelist with ${value.size} entries: ${value.take(5)}")
            Preferences.putString(
                context,
                PREFERENCE_WHITELIST,
                json.encodeToString(value)
            )
        }
        get() {
            return try {
                val rawWhitelist = if (isNAndAbove) {
                    val refMethod = Context::class.java.getDeclaredMethod(
                        "getSharedPreferences",
                        File::class.java,
                        Int::class.java
                    )
                    val refSharedPreferences = refMethod.invoke(
                        context,
                        File("/product/etc/com.aurora.store/whitelist.xml"),
                        Context.MODE_PRIVATE
                    ) as SharedPreferences

                    Preferences.getPrefs(context)
                        .getString(
                            PREFERENCE_WHITELIST,
                            refSharedPreferences.getString(PREFERENCE_WHITELIST, "")
                        )
                } else {
                    Preferences.getString(context, PREFERENCE_WHITELIST)
                }
                if (rawWhitelist!!.isEmpty()) {
                    Log.d(TAG, "No whitelist found, returning empty set")
                    mutableSetOf()
                } else {
                    val whitelistSet = json.decodeFromString<MutableSet<String>>(rawWhitelist)
                    Log.d(TAG, "Retrieved whitelist with ${whitelistSet.size} entries: ${whitelistSet.take(5)}")
                    whitelistSet
                }
            } catch (e: Exception) {
                mutableSetOf()
            }
        }

    fun isWhitelisted(packageName: String): Boolean {
        return whitelist.contains(packageName)
    }


    fun addToWhitelist(packageName: String) {
        whitelist = whitelist.apply {
            add(packageName)
        }
    }

    fun removeFromWhitelist(packageName: String) {
        whitelist = whitelist.apply {
            remove(packageName)
        }
    }
}
