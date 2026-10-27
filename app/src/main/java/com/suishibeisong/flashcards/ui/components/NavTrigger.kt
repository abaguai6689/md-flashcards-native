package com.suishibeisong.flashcards.ui.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xah.container.component.base.SharedContainer

/**
 * Wraps a navigation trigger with SharedNav's shared container. The target
 * Destination must use the same key, so the page grows out of this exact box.
 * Keep the inner visual's shape rectangular; the container provides the radius.
 */
@Composable
fun NavTrigger(
    key: String,
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SharedContainer(key = key, shape = shape, modifier = modifier) {
        content()
    }
}
