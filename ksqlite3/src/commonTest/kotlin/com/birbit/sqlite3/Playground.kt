package com.birbit.sqlite3

import com.birbit.sqlite3.OsSpecificTestUtils.threadId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * This is not a real test, rather handy setup to play w/ the APIs.
 */
class Playground {
    @Test
    fun coroutine() {
        withTmpFolder {
            val path = getFilePath("test.db")
            OsSpecificTestUtils.myRunBlocking {
                fun getConn() = SqliteConnection.openConnection(path).also {
//                    it.exec("PRAGMA journal_mode=WAL")
                }
                val createTable = GlobalScope.launch(Dispatchers.Default) {
                    getConn().use {
                        it.exec("CREATE TABLE Foo(version INT, value TEXT)")
                    }
                    println("created")
                }
                createTable.join()
                val inserted = CompletableDeferred<Unit>()
                val read = CompletableDeferred<List<Pair<Int, String?>>>()
                val insert1 = GlobalScope.launch(Dispatchers.Default) {
                    val conn = getConn()
                    println("${threadId()} begin insert transactio}")
                    conn.exec("BEGIN EXCLUSIVE TRANSACTION")
                    conn.exec("INSERT INTO Foo VALUES(1, 'a')")
                    println("delaying insert transaction")
                    delay(2000)
                    println("will commit insert")
                    conn.exec("COMMIT")
                    println("committed insert")
                    conn.close()
                    println("inserted")
                    inserted.complete(Unit)
                }
                val reader = GlobalScope.launch(Dispatchers.Default) {
                    delay(1000)
                    println("${threadId()} reading")
                    read.complete(getConn().use {
                        println("using connection ${threadId()}")
                        it.exec("PRAGMA busy_timeout=10000")
                        it.query("SELECT * FROM Foo") {
                            it.map {
                                it.readInt(0) to it.readString(1)
                            }.toList()
                        }
                    })
                    println("read")
                }
                withTimeout(3_000) {
                    assertEquals(read.await(), listOf(1 to "a"))
                }
            }
        }
    }
}