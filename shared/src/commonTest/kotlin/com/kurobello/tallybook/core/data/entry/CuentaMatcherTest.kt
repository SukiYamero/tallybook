package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CuentaMatcherTest {
  private val matcher = CuentaMatcher()

  private fun ids(text: String, cuentas: List<Cuenta>): List<String> =
      matcher.match(text, cuentas).map(CuentaMatch::cuentaId)

  @Test
  fun aNameMatchesOnlyAsWholeWords() {
    val cuentas = listOf(cuenta("nu", "Nu"))

    assertEquals(listOf("nu"), ids("Pagué con Nu 20k", cuentas))
    assertEquals(emptyList(), ids("nueve mil en nubank", cuentas))
  }

  @Test
  fun aNameMatchesIgnoringCaseAndAccents() {
    val cuentas = listOf(cuenta("cf", "Crédito Fácil"), cuenta("bb", "BANCO DE BOGOTÁ"))

    assertEquals(listOf("cf"), ids("ropa con credito facil", cuentas))
    assertEquals(listOf("bb"), ids("arriendo banco de bogota", cuentas))
    assertEquals(listOf("cf"), ids("ropa CRÉDITO FÁCIL", cuentas))
  }

  @Test
  fun aMultiWordNameMatchesOnlyWhenItsWordsAppearTogetherInOrder() {
    val cuentas = listOf(cuenta("bb", "Banco de Bogotá"))

    assertEquals(emptyList(), ids("Bogotá de banco", cuentas))
    assertEquals(emptyList(), ids("banco en Bogotá", cuentas))
  }

  @Test
  fun theSpanCoversTheNameAsTyped() {
    val text = "Gasolina NU 360, 50k"
    val match = matcher.match(text, listOf(cuenta("nu", "Nu 360"))).single()

    assertEquals("NU 360", text.substring(match.start, match.endExclusive))
  }

  @Test
  fun matchesFollowTheirOrderInTheSentence() {
    val cuentas = listOf(cuenta("davi", "Daviplata"), cuenta("nequi", "Nequi"))

    assertEquals(listOf("nequi", "davi"), ids("de Nequi a Daviplata 30 mil", cuentas))
  }

  @Test
  fun overlappingNamesKeepTheEarliestAndThenTheLongest() {
    val cuentas =
        listOf(
            cuenta("banco", "Banco"),
            cuenta("pichincha", "Banco Pichincha"),
            cuenta("ahorro", "Pichincha Ahorro"),
        )

    assertEquals(listOf("pichincha"), ids("con banco pichincha ahorro", cuentas))
    assertEquals(listOf("pichincha", "banco"), ids("banco pichincha y banco", cuentas))
  }

  @Test
  fun archivedAccountsNeverMatch() {
    val cuentas = listOf(cuenta("old", "Nequi", archivada = true), cuenta("nu", "Nu"))

    assertEquals(emptyList(), ids("pagué con nequi", cuentas))
  }

  @Test
  fun theSameNameInTwoCurrenciesReturnsBothOnTheSameSpan() {
    val cuentas = listOf(cuenta("cop", "Efectivo", moneda = Moneda.COP), cuenta("usd", "Efectivo"))

    val matches = matcher.match("almuerzo en efectivo", cuentas)

    assertEquals(listOf("cop", "usd"), matches.map(CuentaMatch::cuentaId))
    assertEquals(1, matches.map { it.start to it.endExclusive }.distinct().size)
  }

  @Test
  fun aNameThatIsAlsoATagMatchesBoth() {
    val text = "hotel viaje 300 mil"

    assertEquals(listOf("viaje"), ids(text, listOf(cuenta("viaje", "Viaje"))))
    assertEquals(
        listOf("Viaje"),
        EtiquetaMatcher().match(text, listOf(Etiqueta("Viaje", "Viaje", CREATED_AT))),
    )
  }

  @Test
  fun blankTextOrNoAccountsMatchNothing() {
    assertEquals(emptyList(), ids("   ", listOf(cuenta("nu", "Nu"))))
    assertEquals(emptyList(), ids("con nu", emptyList()))
  }
}

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")

internal fun cuenta(
    id: String,
    nombre: String,
    tipo: TipoCuenta = TipoCuenta.BANCO,
    moneda: Moneda = Moneda.USD,
    archivada: Boolean = false,
) =
    Cuenta(
        id,
        nombre,
        tipo,
        Money(0, moneda),
        null,
        archivada,
        CREATED_AT,
        color = tipo.defaultColor,
    )
