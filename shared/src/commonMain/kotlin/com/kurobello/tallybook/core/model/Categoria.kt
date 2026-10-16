package com.kurobello.tallybook.core.model

data class Categoria(
    val id: String,
    val nombre: String,
    val padreId: String?,
    val icono: CategoryIcon,
    val color: CategoryTint,
    val descripcion: String? = null,
    val origen: CategoriaOrigen = CategoriaOrigen.SISTEMA,
    val oculta: Boolean = false,
)

enum class CategoriaOrigen(val storageKey: String) {
  SISTEMA("sistema"),
  USUARIO("usuario"),
}

const val CATEGORIA_OTROS_ID: String = "cat_otros"
