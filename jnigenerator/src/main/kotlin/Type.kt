import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

typealias ToJniFun = (type: Type, envParam: ParameterSpec, inVar: String, outVar: String) -> (CodeBlock)
typealias FronJniFun = (type: Type, envParam: ParameterSpec, inParam: ParameterSpec, outVar: String) -> (CodeBlock)

open class Type(
    val kotlinDecl: String,
    val kotlinClass: TypeName,
    val nativeClass: TypeName,
    private val convertToJni: ToJniFun? = null,
    private val convertFromJni: FronJniFun? = null,
    val nullable: Boolean
) {
    constructor(
        kotlinClass: ClassName,
        nativeClass: TypeName,
        convertToJni: ToJniFun? = null,
        convertFromJni: FronJniFun? = null
    ) : this(
        kotlinDecl = kotlinClass.simpleName,
        kotlinClass = kotlinClass,
        nativeClass = nativeClass,
        convertToJni = convertToJni,
        convertFromJni = convertFromJni,
        nullable = false
    )

    fun hasConvertToJni() = convertToJni != null
    fun hasConvertFromJni() = convertFromJni != null

    fun convertToJni(envParam: ParameterSpec, inVar: String, outVar: String) =
        convertToJni!!(this, envParam, inVar, outVar)

    fun convertFromJni(envParam: ParameterSpec, inParam: ParameterSpec, outVar: String) =
        convertFromJni!!(this, envParam, inParam, outVar)

    fun copy(nullable: Boolean) = Type(
        kotlinDecl = kotlinDecl,
        kotlinClass = kotlinClass.copy(nullable = nullable),
        nativeClass = nativeClass.copy(nullable = nullable),
        convertFromJni = convertFromJni,
        convertToJni = convertToJni,
        nullable = nullable
    )

    class BridgeType(kotlinClass: ClassName) : Type(
        kotlinClass = kotlinClass,
        nativeClass = ClassNames.JLONG,
        convertFromJni = { type, envParam, inParam, outVar ->
            CodeBlock.builder().apply {
                addStatement("val %L = %T.fromJni(%N)", outVar, kotlinClass, inParam)
            }.build()
        },
        convertToJni = { type, envParam, inVar, outVar ->
            CodeBlock.builder().apply {
                addStatement("val %L = %L.toJni()", outVar, inVar)
            }.build()
        }
    )

    class StringType() : Type(
        kotlinClass = String::class.asClassName(),
        nativeClass = ClassNames.JSTRING,
        convertToJni = { type, envParam, inVar, outVar ->
            CodeBlock.builder().apply {
                if (type.nullable) {
                    addStatement("val %L = %L?.toJString(%N)", outVar, inVar, envParam)
                } else {
                    addStatement("val %L = checkNotNull(%L.toJString(%N))", outVar, inVar, envParam)
                }

            }.build()
        },
        convertFromJni = { type, envParam, inParam, outVar ->
            CodeBlock.builder().apply {
                if (type.nullable) {
                    addStatement("val %L = %N.toKString(%N)", outVar, inParam, envParam)
                } else {
                    addStatement("val %L = checkNotNull(%N.toKString(%N))", outVar, inParam, envParam)
                }

            }.build()
        }
    )

    companion object {
        private val types: List<Type> by lazy {
            val self = this
            this::class.java.methods.filter {
                Type::class.java.isAssignableFrom(it.returnType) && it.name.startsWith("get")
            }.map {
                it.invoke(self) as Type
            }
        }

        fun resolve(kotlinType: String, nullable: Boolean) = types.firstOrNull {
            it.kotlinDecl == kotlinType
        }?.let {
            it.copy(nullable = nullable)
        } ?: error("cannot resolve type $kotlinType")

        val INT = Type(Int::class.asClassName(), ClassNames.JINT)
        val STRING = StringType()
        val DBREF = BridgeType(ClassNames.DB_REF)
        val STMTREF = BridgeType(ClassNames.STMT_REF)
        val LONG = Type(Long::class.asClassName(), ClassNames.JLONG)
        val BOOLEAN = Type(Boolean::class.asClassName(), ClassNames.JBOOLEAN,
            convertFromJni = { type, envParam, inParam, outVar ->
                buildCodeBlock {
                    addStatement("val %L = %N.toKBoolean()", outVar, inParam)
                }
            },
            convertToJni = { type, envParam, inVar, outVar ->
                buildCodeBlock {
                    addStatement("val %L = %L.toJBoolean()", outVar, inVar)
                }
            })
        val RESULT_CODE = Type(ClassNames.RESULT_CODE, ClassNames.RESULT_CODE)
    }
}

fun String.resolveType(nullable: Boolean) = Type.resolve(this, nullable)