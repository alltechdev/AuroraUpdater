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

package com.aurora.store.view.ui.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.store.R
import com.aurora.store.util.PasscodeUtil
import com.aurora.store.view.ui.sheets.PasscodeDialogSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)

        findPreference<Preference>("pref_perms")?.setOnPreferenceClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToPermissionsFragment(false)
            )
            true
        }
        findPreference<Preference>("pref_install")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.installationPreference)
            true
        }
        findPreference<Preference>("pref_network")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.networkPreference)
            true
        }
        findPreference<Preference>("pref_updates")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.updatesPreference)
            true
        }
        findPreference<Preference>("pref_blacklist_password")?.setOnPreferenceClickListener {
            handleBlacklistPasswordPreference()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_settings)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
        
        updatePasswordPreferenceSummary()
    }

    private fun updatePasswordPreferenceSummary() {
        findPreference<Preference>("pref_blacklist_password")?.summary = 
            if (PasscodeUtil.hasBlacklistPassword(requireContext())) {
                "Password is set"
            } else {
                getString(R.string.pref_blacklist_password_summary)
            }
    }

    private fun handleBlacklistPasswordPreference() {
        val hasPassword = PasscodeUtil.hasBlacklistPassword(requireContext())
        
        if (hasPassword) {
            // Show options: Change or Remove
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.pref_blacklist_password_title))
                .setItems(arrayOf(
                    getString(R.string.pref_blacklist_password_change),
                    getString(R.string.pref_blacklist_password_remove)
                )) { _, which ->
                    when (which) {
                        0 -> {
                            // Change password
                            val setPasswordDialog = PasscodeDialogSheet.newInstanceForSet()
                            setPasswordDialog.show(parentFragmentManager, PasscodeDialogSheet.TAG)
                        }
                        1 -> {
                            // Remove password - first verify current password
                            showPasswordVerificationForRemoval()
                        }
                    }
                }
                .show()
        } else {
            // No password set, show set password dialog
            val setPasswordDialog = PasscodeDialogSheet.newInstanceForSet()
            setPasswordDialog.show(parentFragmentManager, PasscodeDialogSheet.TAG)
        }
    }

    private fun showPasswordVerificationForRemoval() {
        val verifyDialog = PasscodeDialogSheet.newInstanceForRemoval()
        verifyDialog.show(parentFragmentManager, PasscodeDialogSheet.TAG)
    }
}
