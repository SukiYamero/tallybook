@file:Suppress("MagicNumber") // This file is the sole definition point for design-color tokens.

package com.kurobello.tallybook.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightCanvas = Color(0xFFF4F3EF)
private val LightSurfaceDeep = Color(0xFFEFEEE9)
private val LightSurfaceSunken = Color(0xFFFFFFFF)
private val LightSurfaceCard = Color(0xFFFFFFFF)
private val LightSurfaceRaised = Color(0xFFF0EFE9)
private val LightSurfaceHigh = Color(0xFFEAE9E2)
private val LightSurfaceHighest = Color(0xFFE4E3DA)
private val LightForeground = Color(0xFF1B1C19)
private val LightMutedForeground = Color(0xFF71736B)
private val BrandPrimary = Color(0xFFA8C9B0)
private val BrandPrimaryForeground = Color(0xFF06231A)
private val BrandSecondary = Color(0xFF3F6B56)
private val BrandSecondaryForeground = Color(0xFFFFFFFF)
private val LightPrimary = BrandPrimary
private val LightPrimaryForeground = BrandPrimaryForeground
private val LightDestructive = Color(0xFFCF4B4B)
private val LightFgSecondary = Color(0xFF3C3E39)
private val LightFgTertiary = Color(0xFF585A52)
private val LightFgFaint = Color(0xFF8A8C83)
private val LightFgDisabled = Color(0xFF9EA096)
private val LightSuccess = Color(0xFF0E9765)
private val LightSuccessStrong = Color(0xFF12A873)
private val LightSuccessForeground = Color(0xFFFFFFFF)
private val LightDanger = Color(0xFFCF4B4B)
private val LightDangerStrong = Color(0xFFCF4B4B)
private val LightDangerForeground = Color(0xFFFFFFFF)
private val LightInfo = Color(0xFF3D74C4)
private val LightWarning = Color(0xFFAF7809)
private val LightBorder = Color(0x121C1C14)
private val LightBorderSubtle = Color(0x0B1C1C14)
private val LightBorderStrong = Color(0x1C1C1C14)
private val LightBorderHover = Color(0x291C1C14)
private val LightInput = Color(0x171C1C14)
private val LightRing = Color(0x80A8C9B0)
private val LightInputCursor = BrandSecondary
private val LightPressedOverlay = Color(0x0C1C1C14)
private val LightGreenPanel = Color(0xFFDDEFE4)
private val LightGreenChip = Color(0xFFC8E6D6)
private val LightGreenChipForeground = Color(0xFF0E4B34)
private val LightGreenText = Color(0xFF2E6B52)
private val LightRedChip = Color(0xFFF7DEDE)
private val LightRedText = Color(0xFF9C4A4A)

private val LightChart =
    listOf(
        Color(0xFF1C9465),
        Color(0xFF4180E9),
        Color(0xFFAF7809),
        Color(0xFFF72121),
        Color(0xFFA958FB),
        Color(0xFF0C8F8B),
        Color(0xFF7670EF),
        Color(0xFFD05F09),
        Color(0xFFED1FA2),
    )

private val DarkCanvas = Color(0xFF0C0D10)
private val DarkSurfaceDeep = Color(0xFF080C0A)
private val DarkSurfaceSunken = Color(0xFF0F1115)
private val DarkSurfaceCard = Color(0xFF16181D)
private val DarkSurfaceRaised = Color(0xFF1F222A)
private val DarkSurfaceHigh = Color(0xFF23262E)
private val DarkSurfaceHighest = Color(0xFF2A2D35)
private val DarkForeground = Color(0xFFF4F4F5)
private val DarkMutedForeground = Color(0xFF85888F)
private val DarkPrimary = BrandPrimary
private val DarkPrimaryForeground = BrandPrimaryForeground
private val DarkDestructive = Color(0xFFF87171)
private val DarkFgSecondary = Color(0xFFC9CBD0)
private val DarkFgTertiary = Color(0xFFA8ABB2)
private val DarkFgFaint = Color(0xFF6C6F76)
private val DarkFgDisabled = Color(0xFF5A5D64)
private val DarkSuccess = Color(0xFF5BE6AE)
private val DarkSuccessStrong = Color(0xFF2FD896)
private val DarkSuccessForeground = Color(0xFF06231A)
private val DarkDanger = Color(0xFFFB8989)
private val DarkDangerStrong = Color(0xFFF87171)
private val DarkDangerForeground = Color(0xFF2A0A0A)
private val DarkInfo = Color(0xFF7BA7F0)
private val DarkWarning = Color(0xFFF5B93F)
private val DarkBorder = Color(0x0FFFFFFF)
private val DarkBorderSubtle = Color(0x0AFFFFFF)
private val DarkBorderStrong = Color(0x1AFFFFFF)
private val DarkBorderHover = Color(0x29FFFFFF)
private val DarkInput = Color(0x14FFFFFF)
private val DarkRing = Color(0x80A8C9B0)
private val DarkInputCursor = BrandPrimary
private val DarkPressedOverlay = Color(0x0CFFFFFF)
private val DarkGreenPanel = Color(0xFF1A4437)
private val DarkRedChip = Color(0xFF40282A)
private val DarkRedText = Color(0xFFE7B7B7)

private val DarkChart =
    listOf(
        Color(0xFF2FD896),
        Color(0xFF7BA7F0),
        Color(0xFFF5B93F),
        Color(0xFFFB8989),
        Color(0xFFC084FC),
        Color(0xFF10C5BF),
        Color(0xFFADA9F5),
        Color(0xFFF89A53),
        Color(0xFFF68FD0),
    )

@Immutable
internal data class ForegroundColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val faint: Color,
    val disabled: Color,
)

@Immutable
internal data class SurfaceColors(
    val deep: Color,
    val sunken: Color,
    val card: Color,
    val raised: Color,
    val high: Color,
    val highest: Color,
)

@Immutable
internal data class BrandColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
)

@Immutable
internal data class StatusColor(val base: Color, val strong: Color, val foreground: Color)

@Immutable
internal data class StatusColors(
    val success: StatusColor,
    val danger: StatusColor,
    val info: Color,
    val warning: Color,
)

@Immutable
internal data class BorderColors(
    val subtle: Color,
    val base: Color,
    val strong: Color,
    val hover: Color,
    val input: Color,
    val ring: Color,
)

@Immutable internal data class InteractionColors(val pressedOverlay: Color)

@Immutable internal data class InputColors(val cursor: Color)

@Immutable
internal data class NavigationColors(
    val surface: Color,
    val content: Color,
    val selected: Color,
    val selectedContainer: Color,
    val divider: Color,
)

@Immutable
internal data class TallybookColors(
    val canvas: Color,
    val surface: SurfaceColors,
    val brand: BrandColors,
    val foreground: ForegroundColors,
    val status: StatusColors,
    val border: BorderColors,
    val interaction: InteractionColors,
    val input: InputColors,
    val chart: List<Color>,
    val navigation: NavigationColors,
)

internal val LightTallybookColors =
    TallybookColors(
        canvas = LightCanvas,
        surface =
            SurfaceColors(
                deep = LightSurfaceDeep,
                sunken = LightSurfaceSunken,
                card = LightSurfaceCard,
                raised = LightSurfaceRaised,
                high = LightSurfaceHigh,
                highest = LightSurfaceHighest,
            ),
        brand =
            BrandColors(
                primary = LightPrimary,
                onPrimary = LightPrimaryForeground,
                secondary = BrandSecondary,
                onSecondary = BrandSecondaryForeground,
            ),
        foreground =
            ForegroundColors(
                primary = LightForeground,
                secondary = LightFgSecondary,
                tertiary = LightFgTertiary,
                faint = LightFgFaint,
                disabled = LightFgDisabled,
            ),
        status =
            StatusColors(
                success =
                    StatusColor(
                        base = LightSuccess,
                        strong = LightSuccessStrong,
                        foreground = LightSuccessForeground,
                    ),
                danger =
                    StatusColor(
                        base = LightDanger,
                        strong = LightDangerStrong,
                        foreground = LightDangerForeground,
                    ),
                info = LightInfo,
                warning = LightWarning,
            ),
        border =
            BorderColors(
                subtle = LightBorderSubtle,
                base = LightBorder,
                strong = LightBorderStrong,
                hover = LightBorderHover,
                input = LightInput,
                ring = LightRing,
            ),
        interaction = InteractionColors(pressedOverlay = LightPressedOverlay),
        input = InputColors(cursor = LightInputCursor),
        chart = LightChart,
        navigation =
            NavigationColors(
                surface = LightSurfaceCard,
                content = LightFgTertiary,
                selected = LightPrimary,
                selectedContainer = LightGreenPanel,
                divider = LightBorderSubtle,
            ),
    )

internal val DarkTallybookColors =
    TallybookColors(
        canvas = DarkCanvas,
        surface =
            SurfaceColors(
                deep = DarkSurfaceDeep,
                sunken = DarkSurfaceSunken,
                card = DarkSurfaceCard,
                raised = DarkSurfaceRaised,
                high = DarkSurfaceHigh,
                highest = DarkSurfaceHighest,
            ),
        brand =
            BrandColors(
                primary = DarkPrimary,
                onPrimary = DarkPrimaryForeground,
                secondary = BrandSecondary,
                onSecondary = BrandSecondaryForeground,
            ),
        foreground =
            ForegroundColors(
                primary = DarkForeground,
                secondary = DarkFgSecondary,
                tertiary = DarkFgTertiary,
                faint = DarkFgFaint,
                disabled = DarkFgDisabled,
            ),
        status =
            StatusColors(
                success =
                    StatusColor(
                        base = DarkSuccess,
                        strong = DarkSuccessStrong,
                        foreground = DarkSuccessForeground,
                    ),
                danger =
                    StatusColor(
                        base = DarkDanger,
                        strong = DarkDangerStrong,
                        foreground = DarkDangerForeground,
                    ),
                info = DarkInfo,
                warning = DarkWarning,
            ),
        border =
            BorderColors(
                subtle = DarkBorderSubtle,
                base = DarkBorder,
                strong = DarkBorderStrong,
                hover = DarkBorderHover,
                input = DarkInput,
                ring = DarkRing,
            ),
        interaction = InteractionColors(pressedOverlay = DarkPressedOverlay),
        input = InputColors(cursor = DarkInputCursor),
        chart = DarkChart,
        navigation =
            NavigationColors(
                surface = DarkSurfaceCard,
                content = DarkFgTertiary,
                selected = DarkPrimary,
                selectedContainer = DarkGreenPanel,
                divider = DarkBorderSubtle,
            ),
    )

internal val LocalTallybookColors = staticCompositionLocalOf { LightTallybookColors }

internal val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightPrimaryForeground,
        primaryContainer = LightGreenPanel,
        onPrimaryContainer = LightGreenChipForeground,
        inversePrimary = DarkPrimary,
        // Secondary and tertiary share the eucalyptus role so Material's baseline purple cannot
        // appear in components that use either slot.
        secondary = BrandSecondary,
        onSecondary = BrandSecondaryForeground,
        secondaryContainer = LightSurfaceSunken,
        onSecondaryContainer = LightForeground,
        tertiary = BrandSecondary,
        onTertiary = BrandSecondaryForeground,
        tertiaryContainer = LightGreenPanel,
        onTertiaryContainer = LightGreenChipForeground,
        background = LightCanvas,
        onBackground = LightForeground,
        surface = LightCanvas,
        onSurface = LightForeground,
        surfaceVariant = LightSurfaceRaised,
        onSurfaceVariant = LightMutedForeground,
        inverseSurface = DarkSurfaceCard,
        inverseOnSurface = DarkForeground,
        error = LightDestructive,
        onError = LightDangerForeground,
        errorContainer = LightRedChip,
        onErrorContainer = LightRedText,
        outline = LightBorderStrong,
        outlineVariant = LightBorder,
        surfaceBright = LightSurfaceCard,
        surfaceDim = LightSurfaceDeep,
        surfaceContainerLowest = LightSurfaceSunken,
        surfaceContainerLow = LightSurfaceCard,
        surfaceContainer = LightSurfaceRaised,
        surfaceContainerHigh = LightSurfaceHigh,
        surfaceContainerHighest = LightSurfaceHighest,
        primaryFixed = LightGreenPanel,
        primaryFixedDim = LightGreenChip,
        onPrimaryFixed = LightGreenChipForeground,
        onPrimaryFixedVariant = LightGreenText,
        secondaryFixed = LightGreenPanel,
        secondaryFixedDim = LightGreenChip,
        onSecondaryFixed = LightGreenChipForeground,
        onSecondaryFixedVariant = LightGreenText,
        tertiaryFixed = LightGreenPanel,
        tertiaryFixedDim = LightGreenChip,
        onTertiaryFixed = LightGreenChipForeground,
        onTertiaryFixedVariant = LightGreenText,
    )

internal val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkPrimaryForeground,
        primaryContainer = BrandSecondary,
        onPrimaryContainer = BrandSecondaryForeground,
        inversePrimary = LightPrimary,
        secondary = BrandSecondary,
        onSecondary = BrandSecondaryForeground,
        secondaryContainer = DarkSurfaceSunken,
        onSecondaryContainer = DarkForeground,
        tertiary = BrandSecondary,
        onTertiary = BrandSecondaryForeground,
        tertiaryContainer = LightGreenPanel,
        onTertiaryContainer = LightGreenChipForeground,
        background = DarkCanvas,
        onBackground = DarkForeground,
        surface = DarkCanvas,
        onSurface = DarkForeground,
        surfaceVariant = DarkSurfaceRaised,
        onSurfaceVariant = DarkMutedForeground,
        inverseSurface = LightSurfaceCard,
        inverseOnSurface = LightForeground,
        error = DarkDestructive,
        onError = DarkDangerForeground,
        errorContainer = DarkRedChip,
        onErrorContainer = DarkRedText,
        outline = DarkBorderStrong,
        outlineVariant = DarkBorder,
        surfaceBright = DarkSurfaceHighest,
        surfaceDim = DarkSurfaceDeep,
        surfaceContainerLowest = DarkSurfaceSunken,
        surfaceContainerLow = DarkSurfaceCard,
        surfaceContainer = DarkSurfaceRaised,
        surfaceContainerHigh = DarkSurfaceHigh,
        surfaceContainerHighest = DarkSurfaceHighest,
        // Fixed roles are theme-independent by definition, so both schemes carry the light tones.
        primaryFixed = LightGreenPanel,
        primaryFixedDim = LightGreenChip,
        onPrimaryFixed = LightGreenChipForeground,
        onPrimaryFixedVariant = LightGreenText,
        secondaryFixed = LightGreenPanel,
        secondaryFixedDim = LightGreenChip,
        onSecondaryFixed = LightGreenChipForeground,
        onSecondaryFixedVariant = LightGreenText,
        tertiaryFixed = LightGreenPanel,
        tertiaryFixedDim = LightGreenChip,
        onTertiaryFixed = LightGreenChipForeground,
        onTertiaryFixedVariant = LightGreenText,
    )
