package org.dokka.it.android.kmp.material3

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.dokka.it.android.kmp.core.MenuItem

@Composable
fun TopAppBarAction(
    menuItem: MenuItem,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = menuItem.onClick,
        modifier = modifier,
    ) {
        Icon(menuItem.imageVector, menuItem.label)
    }
}
