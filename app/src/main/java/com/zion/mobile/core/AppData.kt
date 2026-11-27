package com.zion.mobile.core

import android.content.Context
import android.content.pm.PackageManager

class AppData(private val context: Context) {

    fun loadInstalledApps(): Map<String, String> {
        val apps = mutableMapOf<String, String>()

        val packages = context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            val label = context.packageManager
                .getApplicationLabel(app)
                .toString()
                .lowercase()

            apps[label] = app.packageName
        }

        return apps
    }
}