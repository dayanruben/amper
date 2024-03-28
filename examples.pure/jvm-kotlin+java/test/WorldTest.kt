/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldTest {
    @Test
    fun doTest() {
        assertEquals("World", World.get())
    }

    @Test
    fun shouldFail() {
        assertTrue(false)
    }
}