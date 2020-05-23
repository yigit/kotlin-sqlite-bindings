import org.jetbrains.kotlin.spec.grammar.tools.parseKotlinCode
import java.io.File

/**
 * TODO: should we make this part of the build?
 */
fun main() {
    val srcFile = File("./sqlitebindings/src/jvmMain/kotlin/com/birbit/sqlite3/internal/JvmSqliteApi.kt")
    val targetFile = File("./sqlitebindings/src/nativeMain")
    println("hello ${File(".").absolutePath}")
    val tokens = parseKotlinCode(srcFile.readText(Charsets.UTF_8))
    val sqliteApiObject = tokens.objectDeclarations().first {
        it.name == "SqliteApi"
    }

    val methods = sqliteApiObject.functions.groupBy {
        it.external
    }
    val externalMethods = methods[true] ?: error("no external method?")
    val actualMethods = methods[false]?.associateBy {
        "native${it.name.capitalize()}"
    } ?: error("no actual methods?")
    val pairs = externalMethods.map { native ->
        val actualMethod = checkNotNull(actualMethods[native.name]) {
            "cannot find actual method for  ${native}"
        }
        FunctionPair(
            actualFun = actualMethod,
            nativeFun = native
        )
    }
    println(pairs)
    JniWriter(pairs).write(targetFile)
}
