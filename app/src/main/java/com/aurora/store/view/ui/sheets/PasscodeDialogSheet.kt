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

package com.aurora.store.view.ui.sheets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.navigate
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.databinding.DialogPasscodeBinding
import com.aurora.store.util.PasscodeUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PasscodeDialogSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "PasscodeDialogSheet"
        private const val ARG_MODE = "mode"
        private const val ARG_TEMP_PASSCODE = "temp_passcode"
        
        const val MODE_VERIFY = "verify"
        const val MODE_SET = "set"
        const val MODE_CONFIRM = "confirm"
        const val MODE_REMOVE = "remove"

        fun newInstanceForVerify(): PasscodeDialogSheet {
            return PasscodeDialogSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, MODE_VERIFY)
                }
            }
        }

        fun newInstanceForSet(): PasscodeDialogSheet {
            return PasscodeDialogSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, MODE_SET)
                }
            }
        }

        fun newInstanceForConfirm(tempPasscode: String): PasscodeDialogSheet {
            return PasscodeDialogSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, MODE_CONFIRM)
                    putString(ARG_TEMP_PASSCODE, tempPasscode)
                }
            }
        }

        fun newInstanceForRemoval(): PasscodeDialogSheet {
            return PasscodeDialogSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, MODE_REMOVE)
                }
            }
        }
    }

    private var _binding: DialogPasscodeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mode: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPasscodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mode = arguments?.getString(ARG_MODE) ?: MODE_VERIFY

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        when (mode) {
            MODE_VERIFY -> {
                binding.title.text = getString(R.string.blacklist_password_dialog_title)
                binding.subtitle.text = getString(R.string.blacklist_password_dialog_subtitle)
            }
            MODE_SET -> {
                binding.title.text = getString(R.string.blacklist_password_set_title)
                binding.subtitle.text = getString(R.string.blacklist_password_set_subtitle)
            }
            MODE_CONFIRM -> {
                binding.title.text = getString(R.string.blacklist_password_confirm_title)
                binding.subtitle.text = getString(R.string.blacklist_password_confirm_subtitle)
            }
            MODE_REMOVE -> {
                binding.title.text = getString(R.string.blacklist_password_remove_title)
                binding.subtitle.text = getString(R.string.blacklist_password_remove_subtitle)
            }
        }
    }

    private fun setupListeners() {
        binding.passcodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.errorMessage.isVisible = false
            }

            override fun afterTextChanged(s: Editable?) {
                // Clear error when user types
                binding.errorMessage.isVisible = false
            }
        })

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            handlePasscodeEntry()
        }
    }

    private fun handlePasscodeEntry() {
        val password = binding.passcodeInput.text.toString()

        if (!PasscodeUtil.isValidPassword(password)) {
            showError(getString(R.string.blacklist_password_error_invalid))
            return
        }

        when (mode) {
            MODE_VERIFY -> {
                if (PasscodeUtil.verifyBlacklistPassword(requireContext(), password)) {
                    // Password correct, navigate to blacklist
                    dismiss()
                    requireContext().navigate(Screen.Blacklist)
                } else {
                    showError(getString(R.string.blacklist_password_error_wrong))
                    binding.passcodeInput.text?.clear()
                }
            }
            MODE_SET -> {
                // First entry, ask for confirmation
                dismiss()
                val confirmDialog = newInstanceForConfirm(password)
                confirmDialog.show(parentFragmentManager, TAG)
            }
            MODE_CONFIRM -> {
                val tempPassword = arguments?.getString(ARG_TEMP_PASSCODE) ?: ""
                if (password == tempPassword) {
                    // Passwords match, save it
                    PasscodeUtil.setBlacklistPassword(requireContext(), password)
                    dismiss()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.blacklist_password_set_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Update the preference summary if we're in settings
                    (parentFragment as? PreferenceFragmentCompat)?.let { settingsFragment ->
                        try {
                            val method = settingsFragment.javaClass.getDeclaredMethod("updatePasswordPreferenceSummary")
                            method.isAccessible = true
                            method.invoke(settingsFragment)
                        } catch (e: Exception) {
                            // Method not found or error, ignore
                        }
                    }
                } else {
                    showError(getString(R.string.blacklist_password_error_mismatch))
                    binding.passcodeInput.text?.clear()
                }
            }
            MODE_REMOVE -> {
                if (PasscodeUtil.verifyBlacklistPassword(requireContext(), password)) {
                    // Password correct, show confirmation dialog
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.blacklist_password_remove_confirmation_title))
                        .setMessage(getString(R.string.blacklist_password_remove_confirmation_message))
                        .setPositiveButton("Remove") { _, _ ->
                            PasscodeUtil.removeBlacklistPassword(requireContext())
                            dismiss() // Dismiss after the action is confirmed
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.blacklist_password_removed_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Update the preference summary if we're in settings
                            (parentFragment as? PreferenceFragmentCompat)?.let { settingsFragment ->
                                try {
                                    val method = settingsFragment.javaClass.getDeclaredMethod("updatePasswordPreferenceSummary")
                                    method.isAccessible = true
                                    method.invoke(settingsFragment)
                                } catch (e: Exception) {
                                    // Method not found or error, ignore
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    showError(getString(R.string.blacklist_password_error_wrong))
                    binding.passcodeInput.text?.clear()
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorMessage.isVisible = true
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Clear any sensitive data
        binding.passcodeInput.text?.clear()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}