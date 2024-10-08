/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.schemaConverter.psi

import com.intellij.amper.lang.AmperContextBlock
import com.intellij.amper.lang.AmperContextName
import com.intellij.amper.lang.AmperContextualElement
import com.intellij.amper.lang.AmperContextualStatement
import com.intellij.psi.PsiElement
import org.jetbrains.amper.core.messages.BuildProblemImpl
import org.jetbrains.amper.core.messages.Level
import org.jetbrains.amper.frontend.SchemaBundle
import org.jetbrains.amper.frontend.SchemaEnum
import org.jetbrains.amper.frontend.api.ConstructorParameter
import org.jetbrains.amper.frontend.api.ImplicitConstructor
import org.jetbrains.amper.frontend.api.SchemaNode
import org.jetbrains.amper.frontend.api.Traceable
import org.jetbrains.amper.frontend.api.TraceableEnum
import org.jetbrains.amper.frontend.api.TraceablePath
import org.jetbrains.amper.frontend.api.TraceableString
import org.jetbrains.amper.frontend.api.ValueBase
import org.jetbrains.amper.frontend.api.applyPsiTrace
import org.jetbrains.amper.frontend.api.valueBase
import org.jetbrains.amper.frontend.builders.collectionType
import org.jetbrains.amper.frontend.builders.isBoolean
import org.jetbrains.amper.frontend.builders.isCollection
import org.jetbrains.amper.frontend.builders.isInt
import org.jetbrains.amper.frontend.builders.isMap
import org.jetbrains.amper.frontend.builders.isPath
import org.jetbrains.amper.frontend.builders.isScalar
import org.jetbrains.amper.frontend.builders.isString
import org.jetbrains.amper.frontend.builders.mapValueType
import org.jetbrains.amper.frontend.builders.schemaDeclaredMemberProperties
import org.jetbrains.amper.frontend.builders.unwrapKClass
import org.jetbrains.amper.frontend.messages.PsiBuildProblemSource
import org.jetbrains.amper.frontend.schema.Dependency
import org.jetbrains.amper.frontend.schema.KspProcessorDeclaration
import java.nio.file.Path
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

internal typealias ValueTable = Map<KeyWithContext, AmperElementWrapper>
internal data class KeyWithContext(val key: Pointer, val contexts: Set<TraceableString>)

internal fun PsiElement.readValueTable(): ValueTable {
    val table = mutableMapOf<KeyWithContext, AmperElementWrapper>()
    object : AmperPsiAdapterVisitor() {
        override fun visitScalar(node: Scalar) {
            val parentEntry = MappingEntry.byKey(node.sourceElement)
            val value = parentEntry?.value
            if (value == null || Scalar.from(value) == null) {
                addNode(node)
            }
            super.visitScalar(node)
        }

        override fun visitMappingEntry(node: MappingEntry) {
            addNode(node)
            super.visitMappingEntry(node)
        }

        override fun visitSequenceItem(item: PsiElement, index: Int) {
            if (!table.containsKey(KeyWithContext(position, context))) {
                addNode(UnknownElementWrapper(item))
            }
            super.visitSequenceItem(item, index)
        }

        private fun addNode(node: AmperElementWrapper) {
            table[KeyWithContext(position, context)] = node
        }
    }.visitElement(this)
    return table
}

context(Converter)
internal fun readTypedValue(
    type: KType,
    table: ValueTable,
    path: Pointer,
    contexts: Set<TraceableString>,
    valueBase: ValueBase<Any?>? = null
): Any? {
    if (type.withNullability(false) != type) {
        return readTypedValue(type.withNullability(false), table, path, contexts, valueBase)
    }
    if (type.isSubtypeOf(TraceableEnum::class.starProjectedType)
        || type.isSubtypeOf(TraceableString::class.starProjectedType)
        || type.isSubtypeOf(TraceablePath::class.starProjectedType)) {
        val scalarValue = table[KeyWithContext(path, contexts)]
        return instantiateTraceableScalar(type, table, path, contexts, valueBase, scalarValue)
    }
    val applicableKeys = table.keys.filter { it.key.startsWith(path) && it.contexts.containsAll(contexts) }
    if (applicableKeys.isEmpty()) return null

    val scalarValue = table[KeyWithContext(path, contexts)] as? Scalar

    if (type.isScalar) {
        val text = scalarValue?.textValue ?: return null
        return readScalarType(type, scalarValue, text, valueBase)
    }
    if (type.isMap) {
        if (type.arguments.getOrNull(0)?.type?.isCollection == true) {
            return applicableKeys.map { it.contexts }
                .distinct()
                .associate { ks -> ks.toSet() to readTypedValue(
                    type.mapValueType,
                    table,
                    path,
                    ks
                ) }.filterValues { it != null }
        }
        val processedKeys = mutableSetOf<String>()
        return applicableKeys.mapNotNull {
            val key = it.key.nextAfter(path)?.let {
                // hack for numbered items in a collection
                if (it.segmentName?.toIntOrNull() != null) it.next else it
            }
            val name = key?.segmentName
            if (name == null || it.key.prev == null || !processedKeys.add(name)) null
            else (name to readTypedValue(type.mapValueType, table, key, contexts))
        }.toMap()
    }
    if (type.isCollection) {
        val visitedKeys = hashSetOf<Pointer>()
        return applicableKeys.mapNotNull { keyWithContext ->
            val newKey = keyWithContext.key.nextAfter(path)
            if (newKey != null && !visitedKeys.any { newKey.startsWith(it) }) {
                visitedKeys.add(newKey)
                readTypedValue(type.collectionType, table, newKey, contexts)
            } else null
        }.let { if (type.isSubtypeOf(Set::class.starProjectedType)) it.toSet() else it }
    }

    // ksp processor initialization depends on string prefixes and thus no autowiring
    if (type.unwrapKClass == KspProcessorDeclaration::class) {
        return instantiateKspProcessor(scalarValue)
    }

    // dependencies are too complicated to wire them automatically (different shorthands and a full form)
    if (type.unwrapKClass == Dependency::class) {
        return instantiateDependency(scalarValue, applicableKeys, path, table, contexts)
    }

    if (type.isSubtypeOf(SchemaNode::class.starProjectedType)) {
        val textValue = scalarValue?.textValue
        // handle shorthands - when a schema node is wired for a scalar
        if (textValue != null) {
            // find an implicit or explicit constructor and invoke it
            val constructedType = type.unwrapKClass.findAnnotation<ImplicitConstructor>()?.constructedType
                ?: type.unwrapKClass

            val props = constructedType.schemaDeclaredMemberProperties()
                .filterIsInstance<KMutableProperty1<Any, Any?>>()
            val param = props.singleOrNull {
                    it.hasAnnotation<ConstructorParameter>()
                } ?: props.singleOrNull()
                  // "enabled" shortcut
                  ?: props.singleOrNull { it.name == "enabled" }?.takeIf { textValue == "enabled" }

            if (param != null) {
                return type.instantiateType().also {
                    val value = if (param.name == "enabled" && textValue == "enabled") "true" else textValue
                    val transformedValue = readScalarType(
                        param.returnType, scalarValue,
                        value, param.valueBase(it)
                    )
                    setPropertyValueSafe(param, it, transformedValue)
                    if (it is Traceable) {
                        it.doApplyPsiTrace(scalarValue.sourceElement)
                    }
                }
            }
        }
    }

    return type.instantiateType().also { instance ->
        if (instance is Traceable) {
            table[KeyWithContext(path, contexts)]?.sourceElement?.let {
                instance.doApplyPsiTrace(it)
            }
        }
        readFromTable(instance, table, path, contexts)
    }
}

private fun setPropertyValueSafe(
    prop: KMutableProperty1<Any, Any?>,
    target: Any,
    value: Any?
) {
    // we return here, as such situations must have been processed before we come to this point
    if (value == null && !prop.returnType.isMarkedNullable) return
    prop.set(target, value)
}

context(Converter)
private fun instantiateTraceableScalar(
    type: KType,
    table: ValueTable,
    path: Pointer,
    contexts: Set<TraceableString>,
    valueBase: ValueBase<Any?>?,
    scalarValue: AmperElementWrapper?
): Traceable? {
    return when (type.unwrapKClass) {
        TraceableEnum::class -> type.arguments[0].type?.let {
            readTypedValue(
                it,
                table,
                path,
                contexts,
                valueBase
            )
        }?.let {
            TraceableEnum::class.primaryConstructor!!.call(it).doApplyPsiTrace(scalarValue?.sourceElement)
        }

        TraceableString::class -> readTypedValue(
            String::class.starProjectedType,
            table,
            path,
            contexts,
            valueBase
        )?.let {
            TraceableString(it as String).doApplyPsiTrace(scalarValue?.sourceElement)
        }

        TraceablePath::class -> readTypedValue(
            Path::class.starProjectedType,
            table,
            path,
            contexts,
            valueBase
        )?.let {
            TraceablePath(it as Path).doApplyPsiTrace(scalarValue?.sourceElement)
        }

        else -> null
    }
}

context(Converter)
private fun readScalarType(
    type: KType,
    scalarValue: Scalar?,
    text: String,
    valueBase: ValueBase<Any?>?
): Any? {
    scalarValue?.sourceElement?.let {
        valueBase?.doApplyPsiTrace(it)
    }

    when {
        type.isSubtypeOf(SchemaEnum::class.starProjectedType) -> {
            return readEnum(type, scalarValue)
        }
        type.isString -> return text
        type.isBoolean -> return readBoolean(text, scalarValue)
        type.isInt -> return readInteger(text, scalarValue)
        type.isPath -> return text.asAbsolutePath()
    }

    return null
}

context(Converter)
private fun readInteger(text: String, scalarValue: Scalar?): Int? {
    val value = text.toIntOrNull()
    if (value == null) {
        reportError("validation.expected.integer", scalarValue)
    }
    return value
}

context(Converter)
private fun readBoolean(text: String, scalarValue: Scalar?) = when (text) {
    "true" -> true
    "false" -> false
    else -> {
        if (scalarValue != null) {
            reportError("validation.expected.boolean", scalarValue)
        }
        null
    }
}

context(Converter)
private fun readEnum(
    type: KType,
    scalarValue: Scalar?,
    reportMismatch: Boolean = true
): SchemaEnum? {
    @Suppress("UNCHECKED_CAST")
    val allValues = type.unwrapKClass.declaredFunctions.firstOrNull {
        it.name == "values"
    }?.call() as? Array<SchemaEnum>

    val matchingEnumValue = allValues?.firstOrNull { it.schemaValue == scalarValue?.textValue }
    if (reportMismatch && matchingEnumValue == null) {
        reportError("validation.unknown.enum.value", scalarValue,
            if (allValues.orEmpty().size > 10) "validation.unknown.enum.value.short"
            else "validation.unknown.enum.value",
            type.unwrapKClass.simpleName?.splitByCamelHumps(),
            scalarValue?.textValue,
            allValues?.joinToString { it.schemaValue }
        )
    }
    return matchingEnumValue
}

context(Converter)
private fun reportError(problemId: String,
                        source: AmperElementWrapper?,
                        messageKey: String = problemId,
                        vararg args: String?) {
    source ?: return
    problemReporter.reportMessage(
        BuildProblemImpl("validation.unknown.enum.value",
            PsiBuildProblemSource(source.sourceElement),
            SchemaBundle.message(messageKey, *args),
            Level.Error
        )
    )
}

context(Converter)
internal fun <T : Any> readFromTable(
    obj: T,
    table: ValueTable,
    path: Pointer = Pointer(),
    contexts: Set<TraceableString> = emptySet()
) {
    obj::class.schemaDeclaredMemberProperties()
        .filterIsInstance<KMutableProperty1<Any, Any?>>()
        .forEach { prop ->
            readTypedValue(prop.returnType, table, path + prop.name, contexts, prop.valueBase(obj))?.let {
                setPropertyValueSafe(prop, obj, it)
                table[KeyWithContext(path + prop.name, contexts)]?.let {
                    prop.valueBase(obj)?.doApplyPsiTrace(it.sourceElement)
                }
            }
    }
}

private fun KType.instantiateType(): Any {
    val kClass = unwrapKClass
    if (kClass.isSubclassOf(Set::class)) {
        return HashSet<Any>()
    }
    if (kClass.isSubclassOf(List::class)) {
        return ArrayList<Any>()
    }
    return kClass.findAnnotation<ImplicitConstructor>()?.constructedType?.createInstance()
        ?: kClass.createInstance()
}

internal fun <T : Traceable> T.doApplyPsiTrace(element: PsiElement?): T {
    val adjustedElement =
        MappingEntry.from(element)?.sourceElement ?:
        element?.let { MappingEntry.byValue(it) }?.sourceElement ?:
        element
    return applyPsiTrace(adjustedElement)
}

internal val AmperContextualElement.contexts get() =
    when (this) {
        is AmperContextBlock -> contextNameList.contextNames
        is AmperContextualStatement -> contextNameList.contextNames
        else -> emptyList()
    }

internal val List<AmperContextName>.contextNames get()
    = mapNotNull { c -> c.identifier?.let { TraceableString(it.text).applyPsiTrace(c) } }.toSet()