/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.schemaConverter.psi

import com.intellij.psi.PsiElement
import org.jetbrains.amper.frontend.api.TraceableString
import org.jetbrains.amper.frontend.api.applyPsiTrace
import org.jetbrains.amper.frontend.api.asTraceable
import org.jetbrains.amper.frontend.api.valueBase
import org.jetbrains.amper.frontend.schema.CatalogDependency
import org.jetbrains.amper.frontend.schema.CatalogKey
import org.jetbrains.amper.frontend.schema.CatalogKspProcessorDeclaration
import org.jetbrains.amper.frontend.schema.Dependency
import org.jetbrains.amper.frontend.schema.DependencyScope
import org.jetbrains.amper.frontend.schema.ExternalMavenDependency
import org.jetbrains.amper.frontend.schema.InternalDependency
import org.jetbrains.amper.frontend.schema.MavenKspProcessorDeclaration
import org.jetbrains.amper.frontend.schema.ModuleKspProcessorDeclaration
import org.jetbrains.yaml.YAMLLanguage

context(Converter)
internal fun instantiateKspProcessor(
    scalarValue: Scalar?
): Any? {
    val text = scalarValue?.textValue ?: return null
    return when {
        text.startsWith("$") -> CatalogKspProcessorDeclaration(CatalogKey(text.substring(1)).applyPsiTrace(scalarValue.sourceElement))
        text.startsWith(".") -> ModuleKspProcessorDeclaration(text.asAbsolutePath().asTraceable().applyPsiTrace(scalarValue.sourceElement))
        else -> MavenKspProcessorDeclaration(TraceableString(text).applyPsiTrace(scalarValue.sourceElement))
    }
}

context(Converter)
internal fun instantiateDependency(
    scalarValue: Scalar?,
    applicableKeys: List<KeyWithContext>,
    path: Pointer,
    table: Map<KeyWithContext, AmperElementWrapper>,
    contexts: Set<TraceableString>
): Any? {
    val textValue = scalarValue?.textValue
    if ((scalarValue?.sourceElement?.language is YAMLLanguage
                || textValue == path.segmentName) && textValue != null) {
        val sourceElement = table[KeyWithContext(path, contexts)]?.sourceElement
        return instantiateDependency(textValue, scalarValue.sourceElement).also { dep ->
            sourceElement?.let { e ->
                applyDependencyTrace(dep, e)
            }
            readFromTable(dep, table, path, contexts)
        }
    } else {
        val matchingKeys = applicableKeys.filter { it.key.startsWith(path) }.let {
            if (it.size > 1) it.filter { it.key != path } else it
        }
        if (matchingKeys.size == 1) {
            val key = matchingKeys.single()
            val sourceElement = table[key]?.sourceElement
            val specialValue = (table[key] as? Scalar)?.textValue
            val segmentName = key.key.segmentName
            if (specialValue != null && segmentName != null) {
                instantiateDependency(segmentName, sourceElement).also { dep ->
                    sourceElement?.let {
                        applyDependencyTrace(dep, it)
                    }
                    when (specialValue) {
                        "exported" -> {
                            dep.exported = true
                            dep::exported.valueBase?.doApplyPsiTrace(sourceElement)
                            return dep
                        }

                        "compile-only" -> {
                            dep.scope = DependencyScope.COMPILE_ONLY
                            dep::scope.valueBase?.doApplyPsiTrace(sourceElement)
                            return dep
                        }

                        "runtime-only" -> {
                            dep.scope = DependencyScope.RUNTIME_ONLY
                            dep::scope.valueBase?.doApplyPsiTrace(sourceElement)
                            return dep
                        }

                        "all" -> {
                            dep.scope = DependencyScope.ALL
                            dep::scope.valueBase?.doApplyPsiTrace(sourceElement)
                            return dep
                        }
                    }
                }
            }
        }
        else {
            if (path.segmentName?.toIntOrNull() != null) {
                val next = matchingKeys.map {
                    it.key.nextAfter(path)
                }.distinct()
                if (next.size == 1) {
                    val single = next.single()!!
                    val sourceElement = table[KeyWithContext(single, contexts)]?.sourceElement
                    return instantiateDependency(single.segmentName!!, sourceElement).also { dep ->
                        sourceElement?.let {
                            applyDependencyTrace(dep, it)
                        }
                        readFromTable(dep, table, single, contexts)
                    }
                }
            }
            else {
                val sourceElement = table[KeyWithContext(path, contexts)]?.sourceElement
                return instantiateDependency(path.segmentName!!, sourceElement).also { dep ->
                    sourceElement?.let {
                        applyDependencyTrace(dep, it)
                    }
                    readFromTable(dep, table, path, contexts)
                }
            }
        }
    }
    return null
}

context(Converter)
internal fun instantiateDependency(text: String, sourceElement: PsiElement?): Dependency {
    return when {
        text.startsWith(".") -> InternalDependency().also { it.path = text.asAbsolutePath() }
        text.startsWith("$") -> CatalogDependency().also { it.catalogKey = CatalogKey(text.substring(1)).applyPsiTrace(sourceElement) }
        else -> ExternalMavenDependency().also { it.coordinates = text }
    }
}

internal fun applyDependencyTrace(dep: Dependency, e: PsiElement) {
    dep.doApplyPsiTrace(e)
    (dep as? ExternalMavenDependency)?.let {
        it::coordinates.valueBase?.doApplyPsiTrace(e)
    }
    (dep as? CatalogDependency)?.let {
        it::catalogKey.valueBase?.doApplyPsiTrace(e)
    }
    (dep as? InternalDependency)?.let {
        it::path.valueBase?.doApplyPsiTrace(e)
    }
}