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

package com.aurora.store.util

import android.content.Context
import java.security.MessageDigest

object PasscodeUtil {
    
    fun hasBlacklistPassword(context: Context): Boolean {
        return Preferences.getString(context, Preferences.PREFERENCE_BLACKLIST_PASSWORD).isNotEmpty()
    }
    
    fun setBlacklistPassword(context: Context, password: String) {
        if (isValidPassword(password)) {
            val hashedPassword = hashPassword(password)
            Preferences.putString(context, Preferences.PREFERENCE_BLACKLIST_PASSWORD, hashedPassword)
        }
    }
    
    fun verifyBlacklistPassword(context: Context, password: String): Boolean {
        if (!isValidPassword(password)) return false
        
        val storedPassword = Preferences.getString(context, Preferences.PREFERENCE_BLACKLIST_PASSWORD)
        if (storedPassword.isEmpty()) return true // No password set
        
        return hashPassword(password) == storedPassword
    }
    
    fun removeBlacklistPassword(context: Context) {
        Preferences.remove(context, Preferences.PREFERENCE_BLACKLIST_PASSWORD)
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 4 && password.all { it.isLetterOrDigit() }
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}