package com.suishibeisong.flashcards.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Last touch position, expressed as a fraction of the app window. */
val LocalTapOrigin = staticCompositionLocalOf { TransformOrigin.Center }

/**
 * A Material-looking alert dialog that scales out of the last tapped position
 * instead of popping in from the center of the screen. All dialogs use this so
 * they share the same container-transform feel as secondary pages.
 */
@Composable
fun OriginAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
) {
    val origin = LocalTapOrigin.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    initialScale = 0.78f,
                    transformOrigin = origin,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(180)),
            ) {
                Surface(
                    modifier = modifier.fillMaxWidth().widthIn(max = 560.dp),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                ) {
                    Column(Modifier.padding(24.dp)) {
                        title?.invoke()
                        if (title != null && text != null) {
                            Spacer(Modifier.height(14.dp))
                        }
                        text?.invoke()
                        Row(
                            Modifier.fillMaxWidth().padding(top = 18.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            dismissButton?.invoke()
                            if (dismissButton != null) Spacer(Modifier.width(6.dp))
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
