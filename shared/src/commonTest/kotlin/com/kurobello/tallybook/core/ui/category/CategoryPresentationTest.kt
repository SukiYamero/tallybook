package com.kurobello.tallybook.core.ui.category

import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.LightTallybookColors
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryPresentationTest {

  @Test
  fun everyCategoryIconResolvesToTheExpectedVector() {
    val expected =
        mapOf(
            CategoryIcon.BANKNOTE to "banknote",
            CategoryIcon.BOOK to "book-open",
            CategoryIcon.BRIEFCASE to "briefcase",
            CategoryIcon.BUILDING_2 to "building-2",
            CategoryIcon.BUS to "bus",
            CategoryIcon.CALCULATOR to "calculator",
            CategoryIcon.CAR to "car",
            CategoryIcon.CHEF_HAT to "chef-hat",
            CategoryIcon.CIRCLE_DASHED to "circle-dashed",
            CategoryIcon.COFFEE to "coffee",
            CategoryIcon.CREDIT_CARD to "credit-card",
            CategoryIcon.DUMBBELL to "dumbbell",
            CategoryIcon.FILM to "clapperboard",
            CategoryIcon.FUEL to "fuel",
            CategoryIcon.GAMEPAD to "gamepad-2",
            CategoryIcon.GIFT to "gift",
            CategoryIcon.GRADUATION_CAP to "graduation-cap",
            CategoryIcon.HAMMER to "hammer",
            CategoryIcon.HAND_COINS to "hand-coins",
            CategoryIcon.HEART_PULSE to "heart-pulse",
            CategoryIcon.HOTEL to "hotel",
            CategoryIcon.HOUSE to "house",
            CategoryIcon.LANDMARK to "landmark",
            CategoryIcon.LAPTOP to "laptop",
            CategoryIcon.LUGGAGE to "luggage",
            CategoryIcon.PACKAGE to "package",
            CategoryIcon.PALETTE to "palette",
            CategoryIcon.PARKING to "circle-parking",
            CategoryIcon.PARTY_POPPER to "party-popper",
            CategoryIcon.PAW to "paw-print",
            CategoryIcon.PERCENT to "percent",
            CategoryIcon.PIGGY_BANK to "piggy-bank",
            CategoryIcon.PILL to "pill",
            CategoryIcon.PLANE to "plane",
            CategoryIcon.RECEIPT to "receipt",
            CategoryIcon.SCHOOL to "school",
            CategoryIcon.SCISSORS to "scissors",
            CategoryIcon.SHIELD to "shield",
            CategoryIcon.SHIRT to "shirt",
            CategoryIcon.SHOPPING_BAG to "shopping-bag",
            CategoryIcon.SHOPPING_CART to "shopping-cart",
            CategoryIcon.SOFA to "sofa",
            CategoryIcon.SPARKLES to "sparkles",
            CategoryIcon.STETHOSCOPE to "stethoscope",
            CategoryIcon.TAG to "tag",
            CategoryIcon.TICKET to "ticket",
            CategoryIcon.TRENDING_UP to "trending-up",
            CategoryIcon.TV to "tv",
            CategoryIcon.UTENSILS to "utensils-crossed",
            CategoryIcon.WALLET to "wallet",
            CategoryIcon.WASHING_MACHINE to "washing-machine",
            CategoryIcon.WIFI to "wifi",
        )

    assertEquals(CategoryIcon.entries.toSet(), expected.keys)
    expected.forEach { (icon, vectorName) -> assertEquals(vectorName, icon.toImageVector().name) }
  }

  @Test
  fun everyCategoryTintResolvesToItsThemeRole() {
    val expected =
        mapOf(
            CategoryTint.EMERALD to LightTallybookColors.chart[0],
            CategoryTint.BLUE to LightTallybookColors.chart[1],
            CategoryTint.AMBER to LightTallybookColors.chart[2],
            CategoryTint.ROSE to LightTallybookColors.chart[3],
            CategoryTint.PURPLE to LightTallybookColors.chart[4],
            CategoryTint.SUCCESS to LightTallybookColors.status.success.base,
            CategoryTint.DANGER to LightTallybookColors.status.danger.base,
            CategoryTint.INFO to LightTallybookColors.status.info,
            CategoryTint.NEUTRAL to null,
        )

    assertEquals(CategoryTint.entries.toSet(), expected.keys)
    expected.forEach { (tint, color) ->
      assertEquals(color, tint.accentColor(LightTallybookColors))
    }
  }
}
