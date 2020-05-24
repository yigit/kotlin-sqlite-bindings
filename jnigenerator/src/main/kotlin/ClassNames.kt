import com.squareup.kotlinpoet.ClassName

private inline fun jniType(value: String) = ClassName("com.birbit.jni", value)
private inline fun internalType(value: String) = ClassName("com.birbit.sqlite3.internal", value)
private inline fun interopType(value: String) = ClassName("kotlinx.cinterop", value)

object ClassNames {
    val JBYTEARRAY = jniType("jbyteArray")
    val RESULT_CODE = internalType("ResultCode")
    val JINT = jniType("jint")
    val JLONG = jniType("jlong")
    val JBOOLEAN = jniType("jboolean")
    val JSTRING = jniType("jstring")
    val CNAME = ClassName("kotlin.native", "CName")
    val CPOINTER = interopType("CPointer")
    val CPOINTER_VGAR_OF = interopType("CPointerVarOf")
    val JNI_ENV_VAR = jniType("JNIEnvVar")
    val JCLASS = jniType("jclass")
    val SQLITE_API = internalType("SqliteApi")
    val DB_REF = internalType("DbRef")
    val STMT_REF = internalType("StmtRef")
}
