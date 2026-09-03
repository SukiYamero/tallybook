# Business logic — Tallybook

Única referencia de reglas de negocio del dominio, compartidas por Android e iOS vía `shared/commonMain`.
Se actualiza en el lugar a medida que la lógica cambia — no se duplica en otro `.md` ni se versiona por
decisión histórica (eso lo cubre git).

## Qué es la app

App de finanzas personales tipo budget/wallet para Android e iOS. El objetivo es que el usuario entienda
mejor sus finanzas — en qué gastó, cuánto ingresó, cómo queda su balance — no reemplaza un banco ni
sincroniza cuentas bancarias reales.

## Movimiento (gasto o ingreso)

Un **movimiento** registra un gasto o un ingreso.

- **Obligatorios**: tipo (gasto / ingreso), monto, fecha, categoría.
- **Opcionales**: descripción (por ahora el único campo opcional).

## Tracking y consulta

- Ver y filtrar movimientos por día, semana, mes o año.
- Buscar movimientos dentro de esos rangos de fecha.
- Balance general (ingresos − gastos) siempre visible.
- Vista de gráfico semanal de movimientos/balance.

## Tema

Tema claro y oscuro — de dónde salen los colores/tokens concretos, ver `docs/stack.md`.
