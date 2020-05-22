import com.birbit.sqlite3.openConnection
import kotlin.test.Test
import kotlin.test.assertEquals
class NativeTest {
    @Test
    fun openDb() {
        val db = openConnection("foo")
        assertEquals("3.31.1", db.version(), "can open db")
    }

    @Test
    fun simpleStatement() {
        val db = openConnection(":memory:")
        val stmt = db.prepareStmt("SELECT SQLITE_VERSION() as FOO")
        stmt.step()
        val firstColumn = stmt.readString(0)
        assertEquals("3.31.1", firstColumn)
    }
}