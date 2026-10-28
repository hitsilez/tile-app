package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Job
import com.example.model.UserProfile

class LocalStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tile_calc_storage", Context.MODE_PRIVATE)

    companion object {
        const val KEY_JOBS = "tile_calc_jobs"
        const val KEY_ACTIVE_JOB_ID = "tile_calc_active_job_id"
        const val KEY_USER_PROFILE = "tile_calc_user_profile"
        const val KEY_SEEN_HELP = "tile_calc_seen_help"
    }

    fun hasSeenHelp(): Boolean = try {
        prefs.getBoolean(KEY_SEEN_HELP, false)
    } catch (e: Exception) {
        false
    }

    fun setSeenHelp(seen: Boolean = true) {
        try {
            prefs.edit().putBoolean(KEY_SEEN_HELP, seen).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun saveJobs(jobs: List<Job>) {
        try {
            val json = JobJsonSerializer.serializeJobs(jobs)
            prefs.edit().putString(KEY_JOBS, json).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun loadJobs(): List<Job> {
        return try {
            val json = prefs.getString(KEY_JOBS, null)
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                JobJsonSerializer.deserializeJobs(json)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveActiveJobId(id: String?) {
        try {
            prefs.edit().putString(KEY_ACTIVE_JOB_ID, id ?: "").apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun loadActiveJobId(): String? {
        return try {
            val id = prefs.getString(KEY_ACTIVE_JOB_ID, null)
            if (id.isNullOrBlank()) null else id
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        try {
            val json = JobJsonSerializer.serializeUserProfile(profile)
            prefs.edit().putString(KEY_USER_PROFILE, json).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun loadUserProfile(): UserProfile {
        return try {
            val json = prefs.getString(KEY_USER_PROFILE, null)
            if (json.isNullOrBlank()) {
                UserProfile()
            } else {
                JobJsonSerializer.deserializeUserProfile(json)
            }
        } catch (e: Exception) {
            UserProfile()
        }
    }

    fun exportBackup(): String {
        return try {
            val jobs = loadJobs()
            val activeId = loadActiveJobId()
            val profile = loadUserProfile()
            JobJsonSerializer.createBackupJson(jobs, activeId, profile)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun importBackup(jsonStr: String): Boolean {
        return try {
            val backup = JobJsonSerializer.parseBackupJson(jsonStr) ?: return false
            val (jobs, activeId, profile) = backup
            saveJobs(jobs)
            saveActiveJobId(activeId)
            saveUserProfile(profile)
            true
        } catch (e: Exception) {
            false
        }
    }
}
