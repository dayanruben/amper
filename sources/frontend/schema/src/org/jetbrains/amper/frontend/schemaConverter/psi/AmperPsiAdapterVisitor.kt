/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.schemaConverter.psi

import com.intellij.amper.lang.AmperContextBlock
import com.intellij.amper.lang.AmperContextualElement
import com.intellij.amper.lang.AmperContextualStatement
import com.intellij.amper.lang.AmperElementVisitor
import com.intellij.amper.lang.AmperLanguage
import com.intellij.amper.lang.AmperLiteral
import com.intellij.amper.lang.AmperObject
import com.intellij.amper.lang.AmperProperty
import com.intellij.amper.lang.impl.propertyList
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import com.intellij.util.containers.Stack
import org.jetbrains.amper.frontend.api.TraceableString
import org.jetbrains.amper.frontend.api.applyPsiTrace
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

open class AmperPsiAdapterVisitor {
    private val positionStack: Stack<String> = Stack()
    private val contextStack: Stack<Set<TraceableString>> = Stack()

    val position get() = positionStack.toList().let {
        var path = Pointer()
        for (item in it) {
            path += item
        }
        return@let path
    }
    val context get() = contextStack.toList().flatten().toSet()

    fun visitElement(element: PsiElement) {
        if (element.language is AmperLanguage) {
            object: AmperElementVisitor() {
                override fun visitContextBlock(o: AmperContextBlock) {
                    contextStack.push(o.contextNameList.contextNames)
                    super.visitContextBlock(o)
                    contextStack.pop()
                }

                override fun visitContextualStatement(o: AmperContextualStatement) {
                    contextStack.push(o.contextNameList.contextNames)
                    super.visitContextualStatement(o)
                    contextStack.pop()
                }

                override fun visitElement(o: PsiElement) {
                    ProgressIndicatorProvider.checkCanceled()
                    o.acceptChildren(this)
                }

                override fun visitProperty(o: AmperProperty) {
                    val parentObject = o.parent as? AmperObject
                    val props = parentObject?.propertyList.orEmpty()
                    if (props.isNotEmpty() && hasDuplicateProperties(props, o)) {
                        positionStack.push(props.indexOf(o).toString())
                    } else {
                        positionStack.push(
                            when (o.name) {
                                "testSettings" -> "test-settings"
                                "testDependencies" -> "test-dependencies"
                                null -> "[unnamed]"
                                else -> o.name
                            }
                        )
                    }
                    visitMappingEntry(MappingEntry(o))
                    super.visitProperty(o)
                    positionStack.pop()
                }

                private fun hasDuplicateProperties(
                    props: List<AmperProperty>,
                    o: AmperProperty
                ): Boolean {
                    val namesakeProps = props.filter { it.name == o.name }
                    val hadDuplicateProperties = namesakeProps.size > 1 && namesakeProps.map {
                        it.parentsOfType<AmperContextualElement>().map {
                            it.contexts
                        }.flatten().toSet() to it
                    }.groupBy({ it.first }) { it.second }.entries.any { it.value.size > 1 }
                    return hadDuplicateProperties
                }

                override fun visitLiteral(o: AmperLiteral) {
                    Scalar.from(o)?.let { visitScalar(it) }
                    super.visitLiteral(o)
                }
            }.visitElement(element)
        }
        else {
            object : YamlPsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    ProgressIndicatorProvider.checkCanceled()
                    element.acceptChildren(this)
                }

                override fun visitKeyValue(keyValue: YAMLKeyValue) {
                    val atSign = keyValue.keyText.indexOf('@')
                    if (atSign < 0) {
                        positionStack.push(keyValue.keyText)
                    }
                    else {
                        positionStack.push(keyValue.keyText.substring(0, atSign))
                        contextStack.push(keyValue.keyText.substring(atSign + 1).split('+')
                            .map { TraceableString(it).applyPsiTrace(keyValue.key) }.toSet())
                    }
                    visitMappingEntry(MappingEntry(keyValue))
                    super.visitKeyValue(keyValue)
                    positionStack.pop()
                    if (atSign >= 0) { contextStack.pop() }
                }

                override fun visitSequence(sequence: YAMLSequence) {
                    sequence.items.forEachIndexed { index, item ->
                        positionStack.push(index.toString())
                        item.value?.accept(this)
                        item.value?.let { visitSequenceItem(it, index) }
                        positionStack.pop()
                    }
                    // do not call super here!
                }

                override fun visitScalar(scalar: YAMLScalar) {
                    Scalar.from(scalar)?.let { visitScalar(it) }
                    super.visitScalar(scalar)
                }
            }.visitElement(element)
        }
    }

    open fun visitSequenceItem(item: PsiElement, index: Int) {}
    open fun visitMappingEntry(node: MappingEntry) {}
    open fun visitScalar(node: Scalar) {}
}