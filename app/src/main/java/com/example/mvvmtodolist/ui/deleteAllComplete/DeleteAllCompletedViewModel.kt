package com.example.mvvmtodolist.ui.deleteAllComplete

import androidx.lifecycle.ViewModel
import com.example.mvvmtodolist.data.Dao
import com.example.mvvmtodolist.dependency_injection.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAllCompletedViewModel @Inject constructor(
    private val taskDao: Dao,
    @ApplicationScope private val applicationScope: CoroutineScope
) :ViewModel(){

    fun onConfirmClick() = applicationScope.launch {
        taskDao.deleteCompletedTasks()
    }

}