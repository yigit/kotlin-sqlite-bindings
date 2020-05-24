package com.birbit.sqlite3

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenDatabaseTest {
    @Test
    fun openDb() {
        SqliteConnection.openConnection(":memory:")
        // cool
    }
}