package com.lengcs.fkwakeup.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Test

class ThemeContrastTest {

    @Test
    fun lightTheme_coreTextPairsMeetAaContrast() {
        assertAa(OnPrimaryLight, PrimaryLight)
        assertAa(OnPrimaryContainerLight, PrimaryContainerLight)
        assertAa(OnSecondaryLight, SecondaryLight)
        assertAa(OnTertiaryLight, TertiaryLight)
        assertAa(OnBackgroundLight, BackgroundLight)
        assertAa(OnSurfaceVariantLight, SurfaceVariantLight)
    }

    @Test
    fun darkTheme_coreTextPairsMeetAaContrast() {
        assertAa(OnPrimaryDark, PrimaryDark)
        assertAa(OnPrimaryContainerDark, PrimaryContainerDark)
        assertAa(OnSecondaryDark, SecondaryDark)
        assertAa(OnTertiaryDark, TertiaryDark)
        assertAa(OnBackgroundDark, BackgroundDark)
        assertAa(OnSurfaceVariantDark, SurfaceVariantDark)
    }

    private fun assertAa(foreground: Color, background: Color) {
        assertThat(contrastRatio(foreground, background)).isAtLeast(4.5)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linear(value: Float): Double {
            val channel = value.toDouble()
            return if (channel <= 0.04045) channel / 12.92
            else ((channel + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}
