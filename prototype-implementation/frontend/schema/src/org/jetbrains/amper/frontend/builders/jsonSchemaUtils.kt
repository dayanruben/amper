/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.builders

import kotlin.reflect.KClass
import kotlin.reflect.KType

// W/A around ${} escaping.
const val patternProperties = "^[^@+:]+" + "$" + "{'$'}"

fun String.addIdentButFirst(ident: String = "    ") =
    lines().joinToString(separator = "${System.lineSeparator()}$ident") { it }

fun String.trimNLEnds() = removePrefix(System.lineSeparator()).removeSuffix(System.lineSeparator())

fun buildSchemaKeyBasedCollection(
    block: () -> String,
) = """
"type": "array",
"items": {
    "patternProperties": {
        "$patternProperties": {
            ${block().addIdentButFirst("            ")}
        }
    }
}
""".trimNLEnds()

fun buildModifierBasedCollection(
    name: String,
    block: () -> String,
) = """
"^$name(@.+)?${'$'}": {
    ${block().addIdentButFirst("        ")}
}
""".trimNLEnds()

fun buildProperty(
    name: String,
    block: () -> String,
) = """
"$name": {
    ${block().addIdentButFirst("    ")}   
}
""".trimNLEnds()

fun buildSchemaCollection(
    uniqueElements: Boolean = true, // TODO handle
    minItems: Int? = null, // TODO handle
    block: () -> String,
) = """
"type": "array",
"items": {
    ${block().addIdentButFirst()}
}
""".trimNLEnds()

val stringSchema
    get() = """
"type": "string"
""".trimNLEnds()

val booleanSchema
    get() = """
"type": "boolean"
""".trimNLEnds()

val KClass<*>.jsonDef: String get() = simpleName!!
val KClass<*>.asReferenceTo get() = "\"ref\": \"#/\$defs/${this.jsonDef}\""

fun <T> Collection<T>.wrapInAnyOf(block: (T) -> String) = buildString {
    if (size == 1) {
        append(block(this@wrapInAnyOf.first()))
    } else {
        appendLine("\"anyOf\": [")
        forEachEndAware { isEnd, it ->
            appendLine("    {")
            appendLine(block(it).prependIndent("         "))
            append("    }")
            if (!isEnd) appendLine(",") else appendLine()
        }
        append("]")
    }
}

val KType.enumSchema
    get() = buildString {
        append("\"enum\": [")
        val enumValues = unwrapKClassOrNull!!.java.enumConstants
        enumValues.toList().forEachEndAware<Any> { isEnd, it ->
            append("\"$it\"")
            if (!isEnd) append(",")
        }
        append("]")
    }

fun <T> Collection<T>.forEachEndAware(block: (Boolean, T) -> Unit) =
    forEachIndexed { index, it -> if (index == size - 1) block(true, it) else block(false, it) }