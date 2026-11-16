package com.kurobello.tallybook.core.ui.category

import com.kurobello.tallybook.core.model.CategoryTint
import org.jetbrains.compose.resources.StringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tint_amber
import tallybook.shared.generated.resources.category_tint_blue
import tallybook.shared.generated.resources.category_tint_danger
import tallybook.shared.generated.resources.category_tint_emerald
import tallybook.shared.generated.resources.category_tint_info
import tallybook.shared.generated.resources.category_tint_neutral
import tallybook.shared.generated.resources.category_tint_purple
import tallybook.shared.generated.resources.category_tint_rose
import tallybook.shared.generated.resources.category_tint_success

internal fun CategoryTint.labelResource(): StringResource =
    when (this) {
      CategoryTint.EMERALD -> Res.string.category_tint_emerald
      CategoryTint.BLUE -> Res.string.category_tint_blue
      CategoryTint.AMBER -> Res.string.category_tint_amber
      CategoryTint.ROSE -> Res.string.category_tint_rose
      CategoryTint.PURPLE -> Res.string.category_tint_purple
      CategoryTint.SUCCESS -> Res.string.category_tint_success
      CategoryTint.DANGER -> Res.string.category_tint_danger
      CategoryTint.INFO -> Res.string.category_tint_info
      CategoryTint.NEUTRAL -> Res.string.category_tint_neutral
    }
