/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.whitelist

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.providers.WhitelistProvider
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val json: Json,
    private val updateHelper: UpdateHelper,
    private val whitelistProvider: WhitelistProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = WhitelistViewModel::class.java.simpleName

    private val isAuroraOnlyFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)
    private val isFDroidFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID, true)
    private val isExtendedUpdateEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    private val _packages = MutableStateFlow<List<PackageInfo>?>(null)
    private val _filteredPackages = MutableStateFlow<List<PackageInfo>?>(null)
    val filteredPackages = _filteredPackages.asStateFlow()

    val whitelist = mutableStateListOf<String>()

    init {
        whitelist.addAll(whitelistProvider.whitelist)
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packages.value = PackageUtil.getAllValidPackages(context).also { pkgList ->
                    _filteredPackages.value = pkgList
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun search(query: String) {
        if (query.isNotBlank()) {
            _filteredPackages.value = _packages.value!!
                .filter { it.applicationInfo!!.loadLabel(context.packageManager)
                    .contains(query, true) || it.packageName.contains(query, true)
                }
        } else {
            _filteredPackages.value = _packages.value
        }
    }

    fun isFiltered(packageInfo: PackageInfo): Boolean {
        return when {
            !isExtendedUpdateEnabled && !packageInfo.applicationInfo!!.enabled -> true
            isAuroraOnlyFilterEnabled -> !CertUtil.isAuroraStoreApp(context, packageInfo.packageName)
            isFDroidFilterEnabled -> CertUtil.isFDroidApp(context, packageInfo.packageName)
            else -> false
        }
    }

    fun addToWhitelist(packageName: String) {
        whitelist.add(packageName)
        whitelistProvider.addToWhitelist(packageName)
        AuroraApp.Companion.events.send(BusEvent.Whitelisted(packageName))
    }

    fun addAllToWhitelist() {
        whitelistProvider.whitelist = _packages.value!!.map { it.packageName }.toMutableSet()
        whitelist.apply {
            clear()
            addAll(whitelistProvider.whitelist)
        }
        viewModelScope.launch { updateHelper.deleteAllUpdates() }
    }

    fun removeFromWhitelist(packageName: String) {
        whitelist.remove(packageName)
        whitelistProvider.removeFromWhitelist(packageName)
    }

    fun removeAllFromWhitelist() {
        whitelist.clear()
        whitelistProvider.whitelist = mutableSetOf()
    }

    fun importWhitelist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importedSet = json.decodeFromString<MutableSet<String>>(
                        it.bufferedReader().readText()
                    )

                    val validImportedSet = importedSet
                        .filter { pkgName -> _packages.value!!.any { it.packageName == pkgName } }
                    whitelistProvider.whitelist.addAll(validImportedSet)
                    whitelist.apply {
                        clear()
                        addAll(whitelistProvider.whitelist)
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to import whitelist", exception)
            }
        }
    }

    fun exportWhitelist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.encodeToString(whitelistProvider.whitelist).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export whitelist", exception)
            }
        }
    }
}
