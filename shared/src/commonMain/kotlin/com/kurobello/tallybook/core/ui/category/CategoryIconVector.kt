package com.kurobello.tallybook.core.ui.category

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Banknote
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Briefcase
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.Calculator
import com.composables.icons.lucide.Car
import com.composables.icons.lucide.ChefHat
import com.composables.icons.lucide.CircleDashed
import com.composables.icons.lucide.CircleParking
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Fuel
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.GraduationCap
import com.composables.icons.lucide.Hammer
import com.composables.icons.lucide.HandCoins
import com.composables.icons.lucide.HeartPulse
import com.composables.icons.lucide.Hotel
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Landmark
import com.composables.icons.lucide.Laptop
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Luggage
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.PawPrint
import com.composables.icons.lucide.Percent
import com.composables.icons.lucide.PiggyBank
import com.composables.icons.lucide.Pill
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Receipt
import com.composables.icons.lucide.School
import com.composables.icons.lucide.Scissors
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.Shirt
import com.composables.icons.lucide.ShoppingBag
import com.composables.icons.lucide.ShoppingCart
import com.composables.icons.lucide.Sofa
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Stethoscope
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.Ticket
import com.composables.icons.lucide.TrendingUp
import com.composables.icons.lucide.Tv
import com.composables.icons.lucide.UtensilsCrossed
import com.composables.icons.lucide.Wallet
import com.composables.icons.lucide.WashingMachine
import com.composables.icons.lucide.Wifi
import com.kurobello.tallybook.core.model.CategoryIcon

@Suppress("CyclomaticComplexMethod")
fun CategoryIcon.toImageVector(): ImageVector =
    when (this) {
      CategoryIcon.BANKNOTE -> Lucide.Banknote
      CategoryIcon.BOOK -> Lucide.BookOpen
      CategoryIcon.BRIEFCASE -> Lucide.Briefcase
      CategoryIcon.BUILDING_2 -> Lucide.Building2
      CategoryIcon.BUS -> Lucide.Bus
      CategoryIcon.CALCULATOR -> Lucide.Calculator
      CategoryIcon.CAR -> Lucide.Car
      CategoryIcon.CHEF_HAT -> Lucide.ChefHat
      CategoryIcon.CIRCLE_DASHED -> Lucide.CircleDashed
      CategoryIcon.COFFEE -> Lucide.Coffee
      CategoryIcon.CREDIT_CARD -> Lucide.CreditCard
      CategoryIcon.DUMBBELL -> Lucide.Dumbbell
      CategoryIcon.FILM -> Lucide.Clapperboard
      CategoryIcon.FUEL -> Lucide.Fuel
      CategoryIcon.GAMEPAD -> Lucide.Gamepad2
      CategoryIcon.GIFT -> Lucide.Gift
      CategoryIcon.GRADUATION_CAP -> Lucide.GraduationCap
      CategoryIcon.HAMMER -> Lucide.Hammer
      CategoryIcon.HAND_COINS -> Lucide.HandCoins
      CategoryIcon.HEART_PULSE -> Lucide.HeartPulse
      CategoryIcon.HOTEL -> Lucide.Hotel
      CategoryIcon.HOUSE -> Lucide.House
      CategoryIcon.LANDMARK -> Lucide.Landmark
      CategoryIcon.LAPTOP -> Lucide.Laptop
      CategoryIcon.LUGGAGE -> Lucide.Luggage
      CategoryIcon.PACKAGE -> Lucide.Package
      CategoryIcon.PALETTE -> Lucide.Palette
      CategoryIcon.PARKING -> Lucide.CircleParking
      CategoryIcon.PARTY_POPPER -> Lucide.PartyPopper
      CategoryIcon.PAW -> Lucide.PawPrint
      CategoryIcon.PERCENT -> Lucide.Percent
      CategoryIcon.PIGGY_BANK -> Lucide.PiggyBank
      CategoryIcon.PILL -> Lucide.Pill
      CategoryIcon.PLANE -> Lucide.Plane
      CategoryIcon.RECEIPT -> Lucide.Receipt
      CategoryIcon.SCHOOL -> Lucide.School
      CategoryIcon.SCISSORS -> Lucide.Scissors
      CategoryIcon.SHIELD -> Lucide.Shield
      CategoryIcon.SHIRT -> Lucide.Shirt
      CategoryIcon.SHOPPING_BAG -> Lucide.ShoppingBag
      CategoryIcon.SHOPPING_CART -> Lucide.ShoppingCart
      CategoryIcon.SOFA -> Lucide.Sofa
      CategoryIcon.SPARKLES -> Lucide.Sparkles
      CategoryIcon.STETHOSCOPE -> Lucide.Stethoscope
      CategoryIcon.TAG -> Lucide.Tag
      CategoryIcon.TICKET -> Lucide.Ticket
      CategoryIcon.TRENDING_UP -> Lucide.TrendingUp
      CategoryIcon.TV -> Lucide.Tv
      CategoryIcon.UTENSILS -> Lucide.UtensilsCrossed
      CategoryIcon.WALLET -> Lucide.Wallet
      CategoryIcon.WASHING_MACHINE -> Lucide.WashingMachine
      CategoryIcon.WIFI -> Lucide.Wifi
    }
