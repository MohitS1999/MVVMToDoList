package com.example.mvvmtodolist.ui.task

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmtodolist.R
import com.example.mvvmtodolist.Util.OnQueryTextChanged
import com.example.mvvmtodolist.Util.exhaustive
import com.example.mvvmtodolist.data.SortOrder
import com.example.mvvmtodolist.data.Task
import com.example.mvvmtodolist.databinding.FragmentsTaskBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragments_task.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment: Fragment(R.layout.fragments_task),TaskAdapter.OnItemClickListener {

    private val viewModel:TaskViewModel by viewModels()
    private val TAG = "TasksFragment"
    private lateinit var searchView:SearchView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentsTaskBinding.bind(view)

        val taskAdapter = TaskAdapter(this)

        binding.apply {
            recyclerViewTasks.apply {
                adapter = taskAdapter
                Log.d(TAG, "onViewCreated: set the recycler view")
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = taskAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTaskSwiped(task)
                }

            }).attachToRecyclerView(recyclerViewTasks)
        }

        fab_add_task.setOnClickListener{
            viewModel.onAddNewTaskClick()
        }

        setFragmentResultListener("add_edit_request") {_,bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }

        viewModel.tasks.observe(viewLifecycleOwner){
            Log.d(TAG, "observe: set the adapter")
            taskAdapter.submitList(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tasksEvent.collect{ event ->
                when (event){
                    is TaskViewModel.TasksEvent.showUndoDeleteTaskMessage -> {
                        Snackbar.make(requireView(),"Task deleted",Snackbar.LENGTH_LONG)
                            .setAction("UNDO"){
                                viewModel.onUndoDeleteClick(event.task)
                            }.show()
                    }
                    is TaskViewModel.TasksEvent.NavigateToAddTaskScreen -> {
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment("New Task",null)
                        findNavController().navigate(action)

                    }
                    is TaskViewModel.TasksEvent.NavigateToEditTaskScreen -> {
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment("Edit Task",event.task)
                        findNavController().navigate(action)
                    }
                    is TaskViewModel.TasksEvent.ShowTaskSavedConfirmationMessage -> {

                        Snackbar.make(requireView(),event.msg,Snackbar.LENGTH_SHORT).show()
                    }
                    TaskViewModel.TasksEvent.NavigateToDeleteAllCompletedScreen -> {
                        val action = TasksFragmentDirections.actionGlobalDeleteAllCompletedDialogFragment()
                        findNavController().navigate(action)

                    }
                }.exhaustive
            }
        }
        setHasOptionsMenu(true)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_tasks, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        // restore the previous query
        val pendingQuery = viewModel.searchQuery.value
        
        if (pendingQuery != null && pendingQuery.isNotEmpty()){
            searchItem.expandActionView()
            Log.d(TAG, "onCreateOptionsMenu: store the previous search query before the destory the fragment")
            searchView.setQuery(pendingQuery,false)
        }

        searchView.OnQueryTextChanged {
            Log.d(TAG, "onCreateOptionsMenu: when the query has changed in the search view")
            viewModel.searchQuery.value = it
        }

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideComplete
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_sort_by_name -> {
                viewModel.onSortOrderSeleted(SortOrder.BY_NAME)
                Log.d(TAG, "onOptionsItemSelected:  sort by name")
                true
            }
            R.id.action_sort_by_date_created -> {
                Log.d(TAG, "onOptionsItemSelected: sort by date created")
                viewModel.onSortOrderSeleted(SortOrder.BY_DATE)
                true
            }
            R.id.action_hide_completed_tasks -> {
                Log.d(TAG, "onOptionsItemSelected: hide the completed task")
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_deleted_all_completed_task -> {
                Log.d(TAG, "onOptionsItemSelected: delete all the task")
                viewModel.onDeleteAllCompletedClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    override fun onItemClick(task: Task) {
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckedChanged(task,isChecked)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: ")
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
    }

}