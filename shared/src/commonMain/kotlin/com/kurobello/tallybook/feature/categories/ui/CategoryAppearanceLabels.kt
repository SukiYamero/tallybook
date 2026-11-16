package com.kurobello.tallybook.feature.categories.ui

import com.kurobello.tallybook.core.model.CategoryIcon
import org.jetbrains.compose.resources.StringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_icon_banknote
import tallybook.shared.generated.resources.category_icon_book
import tallybook.shared.generated.resources.category_icon_briefcase
import tallybook.shared.generated.resources.category_icon_building_2
import tallybook.shared.generated.resources.category_icon_bus
import tallybook.shared.generated.resources.category_icon_calculator
import tallybook.shared.generated.resources.category_icon_car
import tallybook.shared.generated.resources.category_icon_chef_hat
import tallybook.shared.generated.resources.category_icon_circle_dashed
import tallybook.shared.generated.resources.category_icon_coffee
import tallybook.shared.generated.resources.category_icon_credit_card
import tallybook.shared.generated.resources.category_icon_dumbbell
import tallybook.shared.generated.resources.category_icon_film
import tallybook.shared.generated.resources.category_icon_fuel
import tallybook.shared.generated.resources.category_icon_gamepad
import tallybook.shared.generated.resources.category_icon_gift
import tallybook.shared.generated.resources.category_icon_graduation_cap
import tallybook.shared.generated.resources.category_icon_hammer
import tallybook.shared.generated.resources.category_icon_hand_coins
import tallybook.shared.generated.resources.category_icon_heart_pulse
import tallybook.shared.generated.resources.category_icon_hotel
import tallybook.shared.generated.resources.category_icon_house
import tallybook.shared.generated.resources.category_icon_landmark
import tallybook.shared.generated.resources.category_icon_laptop
import tallybook.shared.generated.resources.category_icon_luggage
import tallybook.shared.generated.resources.category_icon_package
import tallybook.shared.generated.resources.category_icon_palette
import tallybook.shared.generated.resources.category_icon_parking
import tallybook.shared.generated.resources.category_icon_party_popper
import tallybook.shared.generated.resources.category_icon_paw
import tallybook.shared.generated.resources.category_icon_percent
import tallybook.shared.generated.resources.category_icon_piggy_bank
import tallybook.shared.generated.resources.category_icon_pill
import tallybook.shared.generated.resources.category_icon_plane
import tallybook.shared.generated.resources.category_icon_receipt
import tallybook.shared.generated.resources.category_icon_school
import tallybook.shared.generated.resources.category_icon_scissors
import tallybook.shared.generated.resources.category_icon_shield
import tallybook.shared.generated.resources.category_icon_shirt
import tallybook.shared.generated.resources.category_icon_shopping_bag
import tallybook.shared.generated.resources.category_icon_shopping_cart
import tallybook.shared.generated.resources.category_icon_sofa
import tallybook.shared.generated.resources.category_icon_sparkles
import tallybook.shared.generated.resources.category_icon_stethoscope
import tallybook.shared.generated.resources.category_icon_tag
import tallybook.shared.generated.resources.category_icon_ticket
import tallybook.shared.generated.resources.category_icon_trending_up
import tallybook.shared.generated.resources.category_icon_tv
import tallybook.shared.generated.resources.category_icon_utensils
import tallybook.shared.generated.resources.category_icon_wallet
import tallybook.shared.generated.resources.category_icon_washing_machine
import tallybook.shared.generated.resources.category_icon_wifi

@Suppress("CyclomaticComplexMethod")
internal fun CategoryIcon.labelResource(): StringResource =
    when (this) {
      CategoryIcon.BANKNOTE -> Res.string.category_icon_banknote
      CategoryIcon.BOOK -> Res.string.category_icon_book
      CategoryIcon.BRIEFCASE -> Res.string.category_icon_briefcase
      CategoryIcon.BUILDING_2 -> Res.string.category_icon_building_2
      CategoryIcon.BUS -> Res.string.category_icon_bus
      CategoryIcon.CALCULATOR -> Res.string.category_icon_calculator
      CategoryIcon.CAR -> Res.string.category_icon_car
      CategoryIcon.CHEF_HAT -> Res.string.category_icon_chef_hat
      CategoryIcon.CIRCLE_DASHED -> Res.string.category_icon_circle_dashed
      CategoryIcon.COFFEE -> Res.string.category_icon_coffee
      CategoryIcon.CREDIT_CARD -> Res.string.category_icon_credit_card
      CategoryIcon.DUMBBELL -> Res.string.category_icon_dumbbell
      CategoryIcon.FILM -> Res.string.category_icon_film
      CategoryIcon.FUEL -> Res.string.category_icon_fuel
      CategoryIcon.GAMEPAD -> Res.string.category_icon_gamepad
      CategoryIcon.GIFT -> Res.string.category_icon_gift
      CategoryIcon.GRADUATION_CAP -> Res.string.category_icon_graduation_cap
      CategoryIcon.HAMMER -> Res.string.category_icon_hammer
      CategoryIcon.HAND_COINS -> Res.string.category_icon_hand_coins
      CategoryIcon.HEART_PULSE -> Res.string.category_icon_heart_pulse
      CategoryIcon.HOTEL -> Res.string.category_icon_hotel
      CategoryIcon.HOUSE -> Res.string.category_icon_house
      CategoryIcon.LANDMARK -> Res.string.category_icon_landmark
      CategoryIcon.LAPTOP -> Res.string.category_icon_laptop
      CategoryIcon.LUGGAGE -> Res.string.category_icon_luggage
      CategoryIcon.PACKAGE -> Res.string.category_icon_package
      CategoryIcon.PALETTE -> Res.string.category_icon_palette
      CategoryIcon.PARKING -> Res.string.category_icon_parking
      CategoryIcon.PARTY_POPPER -> Res.string.category_icon_party_popper
      CategoryIcon.PAW -> Res.string.category_icon_paw
      CategoryIcon.PERCENT -> Res.string.category_icon_percent
      CategoryIcon.PIGGY_BANK -> Res.string.category_icon_piggy_bank
      CategoryIcon.PILL -> Res.string.category_icon_pill
      CategoryIcon.PLANE -> Res.string.category_icon_plane
      CategoryIcon.RECEIPT -> Res.string.category_icon_receipt
      CategoryIcon.SCHOOL -> Res.string.category_icon_school
      CategoryIcon.SCISSORS -> Res.string.category_icon_scissors
      CategoryIcon.SHIELD -> Res.string.category_icon_shield
      CategoryIcon.SHIRT -> Res.string.category_icon_shirt
      CategoryIcon.SHOPPING_BAG -> Res.string.category_icon_shopping_bag
      CategoryIcon.SHOPPING_CART -> Res.string.category_icon_shopping_cart
      CategoryIcon.SOFA -> Res.string.category_icon_sofa
      CategoryIcon.SPARKLES -> Res.string.category_icon_sparkles
      CategoryIcon.STETHOSCOPE -> Res.string.category_icon_stethoscope
      CategoryIcon.TAG -> Res.string.category_icon_tag
      CategoryIcon.TICKET -> Res.string.category_icon_ticket
      CategoryIcon.TRENDING_UP -> Res.string.category_icon_trending_up
      CategoryIcon.TV -> Res.string.category_icon_tv
      CategoryIcon.UTENSILS -> Res.string.category_icon_utensils
      CategoryIcon.WALLET -> Res.string.category_icon_wallet
      CategoryIcon.WASHING_MACHINE -> Res.string.category_icon_washing_machine
      CategoryIcon.WIFI -> Res.string.category_icon_wifi
    }
