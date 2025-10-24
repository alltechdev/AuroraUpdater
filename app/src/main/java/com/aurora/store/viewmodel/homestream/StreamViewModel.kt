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

package com.aurora.store.viewmodel.homestream

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import com.aurora.store.HomeStash
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val webStreamHelper: WebStreamHelper,
    private val whitelistProvider: com.aurora.store.data.providers.WhitelistProvider
) : ViewModel() {

    private val TAG = StreamViewModel::class.java.simpleName

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private val stash: HomeStash = mutableMapOf()

    private val streamContract: StreamContract
        get() = webStreamHelper

    // Mutex to protect stash access for thread safety
    private val stashMutex = Mutex()

    fun getStreamBundle(category: StreamContract.Category, type: StreamContract.Type) {
        liveData.postValue(ViewState.Loading)
        observe(category, type)
    }

    fun observe(category: StreamContract.Category, type: StreamContract.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stashMutex.withLock {
                    val bundle = targetBundle(category)

                    // Post existing data if any clusters exist
                    if (bundle.hasCluster()) {
                        liveData.postValue(ViewState.Success(stash.toMap()))
                    }

                    if (!bundle.hasCluster() || bundle.hasNext()) {

                        // Fetch new stream bundle
                        val newBundle = if (bundle.hasCluster()) {
                            streamContract.nextStreamBundle(
                                category,
                                bundle.streamNextPageUrl
                            )
                        } else {
                            streamContract.fetch(type, category)
                        }

                        // Filter apps by whitelist
                        val filteredBundle = filterBundleByWhitelist(newBundle)

                        // Update old bundle
                        val mergedBundle = bundle.copy(
                            streamClusters = bundle.streamClusters + filteredBundle.streamClusters,
                            streamNextPageUrl = filteredBundle.streamNextPageUrl
                        )
                        stash[category] = mergedBundle

                        // Post updated to UI
                        liveData.postValue(ViewState.Success(stash.toMap()))
                    } else {
                        Log.i(TAG, "End of Bundle")
                    }
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    fun observeCluster(category: StreamContract.Category, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val newCluster = streamContract.nextStreamCluster(
                        streamCluster.clusterNextPageUrl
                    )

                    // Filter apps by whitelist
                    val filteredCluster = filterClusterByWhitelist(newCluster)

                    stashMutex.withLock {
                        updateCluster(category, streamCluster.id, filteredCluster)
                    }

                    liveData.postValue(ViewState.Success(stash.toMap()))
                } else {
                    stashMutex.withLock {
                        postClusterEnd(category, streamCluster.id)
                    }

                    liveData.postValue(ViewState.Success(stash.toMap()))
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    private fun updateCluster(
        category: StreamContract.Category,
        clusterID: Int,
        newCluster: StreamCluster
    ) {
        val bundle = stash[category] ?: return
        val oldCluster = bundle.streamClusters[clusterID] ?: return

        val mergedCluster = oldCluster.copy(
            clusterNextPageUrl = newCluster.clusterNextPageUrl,
            clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
        )

        val updatedClusters = bundle.streamClusters.toMutableMap().apply {
            this[clusterID] = mergedCluster
        }

        stash[category] = bundle.copy(streamClusters = updatedClusters)
    }

    private fun postClusterEnd(category: StreamContract.Category, clusterID: Int) {
        val bundle = stash[category] ?: return
        val oldCluster = bundle.streamClusters[clusterID] ?: return

        val updatedCluster = oldCluster.copy(clusterNextPageUrl = "")
        val updatedClusters = bundle.streamClusters.toMutableMap().apply {
            this[clusterID] = updatedCluster
        }

        stash[category] = bundle.copy(streamClusters = updatedClusters)
    }

    private fun targetBundle(category: StreamContract.Category): StreamBundle {
        return stash.getOrPut(category) { StreamBundle() }
    }

    /**
     * Filter apps in a StreamBundle by whitelist - only keep whitelisted apps
     */
    private fun filterBundleByWhitelist(bundle: StreamBundle): StreamBundle {
        val filteredClusters = bundle.streamClusters.mapValues { (_, cluster) ->
            filterClusterByWhitelist(cluster)
        }
        return bundle.copy(streamClusters = filteredClusters)
    }

    /**
     * Filter apps in a StreamCluster by whitelist - only keep whitelisted apps
     */
    private fun filterClusterByWhitelist(cluster: StreamCluster): StreamCluster {
        val filteredApps = cluster.clusterAppList.filter { app ->
            whitelistProvider.isWhitelisted(app.packageName)
        }
        Log.d(TAG, "Filtered cluster '${cluster.clusterTitle}': ${cluster.clusterAppList.size} -> ${filteredApps.size} apps (whitelist)")
        return cluster.copy(clusterAppList = filteredApps)
    }
}
