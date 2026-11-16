package com.kurobello.tallybook.core.ui.category

import com.kurobello.tallybook.core.model.CategoryTint
import org.jetbrains.compose.resources.StringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tint_amber
import tallybook.shared.generated.resources.category_tint_blue
import tallybook.shared.generated.resources.category_tint_emerald
import tallybook.shared.generated.resources.category_tint_indigo
import tallybook.shared.generated.resources.category_tint_neutral
import tallybook.shared.generated.resources.category_tint_orange
import tallybook.shared.generated.resources.category_tint_pink
import tallybook.shared.generated.resources.category_tint_purple
import tallybook.shared.generated.resources.category_tint_red
import tallybook.shared.generated.resources.category_tint_teal

internal fun CategoryTint.labelResource(): StringResource =
    when (this) {
      CategoryTint.EMERALD -> Res.string.category_tint_emerald
      CategoryTint.BLUE -> Res.string.category_tint_blue
      CategoryTint.AMBER -> Res.string.category_tint_amber
      CategoryTint.RED -> Res.string.category_tint_red
      CategoryTint.PURPLE -> Res.string.category_tint_purple
      CategoryTint.TEAL -> Res.string.category_tint_teal
      CategoryTint.ORANGE -> Res.string.category_tint_orange
      CategoryTint.INDIGO -> Res.string.category_tint_indigo
      CategoryTint.PINK -> Res.string.category_tint_pink
      CategoryTint.NEUTRAL -> Res.string.category_tint_neutral
    }
