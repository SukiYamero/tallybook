package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_required

@Composable
fun CategoryField(
    selected: Categoria?,
    isError: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val label = stringResource(Res.string.category)
  val selectedName = selected?.nombre ?: stringResource(Res.string.category_required)
  val errorMessage = stringResource(Res.string.category_required).takeIf { isError }
  val borderColor =
      if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .border(width = 1.dp, color = borderColor, shape = MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) { errorMessage?.let(::error) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selected != null) CategoryAvatar(categoria = selected)
      Text(
          text = selectedName,
          modifier = Modifier.weight(1f),
          color =
              if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant
              else MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyLarge,
      )
      Icon(
          imageVector = Lucide.ChevronRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
