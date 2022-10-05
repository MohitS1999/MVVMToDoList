package com.example.mvvmtodolist.data

import androidx.room.*
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("Delete From task_table Where completed = 1")
    suspend fun deleteCompletedTasks()

    fun getTask(query:String, sortOrder: SortOrder,hideCompleted: Boolean) : Flow<List<Task>> =
        when(sortOrder){
            SortOrder.BY_DATE -> getTaskSortedByDateCreated(query,hideCompleted)
            SortOrder.BY_NAME -> getTaskSortedByName(query,hideCompleted)
        }

    @Query("Select * from task_table where (completed != :hideCompleted OR completed = 0) AND name LIKE '%' || :searchQuery || '%' order by important desc,name")
    fun getTaskSortedByName(searchQuery: String,hideCompleted: Boolean) : Flow<List<Task>>

    @Query("Select * from task_table where (completed != :hideCompleted OR completed = 0) AND name LIKE '%' || :searchQuery || '%' order by important desc,created")
    fun getTaskSortedByDateCreated(searchQuery: String,hideCompleted: Boolean) : Flow<List<Task>>

}