/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.dr.resolver

import com.intellij.psi.PsiElement
import kotlinx.coroutines.CancellationException
import org.jetbrains.amper.core.UsedInIdePlugin
import org.jetbrains.amper.core.messages.BuildProblemId
import org.jetbrains.amper.core.messages.Level
import org.jetbrains.amper.core.messages.ProblemReporter
import org.jetbrains.amper.dependency.resolution.DependencyNode
import org.jetbrains.amper.dependency.resolution.MavenDependencyNode
import org.jetbrains.amper.frontend.MavenDependency
import org.jetbrains.amper.frontend.api.PsiTrace
import org.jetbrains.amper.frontend.messages.PsiBuildProblem
import org.slf4j.LoggerFactory
import kotlin.io.path.Path

internal val logger = LoggerFactory.getLogger("files.kt")

@Suppress("UNUSED") // Used in Idea plugin
val DependencyNode.fragmentDependencies: List<DirectFragmentDependencyNodeHolder>
    get() = findParents<DirectFragmentDependencyNodeHolder>()

private inline fun <reified T: DependencyNode> DependencyNode.findParents(): List<T> {
    val result = mutableSetOf<T>()
    findParentsImpl(T::class.java, result = result)
    return result.toList()
}

private fun <T: DependencyNode> DependencyNode.findParentsImpl(
    kClass: Class<T>,
    visited: MutableSet<DependencyNode> = mutableSetOf(),
    result: MutableSet<T> = mutableSetOf()
) {
    if (!visited.add(this)) {
        return
    }

    if (kClass.isInstance(this)) {
        @Suppress("UNCHECKED_CAST")
        result.add(this as T)
    } else {
        parents.forEach { it.findParentsImpl(kClass, visited = visited, result = result) }
    }
}

internal fun parseCoordinates(coordinates: String): MavenCoordinates? {
    val parts = coordinates.split(":")
    if (parts.size < 3) {
        return null
    }
    if (parts.any { resolveSafeOrNull{ Path(it) } == null } ) {
        // Check if resolved parts don't contain illegal characters
        return null
    }
    return MavenCoordinates(parts[0], parts[1], parts[2], classifier = if (parts.size > 3) parts[3] else null)
}

internal fun MavenDependency.parseCoordinates(): MavenCoordinates? {
    return parseCoordinates(this.coordinates)
}

fun MavenDependencyNode.mavenCoordinates(suffix: String? = null): MavenCoordinates {
    return MavenCoordinates(
        groupId = this.dependency.group,
        artifactId = if (suffix == null) dependency.module else "${dependency.module}:${suffix}",
        version = this.dependency.version,
    )
}

/**
 * Describes coordinates of a Maven artifact.
 */
data class MavenCoordinates(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val classifier: String? = null
) {
    override fun toString(): String {
        return "$groupId:$artifactId:$version${if (classifier != null) ":$classifier" else ""}"
    }
}

private fun <T> resolveSafeOrNull(block: () -> T?): T? {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }
}

@UsedInIdePlugin
fun DependencyNode.reportOverriddenDirectModuleDependencies(reporter: ProblemReporter) {
    if (this is DirectFragmentDependencyNodeHolder
        && this.dependencyNode is MavenDependencyNode
        && this.dependencyNode.version != this.dependencyNode.dependency.version)
    {
        // for every direct module dependency referencing this dependency node
        val psiElement = (notation?.trace as? PsiTrace)?.psiElement
        if (psiElement != null) {
            reporter.reportMessage(
                ModuleDependencyWithOverriddenVersion(
                    this.dependencyNode.version,
                    this.dependencyNode.dependency.version,
                    this.dependencyNode.mavenCoordinates().toString(),
                    psiElement
                ))
        }
    }
}

class ModuleDependencyWithOverriddenVersion(
    @UsedInIdePlugin
    val originalVersion: String,
    @UsedInIdePlugin
    val effectiveVersion: String,
    @UsedInIdePlugin
    val effectiveCoordinates: String,
    @UsedInIdePlugin
    override val element: PsiElement,
) : PsiBuildProblem(Level.Warning) {
    override val buildProblemId: BuildProblemId = ID
    override val message: String
        get() = "Declared dependency version is overridden, the actual version is $effectiveVersion"

    companion object {
        const val ID = "dependency.version.is.overridden"
    }
}