package com.nhathuy.nextmeet.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.LoginForm
import com.nhathuy.nextmeet.model.PasswordResetForm
import com.nhathuy.nextmeet.model.RegistrationForm
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.model.ValidationResult
import com.nhathuy.nextmeet.repository.UserRepository
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(@ApplicationContext private val context: Context,
                                        private val repository: UserRepository) : ViewModel() {

    private val _registrationState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val registrationState: StateFlow<Resource<Boolean>> = _registrationState

    private val _loginState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val loginState :StateFlow<Resource<User>> = _loginState

    private val _updateState = MutableStateFlow<Resource<Boolean>?>(null)
    val updateState : StateFlow<Resource<Boolean>?> = _updateState

    private val _logoutState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val logoutState : StateFlow<Resource<Boolean>> =_logoutState

    private val _passwordUpdateState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val passwordUpdateState : StateFlow<Resource<Boolean>> =_passwordUpdateState

    //form validation state
    private val _loginFormState = MutableStateFlow<Map<String,ValidationResult>>(emptyMap())
    val loginFormState : StateFlow<Map<String,ValidationResult>> = _loginFormState

    private val _registerFormState = MutableStateFlow<Map<String,ValidationResult>>(emptyMap())
    val registerFormState:StateFlow<Map<String,ValidationResult>> = _registerFormState

    private val _passwordResetFormState = MutableStateFlow<Map<String,ValidationResult>>(emptyMap())
    val passwordFormResetState : StateFlow<Map<String,ValidationResult>> = _passwordResetFormState

    // Remember me state
    private val _rememberMeState = MutableStateFlow(repository.isRememberMeEnabled())
    val rememberMeState: StateFlow<Boolean> = _rememberMeState

    //update user
    private val _userEditFormState = MutableStateFlow<Map<String, ValidationResult>>(emptyMap())
    val userEditFormState: StateFlow<Map<String, ValidationResult>> = _userEditFormState

    fun register(user: User) = viewModelScope.launch {
        repository.register(user).collect{
            result ->
            _registrationState.value = result
        }
    }

    fun login(phone: String, password: String,rememberMe:Boolean) = viewModelScope.launch {
        repository.login(phone, password,rememberMe).collect {
            result ->
            //Nếu đăng nhập thành công và rememberMe được bac, lưu so điện thoại
            if(result is Resource.Success){
                if(rememberMe){
                    repository.saveUserPhone(phone)
                }
                createLoginSession(result.data!!.id,rememberMe,phone)
            }
            _loginState.value = result
        }
    }
    /**
     * Tạo phiên đăng nhập mới trong SessionManager
     */
    fun createLoginSession(userId: Int, rememberMe: Boolean, phone: String) {
        repository.createLoginSession(userId, rememberMe, phone)
        _rememberMeState.value = rememberMe
    }
    fun updateUser(user: User) = viewModelScope.launch {
        repository.updateUser(user).collect {
            result ->
            _updateState.value = result
        }
    }

    fun getCurrentUser(): LiveData<User?> {
        return repository.getCurrentUser()
    }

    fun logout() = viewModelScope.launch {
        repository.logout().collect {
                result ->
            _logoutState.value = result
        }
    }

    fun updatePassword(phone: String, newPassword: String) = viewModelScope.launch {
        repository.updatePassword(phone, newPassword).collect {
            result ->
            _passwordUpdateState.value = result
        }
    }


    fun validateAndLogin(loginForm: LoginForm,rememberMe: Boolean):Boolean {
        val phoneValidation = ValidationUtils.validatePhone(context,loginForm.phone)
        val passwordValidation = ValidationUtils.validatePassword(context,loginForm.password)

        val validationResults = mapOf(
            "phone" to phoneValidation,
            "password" to passwordValidation
        )
        _loginFormState.value = validationResults

        val isValid = validationResults.all { it.value.isValid }

        if(isValid){
            login(loginForm.phone,loginForm.password,rememberMe)
            setRememberMe(rememberMe)
        }

        return isValid
    }

    /**
     * Kiểm tra số điện thoại đã tồn tại trong hệ thống chưa
     * @param phone Số điện thoại cần kiểm tra
     * @return true nếu số điện thoại đã tồn tại
     */
    suspend fun isPhoneExists(phone: String): Boolean {
        return repository.isPhoneExists(phone)
    }

    fun validateAndRegister(registerForm: RegistrationForm):Boolean {
        val phoneValidation = ValidationUtils.validatePhone(context,registerForm.phone)
        val emailValidation = ValidationUtils.validateEmail(context, registerForm.email)
        val nameValidation = ValidationUtils.validateName(context, registerForm.name)
        val passwordValidation = ValidationUtils.validatePassword(context, registerForm.password)
        val addressValidation = ValidationUtils.validateAddress(context, registerForm.address)
//        val coordinatesValidation = ValidationUtils.validateCoordinate(registerForm.latitude!!, registerForm.longitude!!)

        val validationResults = mapOf(
            "name" to nameValidation,
            "phone" to phoneValidation,
            "email" to emailValidation,
            "password" to passwordValidation,
            "address" to addressValidation,
//            "coordinates" to coordinatesValidation
        )

        viewModelScope.launch {
            val finalValidationResults = if(phoneValidation.isValid && isPhoneExists(registerForm.phone)){
                val phoneResults = validationResults.toMutableMap()
                phoneResults["phone"] = ValidationResult(
                    false,
                    context.getString(R.string.error_phone_exists)
                )
                phoneResults.toMap()
            }else {
                validationResults
            }

            _registerFormState.value = finalValidationResults

            val isValid = finalValidationResults.all { it.value.isValid }

            if(isValid){
                register(
                    User(
                        name = registerForm.name,
                        phone = registerForm.phone,
                        email = registerForm.email,
                        password = registerForm.password,
                        defaultAddress = registerForm.address,
                        defaultLatitude = registerForm.latitude,
                        defaultLongitude = registerForm.longitude
                    )
                )
            }
        }

        return false
    }
    /**
     * Validates password reset form and attempts password update if valid
     * @param form Password reset form data
     * @return Whether validation passed
     */
    fun validateAndUpdatePassword(form: PasswordResetForm): Boolean {
        val phoneValidation = ValidationUtils.validatePhone(context, form.phone)
        val newPasswordValidation = ValidationUtils.validatePassword(context, form.newPassword)
        val passwordsMatchValidation = ValidationUtils.validatePasswordMatch(context,
            form.newPassword,
            form.confirmPassword
        )

        val validationResults = mapOf(
            "phone" to phoneValidation,
            "newPassword" to newPasswordValidation,
            "confirmPassword" to passwordsMatchValidation
        )

        _passwordResetFormState.value = validationResults

        val isValid = validationResults.all { it.value.isValid }

        if (isValid) {
            updatePassword(form.phone, form.newPassword)
        }

        return isValid
    }
    /**
     * validate va update user information
     * @param user User object containing updated information
     * @return Whether validation passed
     */
    fun validateAndUpdateUser(user: User): Boolean {
        val nameValidation = ValidationUtils.validateName(context, user.name)
        val emailValidation = ValidationUtils.validateEmail(context, user.email)
        val phoneValidation = ValidationUtils.validatePhone(context, user.phone)
//        val addressValidation = ValidationUtils.validateAddress(user.defaultAddress)

        val validationResults = mapOf(
            "name" to nameValidation,
            "email" to emailValidation,
            "phone" to phoneValidation
        )

        _userEditFormState.value = validationResults

        val isValid = validationResults.all { it.value.isValid }
        if (isValid){
            updateUser(user)
        }
        return isValid
    }

    /**
     * Checks if a user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }

    /**
     * Updates the remember me preference
     */
    fun setRememberMe(enabled: Boolean) {
        repository.setRememberMe(enabled)
        _rememberMeState.value = enabled
    }

    /**
     * Kiểm tra Remember Me có được bật không
     */
    fun isRememberMeEnabled(): Boolean {
        return repository.isRememberMeEnabled()
    }

    /**
     * Lấy ID người dùng từ session
     */
    fun getUserId(): Int {
        return repository.getUserId()
    }

    /**
     * Lấy số điện thoại của người dùng đã lưu
     */
    fun getUserPhone(): String {
        return repository.getUserPhone()
    }
    /**
     * Cập nhật trạng thái và thông tin người dùng
     */
    fun resetUpdateState() {
        _updateState.value = null
    }


}