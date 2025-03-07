package org.jetbrains.kotlinx.dataframe.impl.codeGen

import org.jetbrains.dataframe.impl.codeGen.CodeGenerator
import org.jetbrains.dataframe.impl.codeGen.InterfaceGenerationMode
import org.jetbrains.dataframe.impl.codeGen.ReplCodeGenerator
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.AnyRow
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.schema
import org.jetbrains.kotlinx.dataframe.codeGen.CodeWithConverter
import org.jetbrains.kotlinx.dataframe.codeGen.Marker
import org.jetbrains.kotlinx.dataframe.codeGen.MarkerVisibility
import org.jetbrains.kotlinx.dataframe.codeGen.MarkersExtractor
import org.jetbrains.kotlinx.dataframe.schema.DataFrameSchema
import org.jetbrains.kotlinx.jupyter.api.Code
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.jvmErasure

internal class ReplCodeGeneratorImpl : ReplCodeGenerator {

    companion object {
        internal val markerInterfacePrefix = "_DataFrameType"
    }

    private val registeredProperties = mutableSetOf<KProperty<*>>()

    private val registeredMarkers = mutableMapOf<KClass<*>, Marker>()

    private val generatedMarkers = mutableMapOf<String, Marker>()

    private val generator: CodeGenerator = CodeGeneratorImpl()

    private fun getMarkerClass(type: KType): KClass<*>? =
        when (type.classifier) {
            DataFrame::class -> type.arguments[0].type?.jvmErasure
            DataRow::class -> type.arguments[0].type?.jvmErasure
            else -> null
        }

    override fun process(row: AnyRow, property: KProperty<*>?) = process(row.df(), property)

    override fun process(df: AnyFrame, property: KProperty<*>?): CodeWithConverter {
        var targetSchema = df.schema()
        var isMutable = false

        if (property != null) {
            val wasProcessedBefore = property in registeredProperties
            registeredProperties.add(property)
            isMutable = property is KMutableProperty

            // maybe property is already properly typed, let's do some checks
            val currentMarker = getMarkerClass(property.returnType)?.let { registeredMarkers[it] ?: MarkersExtractor[it] }
            if (currentMarker != null) {
                // if property is mutable, we need to make sure that its marker type is open in order to let derived data frames be assignable to it
                if (!isMutable || currentMarker.isOpen) {
                    val columnSchema = currentMarker.schema
                    // for mutable properties we do strong typing only at the first processing, after that we allow its type to be more general than actual data frame type
                    if (wasProcessedBefore || columnSchema == targetSchema) {
                        // property scheme is valid for current data frame, but we should also check that all compatible open markers are implemented by it
                        val requiredBaseMarkers =
                            getRequiredMarkers(columnSchema, registeredMarkers.values)
                        if (requiredBaseMarkers.all { currentMarker.implements(it) }) {
                            return CodeWithConverter("") { it }
                        }
                        // use current marker scheme as a target for generation of new marker interface, so that available properties won't change
                        targetSchema = columnSchema
                    }
                }
            }
        }

        return generate(targetSchema, markerInterfacePrefix, isMutable)
    }

    fun generate(
        schema: DataFrameSchema,
        name: String,
        isOpen: Boolean
    ): CodeWithConverter {
        val result = generator.generate(
            schema,
            name,
            fields = false,
            extensionProperties = true,
            isOpen,
            MarkerVisibility.IMPLICIT_PUBLIC,
            registeredMarkers.values
        )

        result.newMarkers.forEach {
            generatedMarkers[it.name] = it
        }
        return result.code
    }

    override fun process(markerClass: KClass<*>): Code {
        val newMarkers = mutableListOf<Marker>()

        fun resolve(clazz: KClass<*>): Marker {
            val processed = registeredMarkers[clazz]
            if (processed != null) return processed
            val temp = generatedMarkers[clazz.simpleName!!]
            if (temp != null) {
                val baseClasses = clazz.superclasses.filter { it != Any::class }

                val baseClassNames = baseClasses.map {
                    it.simpleName!!
                }.sorted()

                val tempBaseClassNames = temp.baseMarkers.map { it.value.shortName }.sorted()

                if (baseClassNames == tempBaseClassNames) {
                    val newBaseMarkers = baseClasses.map { resolve(it) }
                    val newMarker = Marker(
                        clazz.qualifiedName!!,
                        temp.isOpen,
                        temp.fields,
                        newBaseMarkers,
                        MarkerVisibility.IMPLICIT_PUBLIC,
                        clazz
                    )
                    registeredMarkers[markerClass] = newMarker
                    generatedMarkers.remove(temp.name)
                    return newMarker
                }
            }
            val marker = MarkersExtractor[markerClass]
            registeredMarkers[markerClass] = marker
            newMarkers.add(marker)
            return marker
        }

        val marker = resolve(markerClass)
        return newMarkers.map { generator.generate(marker, InterfaceGenerationMode.None, true).declarations }.join()
    }
}
