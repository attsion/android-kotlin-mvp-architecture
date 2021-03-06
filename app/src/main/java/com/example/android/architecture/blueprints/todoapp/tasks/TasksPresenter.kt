/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.tasks

import android.app.Activity

import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository


import java.util.ArrayList

import com.google.common.base.Preconditions.checkNotNull

/**
 * Listens to user actions from the UI ([TasksFragment]), retrieves the data and updates the
 * UI as required.
 */
class TasksPresenter(tasksRepository: TasksRepository, tasksView: TasksContract.View) : TasksContract.Presenter {

    override var filtering: TasksFilterType
        get() = mCurrentFiltering
        set(filterType) {
            mCurrentFiltering = filtering
        }

    private val mTasksRepository: TasksRepository

    private val mTasksView: TasksContract.View

    private var mCurrentFiltering = TasksFilterType.ALL_TASKS

    private var mFirstLoad = true

    init {
        mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null")
        mTasksView = checkNotNull(tasksView, "tasksView cannot be null!")

        mTasksView.setPresenter(this)
    }

    override fun start() {
        loadTasks(false)
    }

    override fun result(requestCode: Int, resultCode: Int) {
        // If a task was successfully added, show snackbar
        if (AddEditTaskActivity.REQUEST_ADD_TASK == requestCode && Activity.RESULT_OK == resultCode) {
            mTasksView.showSuccessfullySavedMessage()
        }
    }

    override fun loadTasks(forceUpdate: Boolean) {
        // Simplification for sample: a network reload will be forced on first load.
        loadTasks(forceUpdate || mFirstLoad, true)
        mFirstLoad = false
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the [TasksDataSource]
     * *
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private fun loadTasks(forceUpdate: Boolean, showLoadingUI: Boolean) {
        if (showLoadingUI) {
            mTasksView.setLoadingIndicator(true)
        }
        if (forceUpdate) {
            mTasksRepository.refreshTasks()
        }

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.


        mTasksRepository.getTasks(object : TasksDataSource.LoadTasksCallback {
            override fun onTasksLoaded(tasks: List<Task>) {
                val tasksToShow = ArrayList<Task>()

                // This callback may be called twice, once for the cache and once for loading
                // the data from the server API, so we check before decrementing, otherwise
                // it throws "Counter has been corrupted!" exception.


                // We filter the tasks based on the requestType
                for (task in tasks) {
                    when (mCurrentFiltering) {
                        TasksFilterType.ALL_TASKS -> tasksToShow.add(task)
                        TasksFilterType.ACTIVE_TASKS -> if (task.isActive) {
                            tasksToShow.add(task)
                        }
                        TasksFilterType.COMPLETED_TASKS -> if (task.isCompleted) {
                            tasksToShow.add(task)
                        }
                        else -> tasksToShow.add(task)
                    }
                }
                // The view may not be able to handle UI updates anymore
                if (!mTasksView.isActive) {
                    return
                }
                if (showLoadingUI) {
                    mTasksView.setLoadingIndicator(false)
                }

                processTasks(tasksToShow)
            }

            override fun onDataNotAvailable() {
                // The view may not be able to handle UI updates anymore
                if (!mTasksView.isActive) {
                    return
                }
                mTasksView.showLoadingTasksError()
            }
        })
    }

    private fun processTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            // Show a message indicating there are no tasks for that filter type.
            processEmptyTasks()
        } else {
            // Show the list of tasks
            mTasksView.showTasks(tasks)
            // Set the filter label's text.
            showFilterLabel()
        }
    }

    private fun showFilterLabel() {
        when (mCurrentFiltering) {
            TasksFilterType.ACTIVE_TASKS -> mTasksView.showActiveFilterLabel()
            TasksFilterType.COMPLETED_TASKS -> mTasksView.showCompletedFilterLabel()
            else -> mTasksView.showAllFilterLabel()
        }
    }

    private fun processEmptyTasks() {
        when (mCurrentFiltering) {
            TasksFilterType.ACTIVE_TASKS -> mTasksView.showNoActiveTasks()
            TasksFilterType.COMPLETED_TASKS -> mTasksView.showNoCompletedTasks()
            else -> mTasksView.showNoTasks()
        }
    }

    override fun addNewTask() {
        mTasksView.showAddTask()
    }

    override fun openTaskDetails(requestedTask: Task) {
        checkNotNull(requestedTask, "requestedTask cannot be null!")
        mTasksView.showTaskDetailsUi(requestedTask.id)
    }

    override fun completeTask(completedTask: Task) {
        checkNotNull(completedTask, "completedTask cannot be null!")
        mTasksRepository.completeTask(completedTask)
        mTasksView.showTaskMarkedComplete()
        loadTasks(false, false)
    }

    override fun activateTask(activeTask: Task) {
        checkNotNull(activeTask, "activeTask cannot be null!")
        mTasksRepository.activateTask(activeTask)
        mTasksView.showTaskMarkedActive()
        loadTasks(false, false)
    }

    override fun clearCompletedTasks() {
        mTasksRepository.clearCompletedTasks()
        mTasksView.showCompletedTasksCleared()
        loadTasks(false, false)
    }

    /**
     * Sets the current task filtering type.

     * @param requestType Can be [TasksFilterType.ALL_TASKS],
     * *                    [TasksFilterType.COMPLETED_TASKS], or
     * *                    [TasksFilterType.ACTIVE_TASKS]
     */

}
