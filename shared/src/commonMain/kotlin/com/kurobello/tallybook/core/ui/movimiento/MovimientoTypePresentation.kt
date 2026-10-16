package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookSegmentTone
import com.kurobello.tallybook.core.ui.theme.TallybookColors
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.StringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income

@Immutable
internal data class MovimientoTypePresentation(
    val icon: ImageVector,
    val label: StringResource,
    val tone: TallybookSegmentTone,
    val accent: (TallybookColors) -> Color,
)

private val presentations: Map<TipoMovimiento, MovimientoTypePresentation> =
    mapOf(
        TipoMovimiento.GASTO to
            MovimientoTypePresentation(
                icon = Lucide.ArrowUpRight,
                label = Res.string.expense,
                tone = TallybookSegmentTone.Expense,
                accent = { it.status.danger.base },
            ),
        TipoMovimiento.INGRESO to
            MovimientoTypePresentation(
                icon = Lucide.ArrowDownLeft,
                label = Res.string.income,
                tone = TallybookSegmentTone.Income,
                accent = { it.status.success.base },
            ),
    )

internal val TipoMovimiento.presentation: MovimientoTypePresentation
  get() = presentations.getValue(this)

internal val TipoMovimiento.accentColor: Color
  @Composable get() = presentation.accent(TallybookTheme.colors)
