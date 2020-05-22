import com.birbit.jni.KSqliteDb
import com.birbit.sqlite3.openDb
import kotlin.test.Test
import kotlin.test.assertEquals
class NativeTest {
    @Test
    fun blah() {
        val db = openDb("foo")
        assertEquals("3.31.1", db.version(), "can open db")
    }
}