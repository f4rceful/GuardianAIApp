package com.example.guardianai.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

object AppManager {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(0)
        
        return apps.filter { 
            val appInfo = it.applicationInfo
            if (appInfo == null) return@filter false
            
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            
            pm.getApplicationLabel(appInfo).toString().isNotBlank() &&
            (!isSystemApp || isUpdatedSystemApp)
        }.map {
            val appInfo = it.applicationInfo!!
            AppInfo(
                name = pm.getApplicationLabel(appInfo).toString(),
                packageName = it.packageName,
                icon = pm.getApplicationIcon(appInfo)
            )
        }.sortedBy { it.name }
    }
}
