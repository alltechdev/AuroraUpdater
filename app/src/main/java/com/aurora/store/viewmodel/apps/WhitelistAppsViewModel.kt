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

package com.aurora.store.viewmodel.apps

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.providers.WhitelistProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhitelistAppsViewModel @Inject constructor(
    private val whitelistProvider: WhitelistProvider,
    private val appDetailsHelper: AppDetailsHelper,
    private val downloadHelper: DownloadHelper
) : ViewModel() {

    private val TAG = WhitelistAppsViewModel::class.java.simpleName

    private val _apps = MutableStateFlow<List<App>>(emptyList())
    val apps: StateFlow<List<App>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val downloadsList = downloadHelper.downloadsList

    fun download(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadHelper.enqueueApp(app)
        }
    }

    fun cancelDownload(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadHelper.cancelDownload(packageName)
        }
    }

    fun fetchWhitelistApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true

                // Get whitelist package names
                val whitelistPackages = whitelistProvider.whitelist.toList()

                Log.d(TAG, "Fetching details for ${whitelistPackages.size} whitelisted apps")

                if (whitelistPackages.isEmpty()) {
                    _apps.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                // Fetch app details from Play Store for whitelisted packages
                val apps = appDetailsHelper.getAppByPackageName(whitelistPackages)
                    .filter { it.displayName.isNotEmpty() }
                    .sortedBy { it.displayName.lowercase() }

                Log.d(TAG, "Successfully fetched ${apps.size} whitelisted apps")

                _apps.value = apps
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch whitelisted apps", e)
                _apps.value = emptyList()
                _isLoading.value = false
            }
        }
    }
}
