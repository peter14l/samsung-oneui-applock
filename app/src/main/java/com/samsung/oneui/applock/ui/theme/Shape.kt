package com.samsung.oneui.applock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Samsung One UI 6 / 7 Shape Guidelines: 26.dp cards and containers, 50% pill buttons
val OneUICardRadius = 26.dp
val OneUIIconRadius = 13.dp
val OneUIPillRadius = 50.dp

val OneUIShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(OneUICardRadius),
    extraLarge = RoundedCornerShape(OneUIPillRadius)
)
