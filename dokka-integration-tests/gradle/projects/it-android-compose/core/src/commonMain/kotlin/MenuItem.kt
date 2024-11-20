package org.dokka.it.android.kmp.core

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
public data class MenuItem(
    val label: String,
    val imageVector: ImageVector,
    val onClick: () -> Unit,
    val isImportant: Boolean,
)
