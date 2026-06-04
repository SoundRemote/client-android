package io.github.soundremote.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import io.github.soundremote.data.room.AppDatabase
import io.github.soundremote.data.room.DatabaseMigrations
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MigrationTest {
    private val testDB = "migration-test"

    private val migrations = listOf(
        DatabaseMigrations.Schema1to2(),
        DatabaseMigrations.Schema2to3(),
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        migrations,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create the earliest version of the database.
        helper.createDatabase(testDB, 1).apply {
            close()
        }

        // Open latest version of the database. Room validates the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDB
        ).build().apply { openHelper.writableDatabase.close() }
    }
}
