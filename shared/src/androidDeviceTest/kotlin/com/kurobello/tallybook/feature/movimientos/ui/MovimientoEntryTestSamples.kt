package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint

internal fun testEntrySamples() =
    MovimientoEntrySamples(
        cinema = Categoria("cat_cine", "Cinema", null, CategoryIcon.FILM, CategoryTint.INFO),
        food = Categoria("cat_comida", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
        transport =
            Categoria("cat_transporte", "Transport", null, CategoryIcon.CAR, CategoryTint.BLUE),
        other =
            Categoria(
                CATEGORIA_OTROS_ID,
                "Other",
                null,
                CategoryIcon.CIRCLE_DASHED,
                CategoryTint.NEUTRAL,
            ),
        health =
            Categoria("cat_salud", "Health", null, CategoryIcon.HEART_PULSE, CategoryTint.ROSE),
        home = Categoria("cat_hogar", "Home", null, CategoryIcon.HOUSE, CategoryTint.EMERALD),
        gifts = Categoria("cat_regalo", "Gifts", null, CategoryIcon.GIFT, CategoryTint.PURPLE),
    )
