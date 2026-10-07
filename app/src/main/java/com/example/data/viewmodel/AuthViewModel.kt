package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.service.GoogleAuthService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleAuthService = GoogleAuthService(application)

    private val _isUserLoggedIn = MutableStateFlow(checkIfLoggedIn())
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn

    private val _userEmail = MutableStateFlow(getUserEmail())
    val userEmail: StateFlow<String> = _userEmail

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        auth.addAuthStateListener { firebaseAuth ->
            updateLoginState()
        }
    }

    private fun checkIfLoggedIn(): Boolean {
        return auth.currentUser != null || googleAuthService.getSignedInAccount() != null
    }

    private fun getUserEmail(): String {
        return auth.currentUser?.email ?: googleAuthService.getSignedInAccount()?.email ?: ""
    }

    fun updateLoginState() {
        _isUserLoggedIn.value = checkIfLoggedIn()
        _userEmail.value = getUserEmail()
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                updateLoginState()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                updateLoginState()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        googleAuthService.signOut()
        updateLoginState()
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }
}
