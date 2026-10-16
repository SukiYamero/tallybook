package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_accesorios_mascota
import tallybook.shared.generated.resources.category_ahorro
import tallybook.shared.generated.resources.category_arriendo
import tallybook.shared.generated.resources.category_belleza
import tallybook.shared.generated.resources.category_cafe
import tallybook.shared.generated.resources.category_caja_menor
import tallybook.shared.generated.resources.category_cine
import tallybook.shared.generated.resources.category_colegio
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_comida_mascota
import tallybook.shared.generated.resources.category_comisiones
import tallybook.shared.generated.resources.category_compras
import tallybook.shared.generated.resources.category_cuidado_personal
import tallybook.shared.generated.resources.category_cursos
import tallybook.shared.generated.resources.category_deuda
import tallybook.shared.generated.resources.category_domicilios
import tallybook.shared.generated.resources.category_educacion
import tallybook.shared.generated.resources.category_electronica
import tallybook.shared.generated.resources.category_farmacia
import tallybook.shared.generated.resources.category_finanzas
import tallybook.shared.generated.resources.category_freelance
import tallybook.shared.generated.resources.category_gasolina
import tallybook.shared.generated.resources.category_gimnasio
import tallybook.shared.generated.resources.category_hogar
import tallybook.shared.generated.resources.category_hospedaje
import tallybook.shared.generated.resources.category_impuestos
import tallybook.shared.generated.resources.category_ingresos
import tallybook.shared.generated.resources.category_internet
import tallybook.shared.generated.resources.category_juegos
import tallybook.shared.generated.resources.category_lavanderia
import tallybook.shared.generated.resources.category_libros
import tallybook.shared.generated.resources.category_mascotas
import tallybook.shared.generated.resources.category_medico
import tallybook.shared.generated.resources.category_muebles
import tallybook.shared.generated.resources.category_ocio
import tallybook.shared.generated.resources.category_otros
import tallybook.shared.generated.resources.category_parqueadero
import tallybook.shared.generated.resources.category_paseos
import tallybook.shared.generated.resources.category_peluqueria
import tallybook.shared.generated.resources.category_reembolso
import tallybook.shared.generated.resources.category_regalo
import tallybook.shared.generated.resources.category_reparaciones
import tallybook.shared.generated.resources.category_restaurante
import tallybook.shared.generated.resources.category_ropa
import tallybook.shared.generated.resources.category_salidas
import tallybook.shared.generated.resources.category_salud
import tallybook.shared.generated.resources.category_seguro
import tallybook.shared.generated.resources.category_servicios
import tallybook.shared.generated.resources.category_streaming
import tallybook.shared.generated.resources.category_sueldo
import tallybook.shared.generated.resources.category_supermercado
import tallybook.shared.generated.resources.category_taxi
import tallybook.shared.generated.resources.category_transporte
import tallybook.shared.generated.resources.category_transporte_publico
import tallybook.shared.generated.resources.category_varios
import tallybook.shared.generated.resources.category_ventas
import tallybook.shared.generated.resources.category_veterinario
import tallybook.shared.generated.resources.category_viajes
import tallybook.shared.generated.resources.category_vuelos

class CategorySeedProvider(
    private val stringResolver: suspend (StringResource) -> String = ::getString,
) {
  suspend fun getCategories(): List<Categoria> =
      CategorySeedCatalog.categories.map { seed ->
        Categoria(
            id = seed.id,
            nombre = getCategoryName(seed.id),
            padreId = seed.padreId,
            icono = seed.icono,
            color = seed.color,
        )
      }

  private companion object {
    val categoryNames: Map<String, StringResource> =
        mapOf(
            "cat_comida" to Res.string.category_comida,
            "cat_supermercado" to Res.string.category_supermercado,
            "cat_restaurante" to Res.string.category_restaurante,
            "cat_cafe" to Res.string.category_cafe,
            "cat_domicilios" to Res.string.category_domicilios,
            "cat_transporte" to Res.string.category_transporte,
            "cat_gasolina" to Res.string.category_gasolina,
            "cat_taxi" to Res.string.category_taxi,
            "cat_transporte_publico" to Res.string.category_transporte_publico,
            "cat_parqueadero" to Res.string.category_parqueadero,
            "cat_hogar" to Res.string.category_hogar,
            "cat_arriendo" to Res.string.category_arriendo,
            "cat_servicios" to Res.string.category_servicios,
            "cat_internet" to Res.string.category_internet,
            "cat_reparaciones" to Res.string.category_reparaciones,
            "cat_compras" to Res.string.category_compras,
            "cat_ropa" to Res.string.category_ropa,
            "cat_electronica" to Res.string.category_electronica,
            "cat_muebles" to Res.string.category_muebles,
            "cat_varios" to Res.string.category_varios,
            "cat_salud" to Res.string.category_salud,
            "cat_medico" to Res.string.category_medico,
            "cat_farmacia" to Res.string.category_farmacia,
            "cat_gimnasio" to Res.string.category_gimnasio,
            "cat_seguro" to Res.string.category_seguro,
            "cat_ocio" to Res.string.category_ocio,
            "cat_cine" to Res.string.category_cine,
            "cat_streaming" to Res.string.category_streaming,
            "cat_salidas" to Res.string.category_salidas,
            "cat_juegos" to Res.string.category_juegos,
            "cat_educacion" to Res.string.category_educacion,
            "cat_cursos" to Res.string.category_cursos,
            "cat_libros" to Res.string.category_libros,
            "cat_colegio" to Res.string.category_colegio,
            "cat_cuidado_personal" to Res.string.category_cuidado_personal,
            "cat_peluqueria" to Res.string.category_peluqueria,
            "cat_belleza" to Res.string.category_belleza,
            "cat_lavanderia" to Res.string.category_lavanderia,
            "cat_mascotas" to Res.string.category_mascotas,
            "cat_comida_mascota" to Res.string.category_comida_mascota,
            "cat_veterinario" to Res.string.category_veterinario,
            "cat_accesorios_mascota" to Res.string.category_accesorios_mascota,
            "cat_viajes" to Res.string.category_viajes,
            "cat_vuelos" to Res.string.category_vuelos,
            "cat_hospedaje" to Res.string.category_hospedaje,
            "cat_paseos" to Res.string.category_paseos,
            "cat_finanzas" to Res.string.category_finanzas,
            "cat_impuestos" to Res.string.category_impuestos,
            "cat_comisiones" to Res.string.category_comisiones,
            "cat_ahorro" to Res.string.category_ahorro,
            "cat_deuda" to Res.string.category_deuda,
            "cat_caja_menor" to Res.string.category_caja_menor,
            "cat_ingresos" to Res.string.category_ingresos,
            "cat_sueldo" to Res.string.category_sueldo,
            "cat_freelance" to Res.string.category_freelance,
            "cat_ventas" to Res.string.category_ventas,
            "cat_regalo" to Res.string.category_regalo,
            "cat_reembolso" to Res.string.category_reembolso,
            CATEGORIA_OTROS_ID to Res.string.category_otros,
        )
  }

  private suspend fun getCategoryName(id: String): String {
    val resource = categoryNames.getValue(id)
    return stringResolver(resource)
  }
}
