package com.example.mvvmtodolist.ui.task

import android.util.Log
import androidx.lifecycle.*
import com.example.mvvmtodolist.data.Dao
import com.example.mvvmtodolist.data.PreferenceManager
import com.example.mvvmtodolist.data.SortOrder
import com.example.mvvmtodolist.data.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskDao: Dao,
    private val preferencesManager: PreferenceManager,
    private val state:SavedStateHandle
) : ViewModel() {
    private val TAG = "TaskViewModel"



    val searchQuery = state.getLiveData("searchQuery","")

    val preferencesFlow = preferencesManager.preferenceFlow

    private val tasksEventChannel = Channel<TasksEvent>()

    val tasksEvent = tasksEventChannel.receiveAsFlow()

    private val tasksFlow = combine(searchQuery.asFlow(),preferencesFlow) {
        query,filterPreferences -> Pair(query,filterPreferences)
    }.flatMapLatest {
        (query,filterPreferences) -> taskDao.getTask(query,filterPreferences.sortOrder,filterPreferences.hideComplete)
    }

    val tasks =tasksFlow.asLiveData()

    fun onSortOrderSeleted(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted:Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

   fun onTaskSelected(task: Task) = viewModelScope.launch{
       tasksEventChannel.send(TasksEvent.NavigateToEditTaskScreen(task))
   }

    fun onTaskCheckedChanged(task: Task,isChecked:Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)
        tasksEventChannel.send(TasksEvent.showUndoDeleteTaskMessage(task))
    }

    fun onUndoDeleteClick(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClick() = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToAddTaskScreen)
    }

    fun onAddEditResult(result :Int){
        when(result){
            ADD_TASK_RESULT_OK ->   showTaskSavedConfirmationMessage("Task added")
            EDIT_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task updated")
        }
    }
    private fun showTaskSavedConfirmationMessage(text:String) = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.ShowTaskSavedConfirmationMessage(text))
    }

    fun onDeleteAllCompletedClick() = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToDeleteAllCompletedScreen)
    }

    sealed class TasksEvent{
        object NavigateToAddTaskScreen : TasksEvent()
        data class NavigateToEditTaskScreen(val task: Task) : TasksEvent()
        data class showUndoDeleteTaskMessage(val task: Task) : TasksEvent()
        data class ShowTaskSavedConfirmationMessage(val msg:String) :TasksEvent()
        object NavigateToDeleteAllCompletedScreen : TasksEvent()
    }
}

