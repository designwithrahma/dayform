package com.example.ui.navigation

import com.example.ui.MainTab

object DayformRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TASKS = "tasks"
    const val CALENDAR = "calendar"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"

    const val ARG_TASK_ID = "taskId"
    const val ARG_ORIGIN = "origin"
    const val ARG_EPOCH_DAY = "epochDay"

    const val TASK_DETAILS_PATTERN = "task_details/{$ARG_TASK_ID}?$ARG_ORIGIN={$ARG_ORIGIN}"
    const val CREATE_TASK_PATTERN = "create_task?$ARG_EPOCH_DAY={$ARG_EPOCH_DAY}"
    const val EDIT_TASK_PATTERN = "edit_task/{$ARG_TASK_ID}"

    fun taskDetails(taskId: Long, origin: String = HOME): String {
        return "task_details/$taskId?$ARG_ORIGIN=$origin"
    }

    fun createTask(epochDay: Long? = null): String {
        return if (epochDay != null) {
            "create_task?$ARG_EPOCH_DAY=$epochDay"
        } else {
            "create_task"
        }
    }

    fun editTask(taskId: Long): String {
        return "edit_task/$taskId"
    }

    fun tabOrderIndex(route: String?): Int {
        return when {
            route == HOME -> 0
            route == TASKS || route == CALENDAR -> 1
            route == INSIGHTS -> 2
            route == PROFILE -> 3
            else -> 0
        }
    }

    fun routeToMainTab(route: String?): MainTab? {
        return when (route) {
            HOME -> MainTab.HOME
            TASKS, CALENDAR -> MainTab.TASKS
            INSIGHTS -> MainTab.INSIGHTS
            PROFILE -> MainTab.PROFILE
            else -> null
        }
    }

    fun mainTabToRoute(tab: MainTab): String {
        return when (tab) {
            MainTab.HOME -> HOME
            MainTab.TASKS -> TASKS
            MainTab.INSIGHTS -> INSIGHTS
            MainTab.PROFILE -> PROFILE
        }
    }
}
