package com.example.guardianai.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object SettingsManager {
    private const val PREFS_NAME = "guardian_settings"
    private const val KEY_STRICT_MODE = "strict_mode"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_TRUSTED_APPS = "trusted_apps"
    private const val KEY_TRUSTED_CONTACTS = "trusted_contacts"

    // Наблюдаемые состояния UI
    var isStrictMode = mutableStateOf(false)
    var isProtectionEnabled = mutableStateOf(true) // По умолчанию включено
    var themeMode = mutableStateOf(0)
    
    // Белые списки
    var trustedApps = mutableStateOf(setOf<String>())
    var trustedContacts = mutableStateOf(setOf<String>())

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isStrictMode.value = prefs.getBoolean(KEY_STRICT_MODE, false)
        isProtectionEnabled.value = prefs.getBoolean("protection_enabled", true)
        themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
        
        // Загрузка списков (копия для безопасности, так как SharedPreferences может вернуть тот же инстанс)
        trustedApps.value = prefs.getStringSet(KEY_TRUSTED_APPS, emptySet())?.toSet() ?: emptySet()
        trustedContacts.value = prefs.getStringSet(KEY_TRUSTED_CONTACTS, emptySet())?.toSet() ?: emptySet()
    }



    fun addTrustedApp(context: Context, packageName: String) {
        val newSet = trustedApps.value + packageName
        trustedApps.value = newSet
        saveSet(context, KEY_TRUSTED_APPS, newSet)
    }

    fun removeTrustedApp(context: Context, packageName: String) {
        val newSet = trustedApps.value - packageName
        trustedApps.value = newSet
        saveSet(context, KEY_TRUSTED_APPS, newSet)
    }

    fun addTrustedContact(context: Context, contactName: String) {
        val newSet = trustedContacts.value + contactName
        trustedContacts.value = newSet
        saveSet(context, KEY_TRUSTED_CONTACTS, newSet)
    }

    fun removeTrustedContact(context: Context, contactName: String) {
        val newSet = trustedContacts.value - contactName
        trustedContacts.value = newSet
        saveSet(context, KEY_TRUSTED_CONTACTS, newSet)
    }

    private fun saveSet(context: Context, key: String, set: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, set)
            .apply()
    }

    fun setStrictMode(context: Context, enabled: Boolean) {
        isStrictMode.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STRICT_MODE, enabled)
            .apply()
    }

    fun setThemeMode(context: Context, mode: Int) {
        themeMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
    }
    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        isProtectionEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("protection_enabled", enabled)
            .apply()
    }
}
