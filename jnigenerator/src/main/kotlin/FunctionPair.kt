data class FunctionPair(
    val actualFun : FunctionDeclaration,
    val nativeFun : FunctionDeclaration
) {
    val jniSignature: String
        // TODO this is probably more complicated
        get() = "Java_com_birbit_sqlite3_internal_SqliteApi_${nativeFun.name}"

}