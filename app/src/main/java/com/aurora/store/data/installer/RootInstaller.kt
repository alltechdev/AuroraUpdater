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

package com.aurora.store.data.installer

import android.content.Context
import android.util.Log
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.data.model.BuildType
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {
        const val PLAY_PACKAGE_NAME = "com.android.vending"

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 2,
                installer = Installer.ROOT,
                packageNames = BuildType.PACKAGE_NAMES,
                installerPackageNames = listOf(PLAY_PACKAGE_NAME),
                title = R.string.pref_install_mode_root,
                subtitle = R.string.root_installer_subtitle,
                description = R.string.root_installer_desc
            )

        /**
         * Wraps the given command to run as the given UID through su, or as root when it is null
         */
        internal fun asUid(uid: Int?, command: String): String =
            uid?.let { "su $it -c '$command'" } ?: command
    }

    private val TAG = RootInstaller::class.java.simpleName

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            if (Shell.getShell().isRoot) {
                download.sharedLibs.forEach {
                    // Shared library packages cannot be updated
                    if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                        xInstall(download.packageName, download.versionCode, it.packageName)
                    }
                }
                xInstall(download.packageName, download.versionCode)
            } else {
                postError(
                    download.packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_root_unavailable)
                )
                Log.e(TAG, " >>>>>>>>>>>>>>>>>>>>>>>>>> NO ROOT ACCESS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
            }
        }
    }

    private fun xInstall(packageName: String, versionCode: Long, sharedLibPkgName: String = "") {
        var totalSize = 0

        for (file in getFiles(packageName, versionCode, sharedLibPkgName))
            totalSize += file.length().toInt()

        val createCommand =
            "pm install-create -i $PLAY_PACKAGE_NAME --user 0 -r -S $totalSize"

        // Create and commit as the Play Store so it is also recorded as the initiating package,
        // falling back to root if it is missing or su cannot switch to its UID
        var playStoreUid = getPlayStoreUid()
        val result = playStoreUid?.let { Shell.cmd(asUid(it, createCommand)).exec() }
            ?.takeIf { it.isSuccess }
            ?: Shell.cmd(createCommand).exec().also { playStoreUid = null }

        val response = result.out

        val sessionIdPattern = Pattern.compile("(\\d+)")
        val sessionIdMatcher = sessionIdPattern.matcher(response[0])
        val found = sessionIdMatcher.find()

        if (found) {
            val sessionId = sessionIdMatcher.group(1)?.toInt()
            if (Shell.getShell().isRoot && sessionId != null) {
                for (file in getFiles(packageName, versionCode, sharedLibPkgName)) {
                    Shell.cmd("cat \"${file.absoluteFile}\" | pm install-write -S ${file.length()} $sessionId \"${file.name}\"")
                        .exec()
                }

                val shellResult =
                    Shell.cmd(asUid(playStoreUid, "pm install-commit $sessionId")).exec()

                if (shellResult.isSuccess) {
                    // Installation is not yet finished if this is a shared library
                    if (packageName == download?.packageName) onInstallationSuccess()
                } else {
                    removeFromInstallQueue(packageName)
                    AuroraApp.events.send(
                        InstallerEvent.Failed(
                            packageName = packageName,
                            error = parseError(shellResult)
                        )
                    )
                }
            } else {
                removeFromInstallQueue(packageName)
                postError(
                    packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_root_unavailable)
                )
            }
        } else {
            removeFromInstallQueue(packageName)
            postError(
                packageName,
                context.getString(R.string.installer_status_failure),
                context.getString(R.string.installer_status_failure_session)
            )
        }
    }

    private fun getPlayStoreUid(): Int? = runCatching {
        PackageUtil.getPackageInfo(context, PLAY_PACKAGE_NAME).applicationInfo?.uid
    }.getOrNull()

    private fun parseError(result: Shell.Result): String {
        return result.err.joinToString(separator = "\n")
    }
}
