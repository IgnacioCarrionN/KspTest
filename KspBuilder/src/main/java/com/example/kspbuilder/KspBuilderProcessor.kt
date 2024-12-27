package com.example.kspbuilder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

class KspBuilderProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(GenerateBuilder::class.qualifiedName.toString())
                .filterIsInstance<KSClassDeclaration>()

        symbols.forEach { symbol ->
            val className = symbol.simpleName.asString()
            val packageName = symbol.packageName.asString()
            val generatedClassName = "${className}Builder"

            val file = codeGenerator.createNewFile(
                dependencies = Dependencies(false, symbol.containingFile!!),
                packageName = packageName,
                fileName = generatedClassName
            )

            val properties = symbol.getAllProperties()

            val builderProperties = mutableListOf<String>()
            val setters = mutableListOf<String>()
            val buildMethodParams = mutableListOf<String>()

            properties.forEach { property ->
                val propName = property.simpleName.asString()
                val propType = property.type.resolve().declaration.simpleName.asString()
                        .let { if (property.type.resolve().isMarkedNullable) "$it?" else it }
                val defaultValue = getDefaultValueFromProperty(property)
                builderProperties.add("   private var $propName: $propType = $defaultValue")
                setters.add("   fun set${propName.replaceFirstChar { it.uppercase() }}($propName: $propType) = apply { this.$propName = $propName }")
                buildMethodParams.add("           $propName = this.$propName")
            }

            val builderClass = buildString {
                appendLine("package $packageName")
                appendLine()
                appendLine("class $generatedClassName {")
                builderProperties.forEach { property ->
                    appendLine(property)
                }
                appendLine()
                setters.forEach { setter ->
                    appendLine(setter)
                }
                appendLine()
                appendLine("    fun build(): $className {")
                appendLine("        return $className(")
                buildMethodParams.forEach { methodParam ->
                    appendLine("$methodParam,")
                }
                appendLine("        )")
                appendLine("    }")
                appendLine("}")
                appendLine()
                appendLine("fun ${generatedClassName.replaceFirstChar { it.lowercase() }}(block: $generatedClassName.() -> Unit): $className {")
                appendLine("    return $generatedClassName().apply(block).build()")
                appendLine("}")
            }
            resolver.getNewFiles()
            file.write(builderClass.toByteArray())
            file.close()

        }
        return symbols.filterNot { it.validate() }.toList()
    }

    fun getDefaultValueFromProperty(property: KSPropertyDeclaration): String {
        val propType = property.type.resolve().declaration.qualifiedName?.asString() ?: "Any"
        val isNullable = property.type.resolve().isMarkedNullable

        return if (isNullable) "null" else when (propType) {
            "kotlin.String" -> "\"\""
            "kotlin.Int", "kotlin.Long", "kotlin.Short", "kotlin.Byte" -> "0"
            "kotlin.Double", "kotlin.Float" -> "0.0"
            "kotlin.Boolean" -> "false"
            else -> throw IllegalArgumentException("Non-nullable type $propType requires a default value")
        }
    }
}
