package com.example.fintrack.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.fintrack.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages active user authentication session and multi-user profile switching.
 * Guarantees that all database queries are bound to the currently authenticated user.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "fintrack_session_prefs"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_ACTIVE_USER_NAME = "active_user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LAST_ACTIVITY = "last_activity_time"
        private const val KEY_HAS_ONBOARDED = "has_completed_onboarding"
        private const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes of inactivity
    }

    private val _activeUserIdFlow = MutableStateFlow(getActiveUserId())
    val activeUserIdFlow: StateFlow<String> = _activeUserIdFlow.asStateFlow()

    private val _activeUserNameFlow = MutableStateFlow(getActiveUserName())
    val activeUserNameFlow: StateFlow<String> = _activeUserNameFlow.asStateFlow()

    val hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_ONBOARDED, false)

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_HAS_ONBOARDED, true).apply()
    }

    fun getActiveUserId(): String {
        return prefs.getString(KEY_ACTIVE_USER_ID, User.DEFAULT_USER_ID) ?: User.DEFAULT_USER_ID
    }

    fun getActiveUserName(): String {
        val stored = prefs.getString(KEY_ACTIVE_USER_NAME, null)
        if (stored.isNullOrBlank() || stored == "User" || stored == "Primary User" || stored == "Default User") {
            return "Yatin Kumar Singh"
        }
        return stored
    }

    fun setActiveUserName(name: String) {
        val cleanName = name.trim().ifBlank { "User" }
        prefs.edit().putString(KEY_ACTIVE_USER_NAME, cleanName).apply()
        _activeUserNameFlow.value = cleanName
    }

    fun isSessionValid(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return false

        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        val now = System.currentTimeMillis()
        if (now - lastActivity > SESSION_TIMEOUT_MS) {
            // Session expired due to inactivity
            logout()
            return false
        }
        touchActivity()
        return true
    }

    fun login(userId: String = User.DEFAULT_USER_ID, userName: String = "Yatin Kumar Singh") {
        val effectiveName = if (userName.isBlank() || userName == "User" || userName == "Primary User") "Yatin Kumar Singh" else userName
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(KEY_ACTIVE_USER_NAME, effectiveName)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_HAS_ONBOARDED, true)
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
        _activeUserIdFlow.value = userId
        _activeUserNameFlow.value = effectiveName
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putLong(KEY_LAST_ACTIVITY, 0L)
            .apply()
    }

    fun switchUser(userId: String, userName: String) {
        val effectiveName = if (userName.isBlank() || userName == "User" || userName == "Primary User") "Yatin Kumar Singh" else userName
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(KEY_ACTIVE_USER_NAME, effectiveName)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_HAS_ONBOARDED, true)
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
        _activeUserIdFlow.value = userId
        _activeUserNameFlow.value = effectiveName
    }

    fun touchActivity() {
        prefs.edit()
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }
}
