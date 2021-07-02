package org.jetbrains.dataframe.gradle

import java.io.File

open class SchemaGeneratorExtension {
    val schemas: MutableList<Schema> = mutableListOf()

    fun schema(config: Schema.() -> Unit) {
        val schema = Schema().apply(config)
        schemas.add(schema)
    }
}

class Schema(
    var data: Any? = null,
    var src: File? = null,
    var interfaceName: String = "",
    var packageName: String = ""
)
