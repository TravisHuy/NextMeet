package com.nhathuy.nextmeet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.repository.UserRepository
import com.nhathuy.nextmeet.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val repository: UserRepository) : ViewModel() {

    private val _registrationState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val registrationState: StateFlow<Resource<Boolean>> = _registrationState

    private val _loginState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val loginState :StateFlow<Resource<User>> = _loginState

    private val _updateState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val updateState : StateFlow<Resource<Boolean>> =_updateState

    private val _logoutState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val logoutState : StateFlow<Resource<Boolean>> =_logoutState

    private val _passwordUpdateState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val passwordUpdateState : StateFlow<Resource<Boolean>> =_passwordUpdateState

    fun register(user: User) = viewModelScope.launch {
        repository.register(user).collect{
            result ->
            _registrationState.value = result
        }
    }

    fun login(phone: String, password: String) = viewModelScope.launch {
        repository.login(phone, password).collect {
            result ->
            _loginState.value = result
        }
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
}