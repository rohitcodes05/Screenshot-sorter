package com.screensort.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.screensort.app.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun testMigration3To4CreatesExpectedIndices() {
        val executedSql = mutableListOf<String>()
        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_3_4.migrate(mockDb)

        assertEquals(2, executedSql.size)
        assertTrue(
            "Expected index on category and dateAdded",
            executedSql[0].contains("index_screenshots_category_dateAdded") &&
                executedSql[0].contains("`category`, `dateAdded`")
        )
        assertTrue(
            "Expected index on dateAdded",
            executedSql[1].contains("index_screenshots_dateAdded") &&
                executedSql[1].contains("`dateAdded`")
        )
    }
}
