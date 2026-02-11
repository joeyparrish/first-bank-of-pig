package io.github.joeyparrish.fbop.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.github.joeyparrish.fbop.data.model.AppConfig
import io.github.joeyparrish.fbop.data.model.AppMode

class ConfigRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getConfig(): AppConfig {
        val modeString = prefs.getString(KEY_MODE, null)
        val mode = modeString?.let {
            try {
                AppMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                AppMode.NOT_CONFIGURED
            }
        } ?: AppMode.NOT_CONFIGURED

        return AppConfig(
            mode = mode,
            familyId = prefs.getString(KEY_FAMILY_ID, null),
            childId = prefs.getString(KEY_CHILD_ID, null),
            lookupCode = prefs.getString(KEY_LOOKUP_CODE, null)
        )
    }

    fun setParentMode(familyId: String) {
        prefs.edit()
            .putString(KEY_MODE, AppMode.PARENT.name)
            .putString(KEY_FAMILY_ID, familyId)
            .remove(KEY_CHILD_ID)
            .remove(KEY_LOOKUP_CODE)
            .apply()
    }

    fun setKidMode(familyId: String, childId: String, lookupCode: String) {
        prefs.edit()
            .putString(KEY_MODE, AppMode.KID.name)
            .putString(KEY_FAMILY_ID, familyId)
            .putString(KEY_CHILD_ID, childId)
            .putString(KEY_LOOKUP_CODE, lookupCode)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "fbop_config"
        private const val KEY_MODE = "mode"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_CHILD_ID = "child_id"
        private const val KEY_LOOKUP_CODE = "lookup_code"
    }
}
