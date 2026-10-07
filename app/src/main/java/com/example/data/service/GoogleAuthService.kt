package com.example.data.service

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthService(private val context: Context) {
    
    companion object {
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }

    fun getSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))) {
            account
        } else {
            null
        }
    }
    
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()?.account
            if (account != null) {
                GoogleAuthUtil.getToken(context, account, "oauth2:\$DRIVE_SCOPE")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getSignInIntent(): Intent {
        return getSignInClient().signInIntent
    }
    
    fun signOut() {
        getSignInClient().signOut()
    }
}
