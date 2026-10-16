package com.kurobello.tallybook.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategorySeedCatalogTest {

  @Test
  fun categorySeedCatalogMatchesTheLegacyTaxonomy() {
    val actual =
        CategorySeedCatalog.categories.map {
          SeedExpectation(it.id, it.padreId, it.icono.storageKey, it.color.storageKey)
        }

    assertEquals(expectedCatalog, actual)
    assertEquals(59, actual.size)
    assertEquals(13, actual.count { it.padreId == null })
    assertEquals(actual.size, actual.map(SeedExpectation::id).distinct().size)
    actual.forEachIndexed { index, category ->
      assertTrue(category.icono.isNotBlank())
      assertTrue(category.color.isNotBlank())
      category.padreId?.let { padreId ->
        assertTrue(actual.indexOfFirst { it.id == padreId } in 0 until index)
      }
    }
  }

  private data class SeedExpectation(
      val id: String,
      val padreId: String?,
      val icono: String,
      val color: String,
  )

  private companion object {
    val expectedCatalog =
        listOf(
            SeedExpectation("cat_comida", null, "utensils", "amber"),
            SeedExpectation("cat_supermercado", "cat_comida", "shopping-cart", "amber"),
            SeedExpectation("cat_restaurante", "cat_comida", "chef-hat", "amber"),
            SeedExpectation("cat_cafe", "cat_comida", "coffee", "amber"),
            SeedExpectation("cat_domicilios", "cat_comida", "package", "amber"),
            SeedExpectation("cat_transporte", null, "car", "blue"),
            SeedExpectation("cat_gasolina", "cat_transporte", "fuel", "blue"),
            SeedExpectation("cat_taxi", "cat_transporte", "car", "blue"),
            SeedExpectation("cat_transporte_publico", "cat_transporte", "bus", "blue"),
            SeedExpectation("cat_parqueadero", "cat_transporte", "parking", "blue"),
            SeedExpectation("cat_hogar", null, "house", "purple"),
            SeedExpectation("cat_arriendo", "cat_hogar", "building-2", "purple"),
            SeedExpectation("cat_servicios", "cat_hogar", "receipt", "purple"),
            SeedExpectation("cat_internet", "cat_hogar", "wifi", "purple"),
            SeedExpectation("cat_reparaciones", "cat_hogar", "hammer", "purple"),
            SeedExpectation("cat_compras", null, "shopping-bag", "rose"),
            SeedExpectation("cat_ropa", "cat_compras", "shirt", "rose"),
            SeedExpectation("cat_electronica", "cat_compras", "laptop", "rose"),
            SeedExpectation("cat_muebles", "cat_compras", "sofa", "rose"),
            SeedExpectation("cat_varios", "cat_compras", "package", "rose"),
            SeedExpectation("cat_salud", null, "heart-pulse", "success"),
            SeedExpectation("cat_medico", "cat_salud", "stethoscope", "success"),
            SeedExpectation("cat_farmacia", "cat_salud", "pill", "success"),
            SeedExpectation("cat_gimnasio", "cat_salud", "dumbbell", "success"),
            SeedExpectation("cat_seguro", "cat_salud", "shield", "success"),
            SeedExpectation("cat_ocio", null, "ticket", "info"),
            SeedExpectation("cat_cine", "cat_ocio", "film", "info"),
            SeedExpectation("cat_streaming", "cat_ocio", "tv", "info"),
            SeedExpectation("cat_salidas", "cat_ocio", "party-popper", "info"),
            SeedExpectation("cat_juegos", "cat_ocio", "gamepad", "info"),
            SeedExpectation("cat_educacion", null, "graduation-cap", "amber"),
            SeedExpectation("cat_cursos", "cat_educacion", "school", "amber"),
            SeedExpectation("cat_libros", "cat_educacion", "book", "amber"),
            SeedExpectation("cat_colegio", "cat_educacion", "building-2", "amber"),
            SeedExpectation("cat_cuidado_personal", null, "sparkles", "blue"),
            SeedExpectation("cat_peluqueria", "cat_cuidado_personal", "scissors", "blue"),
            SeedExpectation("cat_belleza", "cat_cuidado_personal", "palette", "blue"),
            SeedExpectation("cat_lavanderia", "cat_cuidado_personal", "washing-machine", "blue"),
            SeedExpectation("cat_mascotas", null, "paw", "purple"),
            SeedExpectation("cat_comida_mascota", "cat_mascotas", "shopping-cart", "purple"),
            SeedExpectation("cat_veterinario", "cat_mascotas", "stethoscope", "purple"),
            SeedExpectation("cat_accesorios_mascota", "cat_mascotas", "shopping-bag", "purple"),
            SeedExpectation("cat_viajes", null, "luggage", "rose"),
            SeedExpectation("cat_vuelos", "cat_viajes", "plane", "rose"),
            SeedExpectation("cat_hospedaje", "cat_viajes", "hotel", "rose"),
            SeedExpectation("cat_paseos", "cat_viajes", "ticket", "rose"),
            SeedExpectation("cat_finanzas", null, "calculator", "danger"),
            SeedExpectation("cat_impuestos", "cat_finanzas", "landmark", "danger"),
            SeedExpectation("cat_comisiones", "cat_finanzas", "percent", "danger"),
            SeedExpectation("cat_ahorro", "cat_finanzas", "piggy-bank", "danger"),
            SeedExpectation("cat_deuda", "cat_finanzas", "credit-card", "danger"),
            SeedExpectation("cat_caja_menor", "cat_finanzas", "wallet", "danger"),
            SeedExpectation("cat_ingresos", null, "banknote", "emerald"),
            SeedExpectation("cat_sueldo", "cat_ingresos", "briefcase", "emerald"),
            SeedExpectation("cat_freelance", "cat_ingresos", "laptop", "emerald"),
            SeedExpectation("cat_ventas", "cat_ingresos", "trending-up", "emerald"),
            SeedExpectation("cat_regalo", "cat_ingresos", "gift", "emerald"),
            SeedExpectation("cat_reembolso", "cat_ingresos", "hand-coins", "emerald"),
            SeedExpectation("cat_otros", null, "circle-dashed", "neutral"),
        )
  }
}
