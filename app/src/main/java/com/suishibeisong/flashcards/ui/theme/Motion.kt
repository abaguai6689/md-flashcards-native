package com.suishibeisong.flashcards.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object GhostMotion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val springMedium = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    val springFast = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
}
