package com.kurobello.tallybook.core.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.tintColors

/** Decorative: every place that shows it also shows the account's name and type. */
@Composable
internal fun CuentaAvatar(tipo: TipoCuenta, color: CategoryTint, modifier: Modifier = Modifier) {
  CategoryAvatar(tipo.icon, color.tintColors(), modifier)
}
