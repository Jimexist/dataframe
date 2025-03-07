package org.jetbrains.dataframe.impl.codeGen

import org.jetbrains.kotlinx.dataframe.codeGen.CodeWithConverter
import org.jetbrains.kotlinx.dataframe.codeGen.DefaultReadDfMethod
import org.jetbrains.kotlinx.dataframe.codeGen.ExtensionsCodeGenerator
import org.jetbrains.kotlinx.dataframe.codeGen.Marker
import org.jetbrains.kotlinx.dataframe.codeGen.MarkerVisibility
import org.jetbrains.kotlinx.dataframe.codeGen.MarkersExtractor
import org.jetbrains.kotlinx.dataframe.codeGen.NameNormalizer
import org.jetbrains.kotlinx.dataframe.impl.codeGen.CodeGeneratorImpl
import org.jetbrains.kotlinx.dataframe.impl.codeGen.FqNames
import org.jetbrains.kotlinx.dataframe.impl.codeGen.ShortNames
import org.jetbrains.kotlinx.dataframe.impl.codeGen.id
import org.jetbrains.kotlinx.dataframe.schema.DataFrameSchema
import kotlin.reflect.KClass

public enum class InterfaceGenerationMode {
    NoFields,
    WithFields,
    None
}

public data class CodeGenResult(val code: CodeWithConverter, val newMarkers: List<Marker>)

public interface CodeGenerator : ExtensionsCodeGenerator {

    public fun generate(
        schema: DataFrameSchema,
        name: String,
        fields: Boolean,
        extensionProperties: Boolean,
        isOpen: Boolean,
        visibility: MarkerVisibility = MarkerVisibility.IMPLICIT_PUBLIC,
        knownMarkers: Iterable<Marker> = emptyList(),
        readDfMethod: DefaultReadDfMethod? = null,
        fieldNameNormalizer: NameNormalizer = NameNormalizer.id()
    ): CodeGenResult

    public fun generate(marker: Marker, interfaceMode: InterfaceGenerationMode, extensionProperties: Boolean): CodeWithConverter

    public companion object {
        public fun create(useFqNames: Boolean = true): CodeGenerator {
            return if (useFqNames) {
                CodeGeneratorImpl(FqNames)
            } else {
                CodeGeneratorImpl(ShortNames)
            }
        }
    }
}

internal fun CodeGenerator.generate(
    markerClass: KClass<*>,
    interfaceMode: InterfaceGenerationMode,
    extensionProperties: Boolean
) = generate(
    MarkersExtractor.get(markerClass),
    interfaceMode,
    extensionProperties
)

internal inline fun <reified T> CodeGenerator.generate(
    interfaceMode: InterfaceGenerationMode,
    extensionProperties: Boolean
) = generate(T::class, interfaceMode, extensionProperties)
