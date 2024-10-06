/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.gradle.parcelize

import org.jetbrains.amper.gradle.base.BindingPluginPart
import org.jetbrains.amper.gradle.base.PluginPartCtx

class ParcelizePluginPart(ctx: PluginPartCtx) : BindingPluginPart by ctx {
    override val needToApply: Boolean by lazy {
        module.leafFragments.any { it.settings.kotlin.parcelize.enabled }
    }

    override fun applyBeforeEvaluate() {
        project.plugins.apply("org.jetbrains.kotlin.plugin.parcelize")
    }
}