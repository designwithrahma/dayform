package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CategoryEntity
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayformDao {

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateEpochDay ASC, sortOrder ASC, createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromTasks(categoryId: Long)

    @Query("UPDATE tasks SET categoryId = :targetCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun moveTasksToCategory(oldCategoryId: Long, targetCategoryId: Long?)

    // Subtasks
    @Query("SELECT * FROM subtasks ORDER BY sortOrder ASC, id ASC")
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSubtasksOnce(): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    suspend fun getSubtasksForTaskOnce(taskId: Long): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Update
    suspend fun updateSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtaskById(subtaskId: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Transaction
    suspend fun safeDeleteCategory(categoryId: Long, moveTasksToCategoryId: Long? = null) {
        if (moveTasksToCategoryId != null) {
            moveTasksToCategory(categoryId, moveTasksToCategoryId)
        } else {
            clearCategoryFromTasks(categoryId)
        }
        deleteCategoryById(categoryId)
    }
}
