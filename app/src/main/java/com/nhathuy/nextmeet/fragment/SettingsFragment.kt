package com.nhathuy.nextmeet.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.FragmentSettingsBinding
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.ui.LoginActivity
import com.nhathuy.nextmeet.ui.ProfileEditActivity
import com.nhathuy.nextmeet.utils.Constant
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Fragment quản lý cài đặt ứng dụng
 *
 * @author TravisHuy(Ho Nhat Huy)
 * @since 30.06.2025
 */
@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var currentUserId: Int = 0
    companion object {
        private const val PREF_LANGUAGE = "pref_language"
        private const val PREF_THEME = "pref_theme"
        private const val PREF_NOTIFICATIONS = "pref_notifications"

        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
        private const val THEME_SYSTEM = "system"

        private const val LANG_VIETNAMESE = "vi"
        private const val LANG_ENGLISH = "en"

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
        setupUserInfo()
        setupClickListeners()
        setupInitialStates()
        observeLogoutState()

    }
    private fun initializeComponents() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private fun setupUserInfo() {
        // Quan sát thông tin người dùng hiện tại từ ViewModel
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUserId = user.id
                displayUserInfo(it)
            } ?: run {
                // Nếu không có thông tin người dùng, chuyển về màn hình đăng nhập
                redirectToLogin()
            }
        }
    }

    /**
     * Hiển thị thông tin người dùng lên giao diện
     */
    private fun displayUserInfo(user: User) {
        binding.apply {
            tvUserName.text = user.name
            tvUserPhone.text = user.phone
            tvUserEmail.text = user.email

            val initials = getInitials(user.name)
            binding.ivUserAvatar.text = initials
        }
    }

    /**
     * Lay user 2 chu
     */
    private fun getInitials(contactName: String): String {
        return contactName.split(" ")
            .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
    }

    /**
     * setup click
     */
    private fun setupClickListeners(){
        binding.apply {

            //edit button
            btnEditProfile.setOnClickListener {
                val intent = Intent(requireContext(), ProfileEditActivity::class.java).apply {
                    putExtra(Constant.EXTRA_USER_ID,currentUserId)
                }
                startActivity(intent)
            }

            //language setting
            llLanguageSetting.setOnClickListener {
                showLanguageDialog()
            }

            // Theme setting
            llThemeSetting.setOnClickListener {
                showThemeDialog()
            }

            // Notifications switch
            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                handleNotificationToggle(isChecked)
            }

            // Rate app
            llRateApp.setOnClickListener {
                rateApp()
            }

            // Feedback
            llFeedback.setOnClickListener {
                sendFeedback()
            }

            // About
            llAbout.setOnClickListener {
                showAboutDialog()
            }

            // Logout button
            btnLogout.setOnClickListener {
                showLogoutConfirmation()
            }
        }
    }

    // init state
    private fun setupInitialStates(){
        // set ngon ngu hien tại
        val currentLanguage = sharedPreferences.getString(PREF_LANGUAGE,LANG_VIETNAMESE)
        updateLanguageDisplay(currentLanguage ?: LANG_VIETNAMESE)

        // cai dat theme hiện tại
        val currentTheme = sharedPreferences.getString(PREF_THEME, THEME_SYSTEM)
        updateThemeDisplay(currentTheme ?: THEME_SYSTEM)

        // cài đặt trạng thái của thông báo
        val notificationEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATIONS,true)
        binding.switchNotifications.isChecked = notificationEnabled

        //cài đặt app
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvAppVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: Exception) {
            binding.tvAppVersion.text = getString(R.string.version_unknown)
        }
    }

    /**
     * Hiển thị hộp thoại xác nhận đăng xuất
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                userViewModel.logout()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    /**
     * Hiển thị dialog voi ngôn ngu
     */
    private fun showLanguageDialog(){
        val languages = arrayOf(
            getString(R.string.viet_namese),
            getString(R.string.english)
        )

        val languageCodes = arrayOf(LANG_VIETNAMESE,LANG_ENGLISH)
        val currentLanguage = sharedPreferences.getString(PREF_LANGUAGE,LANG_VIETNAMESE)
        val selectedIndex = languageCodes.indexOf(currentLanguage)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languages,selectedIndex) {
                dialog, which ->
                val selectedLanguage = languageCodes[which]
                if(selectedLanguage != currentLanguage){
                    changeLanguage(selectedLanguage)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel),null)
            .show()
    }

    /**
     * thay đổi ngôn ngữ
     */
    private fun changeLanguage(languageCode: String) {
        sharedPreferences.edit().putString(PREF_LANGUAGE,languageCode).apply()
        updateLanguageDisplay(languageCode)

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)

        requireActivity().recreate()
    }

    // cap nhat hien thị ngôn ngữ
    private fun updateLanguageDisplay(languageCode: String) {
        val languageText = when(languageCode){
            LANG_VIETNAMESE -> getString(R.string.viet_namese)
            LANG_ENGLISH -> getString(R.string.english)
            else -> getString(R.string.viet_namese)
        }
        binding.tvCurrentLanguage.text = languageText
    }

    // hiển thị theme dialog
    private fun showThemeDialog(){
        val themes = arrayOf(
            getString(R.string.light_theme),
            getString(R.string.dark_theme),
            getString(R.string.system_theme)
        )

        val themeCodes = arrayOf(THEME_LIGHT,THEME_DARK,THEME_SYSTEM)
        val currentTheme = sharedPreferences.getString(PREF_THEME,THEME_SYSTEM)
        val selectedIndex = themeCodes.indexOf(currentTheme)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_theme))
            .setSingleChoiceItems(themes,selectedIndex) {
                dialog, which ->
                val selectedTheme = themeCodes[which]
                if(selectedTheme != currentTheme){
                    changeTheme(selectedTheme)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel),null)
            .show()
    }

    // thay đổi theme
    private fun changeTheme(themeCode: String) {
        sharedPreferences.edit().putString(PREF_THEME, themeCode).apply()
        updateThemeDisplay(themeCode)

        // ap dung theme thay doi
        val mode = when (themeCode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // cập nhật theme
    private fun updateThemeDisplay(themeCode: String) {
        val themeText = when (themeCode) {
            THEME_LIGHT -> getString(R.string.light_theme)
            THEME_DARK -> getString(R.string.dark_theme)
            THEME_SYSTEM -> getString(R.string.system_theme)
            else -> getString(R.string.system_theme)
        }
        binding.tvCurrentTheme.text = themeText
    }

    // xử lý handle notification
    private fun handleNotificationToggle(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_NOTIFICATIONS, isEnabled).apply()

        if (isEnabled) {
            // Enable notifications
//            Toast.makeText(requireContext(), R.string.notifications_enabled, Toast.LENGTH_SHORT).show()
            // TODO: Register for push notifications or enable local notifications
        } else {
            // Disable notifications
//            Toast.makeText(requireContext(), R.string.notifications_disabled, Toast.LENGTH_SHORT).show()
            // TODO: Unregister from push notifications or disable local notifications
        }
    }

    //đánh giá app
    private fun rateApp(){
        try {
            val uri = Uri.parse("market://details?id=${requireContext().packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        catch(e:Exception){
            val uri = Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    // gửi send feed back
    private fun sendFeedback(){
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("honhathuy098@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_template))
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.send_feedback)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAboutDialog() {
        val packageInfo = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        } catch (e: Exception) {
            null
        }

        val aboutMessage = getString(
            R.string.about_message,
            packageInfo?.versionName ?: "Unknown",
            packageInfo?.versionCode ?: 0
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about)
            .setMessage(aboutMessage)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.privacy_policy) { _, _ ->
                openPrivacyPolicy()
            }
            .show()
    }

    private fun openPrivacyPolicy() {
        val uri = Uri.parse("https://nextmeet.com/privacy-policy")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    /**
     * Quan sát trạng thái đăng xuất
     */
    private fun observeLogoutState() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.logoutState.collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        // Có thể hiển thị loading nếu cần
                    }

                    is Resource.Success -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.logout_success,
                            Toast.LENGTH_SHORT
                        ).show()
                        redirectToLogin()
                    }

                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            result.message ?: getString(R.string.logout_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Chuyển hướng về màn hình đăng nhập
     */
    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        // Xóa tất cả các activity trước đó trong stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        // Thêm hiệu ứng chuyển màn hình
//        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
//        finish()
    }

    private fun displayLoginTime() {
//        val currentTime =
//            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
//        binding.tvLoginTime.text = getString(R.string.login_time_format, currentTime)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}