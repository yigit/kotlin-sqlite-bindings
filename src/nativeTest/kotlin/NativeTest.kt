import com.birbit.jni.KSqliteDb
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTest {
    @Test
    fun blah() {
        assertEquals(0, KSqliteDb.create(), "can open db")
        assertEquals("a", "a", "fails")
    }
}