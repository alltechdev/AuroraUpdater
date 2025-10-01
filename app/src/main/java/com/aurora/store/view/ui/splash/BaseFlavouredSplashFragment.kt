package com.aurora.store.view.ui.splash

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.extensions.getPackageName
import com.aurora.extensions.navigate
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.data.model.AuthState
import com.aurora.store.databinding.FragmentSplashBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MICROG_AUTH
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.view.ui.sheets.PasscodeDialogSheet
import com.aurora.store.viewmodel.auth.AuthViewModel
import com.aurora.store.util.PasscodeUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFlavouredSplashFragment : BaseFragment<FragmentSplashBinding>() {

    private val TAG = BaseFlavouredSplashFragment::class.java.simpleName

    val viewModel: AuthViewModel by activityViewModels()

    val canLoginWithMicroG: Boolean
        get() = PackageUtil.hasSupportedMicroGVariant(requireContext()) &&
                Preferences.getBoolean(requireContext(), PREFERENCE_MICROG_AUTH, true)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Preferences.getBoolean(requireContext(), PREFERENCE_INTRO)) {
            findNavController().navigate(
                SplashFragmentDirections.actionSplashFragmentToOnboardingFragment()
            )
            return
        }

        // Toolbar
        binding.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_spoof_manager -> {
                        findNavController().navigate(R.id.spoofFragment)
                    }

                    R.id.menu_settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                    }

                    R.id.menu_about -> requireContext().navigate(Screen.About)
                }
                true
            }
        }

        attachActions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest {
                when (it) {
                    AuthState.Init -> updateStatus(getString(R.string.session_init))

                    AuthState.Fetching -> {
                        updateStatus(getString(R.string.requesting_new_session))
                    }

                    AuthState.Valid -> {
                        val packageName =
                            requireActivity().intent.getPackageName(requireArguments())
                        if (!packageName.isNullOrBlank()) {
                            requireArguments().remove("packageName")
                        }
                        navigateToDefaultTab()
                    }

                    AuthState.Available -> {
                        updateStatus(getString(R.string.session_verifying))
                        updateActionLayout(false)
                    }

                    AuthState.Unavailable -> {
                        updateStatus(getString(R.string.session_login))
                        updateActionLayout(true)
                    }

                    AuthState.SignedIn -> {
                        val packageName =
                            requireActivity().intent.getPackageName(requireArguments())
                        if (!packageName.isNullOrBlank()) {
                            requireArguments().remove("packageName")
                        }
                        navigateToDefaultTab()
                    }

                    AuthState.SignedOut -> {
                        updateStatus(getString(R.string.session_scrapped))
                        updateActionLayout(true)
                    }

                    AuthState.Verifying -> {
                        updateStatus(getString(R.string.verifying_new_session))
                    }

                    is AuthState.PendingAccountManager -> {
                        // Google authentication not supported, treat as failed
                        updateStatus(getString(R.string.session_login))
                        updateActionLayout(true)
                        resetActions()
                    }

                    is AuthState.Failed -> {
                        updateStatus(it.status)
                        updateActionLayout(true)
                        resetActions()
                    }
                }
            }
        }
    }

    private fun updateStatus(string: String?) {
        activity?.runOnUiThread { binding.txtStatus.text = string }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        binding.layoutAction.isVisible = isVisible
        binding.toolbar.isVisible = isVisible
    }

    private fun navigateToDefaultTab() {
        val defaultDestination =
            Preferences.getInteger(requireContext(), PREFERENCE_DEFAULT_SELECTED_TAB)
        val directions =
            when (requireArguments().getInt("destinationId", defaultDestination)) {
                R.id.updatesFragment -> {
                    requireArguments().remove("destinationId")
                    SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                }

                1 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                2 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                else -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
            }
        requireActivity().viewModelStore.clear() // Clear ViewModelStore to avoid bugs with logout
        findNavController().navigate(directions)
    }


    open fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnSpoofManager?.addOnClickListener {
            findNavController().navigate(R.id.spoofFragment)
        }
    }

    open fun resetActions() {
        binding.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }
    }

    private fun handleBlacklistAccess() {
        // Blacklist management is no longer available to users
        // Blacklist is now automatically managed via remote URL
    }
}
