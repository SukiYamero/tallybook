package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint

data class CategorySeed(
    val id: String,
    val padreId: String?,
    val icono: CategoryIcon,
    val color: CategoryTint,
)

object CategorySeedCatalog {
  val categories =
      listOf(
          category("cat_comida", icono = CategoryIcon.UTENSILS, color = CategoryTint.AMBER),
          category(
              "cat_supermercado",
              "cat_comida",
              CategoryIcon.SHOPPING_CART,
              CategoryTint.AMBER,
          ),
          category("cat_restaurante", "cat_comida", CategoryIcon.CHEF_HAT, CategoryTint.AMBER),
          category("cat_cafe", "cat_comida", CategoryIcon.COFFEE, CategoryTint.AMBER),
          category("cat_domicilios", "cat_comida", CategoryIcon.PACKAGE, CategoryTint.AMBER),
          category("cat_transporte", icono = CategoryIcon.CAR, color = CategoryTint.BLUE),
          category("cat_gasolina", "cat_transporte", CategoryIcon.FUEL, CategoryTint.BLUE),
          category("cat_taxi", "cat_transporte", CategoryIcon.CAR, CategoryTint.BLUE),
          category("cat_transporte_publico", "cat_transporte", CategoryIcon.BUS, CategoryTint.BLUE),
          category("cat_parqueadero", "cat_transporte", CategoryIcon.PARKING, CategoryTint.BLUE),
          category("cat_hogar", icono = CategoryIcon.HOUSE, color = CategoryTint.PURPLE),
          category("cat_arriendo", "cat_hogar", CategoryIcon.BUILDING_2, CategoryTint.PURPLE),
          category("cat_servicios", "cat_hogar", CategoryIcon.RECEIPT, CategoryTint.PURPLE),
          category("cat_internet", "cat_hogar", CategoryIcon.WIFI, CategoryTint.PURPLE),
          category("cat_reparaciones", "cat_hogar", CategoryIcon.HAMMER, CategoryTint.PURPLE),
          category("cat_compras", icono = CategoryIcon.SHOPPING_BAG, color = CategoryTint.ROSE),
          category("cat_ropa", "cat_compras", CategoryIcon.SHIRT, CategoryTint.ROSE),
          category("cat_electronica", "cat_compras", CategoryIcon.LAPTOP, CategoryTint.ROSE),
          category("cat_muebles", "cat_compras", CategoryIcon.SOFA, CategoryTint.ROSE),
          category("cat_varios", "cat_compras", CategoryIcon.PACKAGE, CategoryTint.ROSE),
          category("cat_salud", icono = CategoryIcon.HEART_PULSE, color = CategoryTint.SUCCESS),
          category("cat_medico", "cat_salud", CategoryIcon.STETHOSCOPE, CategoryTint.SUCCESS),
          category("cat_farmacia", "cat_salud", CategoryIcon.PILL, CategoryTint.SUCCESS),
          category("cat_gimnasio", "cat_salud", CategoryIcon.DUMBBELL, CategoryTint.SUCCESS),
          category("cat_seguro", "cat_salud", CategoryIcon.SHIELD, CategoryTint.SUCCESS),
          category("cat_ocio", icono = CategoryIcon.TICKET, color = CategoryTint.INFO),
          category("cat_cine", "cat_ocio", CategoryIcon.FILM, CategoryTint.INFO),
          category("cat_streaming", "cat_ocio", CategoryIcon.TV, CategoryTint.INFO),
          category("cat_salidas", "cat_ocio", CategoryIcon.PARTY_POPPER, CategoryTint.INFO),
          category("cat_juegos", "cat_ocio", CategoryIcon.GAMEPAD, CategoryTint.INFO),
          category(
              "cat_educacion",
              icono = CategoryIcon.GRADUATION_CAP,
              color = CategoryTint.AMBER,
          ),
          category("cat_cursos", "cat_educacion", CategoryIcon.SCHOOL, CategoryTint.AMBER),
          category("cat_libros", "cat_educacion", CategoryIcon.BOOK, CategoryTint.AMBER),
          category("cat_colegio", "cat_educacion", CategoryIcon.BUILDING_2, CategoryTint.AMBER),
          category(
              "cat_cuidado_personal",
              icono = CategoryIcon.SPARKLES,
              color = CategoryTint.BLUE,
          ),
          category(
              "cat_peluqueria",
              "cat_cuidado_personal",
              CategoryIcon.SCISSORS,
              CategoryTint.BLUE,
          ),
          category("cat_belleza", "cat_cuidado_personal", CategoryIcon.PALETTE, CategoryTint.BLUE),
          category(
              "cat_lavanderia",
              "cat_cuidado_personal",
              CategoryIcon.WASHING_MACHINE,
              CategoryTint.BLUE,
          ),
          category("cat_mascotas", icono = CategoryIcon.PAW, color = CategoryTint.PURPLE),
          category(
              "cat_comida_mascota",
              "cat_mascotas",
              CategoryIcon.SHOPPING_CART,
              CategoryTint.PURPLE,
          ),
          category(
              "cat_veterinario",
              "cat_mascotas",
              CategoryIcon.STETHOSCOPE,
              CategoryTint.PURPLE,
          ),
          category(
              "cat_accesorios_mascota",
              "cat_mascotas",
              CategoryIcon.SHOPPING_BAG,
              CategoryTint.PURPLE,
          ),
          category("cat_viajes", icono = CategoryIcon.LUGGAGE, color = CategoryTint.ROSE),
          category("cat_vuelos", "cat_viajes", CategoryIcon.PLANE, CategoryTint.ROSE),
          category("cat_hospedaje", "cat_viajes", CategoryIcon.HOTEL, CategoryTint.ROSE),
          category("cat_paseos", "cat_viajes", CategoryIcon.TICKET, CategoryTint.ROSE),
          category("cat_finanzas", icono = CategoryIcon.CALCULATOR, color = CategoryTint.DANGER),
          category("cat_impuestos", "cat_finanzas", CategoryIcon.LANDMARK, CategoryTint.DANGER),
          category("cat_comisiones", "cat_finanzas", CategoryIcon.PERCENT, CategoryTint.DANGER),
          category("cat_ahorro", "cat_finanzas", CategoryIcon.PIGGY_BANK, CategoryTint.DANGER),
          category("cat_deuda", "cat_finanzas", CategoryIcon.CREDIT_CARD, CategoryTint.DANGER),
          category("cat_caja_menor", "cat_finanzas", CategoryIcon.WALLET, CategoryTint.DANGER),
          category("cat_ingresos", icono = CategoryIcon.BANKNOTE, color = CategoryTint.EMERALD),
          category("cat_sueldo", "cat_ingresos", CategoryIcon.BRIEFCASE, CategoryTint.EMERALD),
          category("cat_freelance", "cat_ingresos", CategoryIcon.LAPTOP, CategoryTint.EMERALD),
          category("cat_ventas", "cat_ingresos", CategoryIcon.TRENDING_UP, CategoryTint.EMERALD),
          category("cat_regalo", "cat_ingresos", CategoryIcon.GIFT, CategoryTint.EMERALD),
          category("cat_reembolso", "cat_ingresos", CategoryIcon.HAND_COINS, CategoryTint.EMERALD),
          category(
              CATEGORIA_OTROS_ID,
              icono = CategoryIcon.CIRCLE_DASHED,
              color = CategoryTint.NEUTRAL,
          ),
      )

  private fun category(
      id: String,
      padreId: String? = null,
      icono: CategoryIcon,
      color: CategoryTint,
  ): CategorySeed = CategorySeed(id = id, padreId = padreId, icono = icono, color = color)
}
