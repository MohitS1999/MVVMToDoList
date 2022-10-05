package com.example.mvvmtodolist.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.createDataStore

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.emptyPreferences
import androidx.datastore.preferences.preferencesKey
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PreferenceManager"

enum class SortOrder{
    BY_NAME,BY_DATE
}

data class FilterPreferences(val sortOrder: SortOrder,val hideComplete:Boolean)

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext context:Context) {

    private val dataStore = context.createDataStore("user_preferences")

    val preferenceFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException){
                Log.e(TAG, "Error reading preferences: ",exception )
                emit(emptyPreferences())
            }else{
                throw exception
            }
        }
        .map { preferences ->

            val sortOrder = SortOrder.valueOf(
                preferences[PreferenceKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name
            )
            val hideComplete = preferences[PreferenceKeys.HIDE_COMPLETED] ?: false
            FilterPreferences(sortOrder,hideComplete)
        }

    suspend fun updateSortOrder(sortOrder: SortOrder){
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateHideCompleted(hideComplete: Boolean){
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HIDE_COMPLETED] = hideComplete
        }
    }

    private object PreferenceKeys{
        val SORT_ORDER = preferencesKey<String>("sort_order")
        val HIDE_COMPLETED = preferencesKey<Boolean>("hide_complete")
    }

}