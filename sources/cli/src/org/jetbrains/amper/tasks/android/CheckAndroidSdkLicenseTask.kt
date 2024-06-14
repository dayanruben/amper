/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.tasks.android

import org.jetbrains.amper.cli.userReadableError
import org.jetbrains.amper.core.AmperUserCacheRoot
import org.jetbrains.amper.engine.Task
import org.jetbrains.amper.frontend.TaskName
import java.nio.file.Path
import kotlin.io.path.div

class CheckAndroidSdkLicenseTask(
    private val androidSdkPath: Path,
    private val userCacheRoot: AmperUserCacheRoot,
    override val taskName: TaskName
): Task {
    override suspend fun run(dependenciesResult: List<org.jetbrains.amper.tasks.TaskResult>): org.jetbrains.amper.tasks.TaskResult {
        // Check the license
        if(!SdkInstallManager(userCacheRoot, androidSdkPath).checkSdkLicenses()) {
            userReadableError("There are some licenses have not been accepted for Android SDK. Run \"${androidSdkPath / "cmdline-tools" / "latest" / "bin" / "sdkmanager"} --licenses\" to review and accept them")
        }
        return TaskResult(dependenciesResult)
    }

    class TaskResult(
        override val dependencies: List<org.jetbrains.amper.tasks.TaskResult>,
    ) : org.jetbrains.amper.tasks.TaskResult
}