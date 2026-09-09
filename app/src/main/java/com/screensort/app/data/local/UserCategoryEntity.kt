package com.screensort.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a user-defined custom category with optional matching keywords.
 */
@Entity(tableName = "user_categories")
data class UserCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val keywords: String = "",
    val dateCreated: Long = System.currentTimeMillis()
) {
    /**
     * Parses comma-separated keywords and trims them.
     */
    fun getKeywordList(): List<String> {
        return keywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }
}
