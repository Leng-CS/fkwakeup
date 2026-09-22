package com.lengcs.fkwakeup.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 全局圆角节奏。
 *
 * 小控件保持利落，大卡片像柔软贴纸；周视图的窄课程块会在页面内使用更紧凑的圆角。
 */
val FkwakeupShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
