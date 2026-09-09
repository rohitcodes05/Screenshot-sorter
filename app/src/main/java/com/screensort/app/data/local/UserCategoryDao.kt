package com.screensort.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryDao {

    @Query("SELECT * FROM user_categories ORDER BY name ASC")
    fun getAllUserCategories(): Flow<List<UserCategoryEntity>>

    @Query("SELECT * FROM user_categories ORDER BY name ASC")
    suspend fun getAllUserCategoriesSync(): List<UserCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: UserCategoryEntity): Long

    @Update
    suspend fun update(category: UserCategoryEntity)

    @Delete
    suspend fun delete(category: UserCategoryEntity)
}
