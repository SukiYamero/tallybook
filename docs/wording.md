# Voice and tone — el estándar

Portado casi verbatim de `web/moneta/docs/voice-and-tone.md` — mismas reglas, mismo criterio, para que
la voz de la app sea una sola entre web y mobile. Ground truth acá: los string resources de Compose
Multiplatform (`shared/src/commonMain/composeResources/values-*/strings.xml`); si una regla de acá y un
string ya shippeado no coinciden, arreglar el que esté mal y actualizar este doc en el mismo cambio.

## 1. Encabezar con la razón del usuario, no con lo que pide la app

Un string que dice lo que la app necesita se lee como la app pidiéndole algo al usuario. Un string que
dice lo que el usuario gana se lee como la app cuidándolo — mismo pedido, mismo resultado, pero el
segundo hace que el usuario quiera cumplir en vez de sentirse obligado.

**Antes:** "Necesitamos los dos permisos de Drive para sincronizar. Intenta de nuevo y marca ambos."
**Después:** "Para que tus datos queden guardados y sincronizados sin problemas, necesitamos los dos
permisos de Drive. Inténtalo de nuevo y acepta ambos."

La diferencia es estructural, no decorativa: abrir con "para que [beneficio para el usuario]", recién
ahí nombrar lo que la app necesita, y cerrar con la acción. Nunca abrir con "necesitamos" — eso es el
deseo de la app liderando, no la razón del usuario.

## 2. Tranquilizar donde hay plata o datos en juego

Esta es una app de finanzas; un mensaje de error de sync, Drive o datos es el momento en que un usuario
más probablemente teme haber perdido algo. Decir claramente que no se perdió nada antes de pedirle que
haga algo ("tus datos quedan guardados", "lo que registraste está guardado en este dispositivo") en vez
de una declaración técnica seca del fallo.

## 3. Traducir la intención, no la oración

`es` es la fuente de verdad de la estructura razón-primero (§1), pero `en`/`pt-BR` nunca son una
traducción literal palabra por palabra de eso — reformulan la misma estructura en lo que sea idiomático
en ese idioma. Un calco que es gramaticalmente correcto pero suena forzado ("So that your data stays
saved…") pierde contra la frase que un hablante nativo realmente escribiría ("To keep your data
saved…"), aunque la segunda esté más lejos de la sintaxis del original en español. Chequear cada string
nuevo contra cómo se escribe normalmente la copy de UX en ese idioma, no contra la sintaxis del español.

## 4. Trato informal y directo — lo que eso signifique en cada idioma

Todos los locales usan trato informal, pero "informal" significa algo distinto en cada uno, así que hay
que ajustarse al techo propio del idioma, no al del español:

- `es` es tú ("intenta", "revisa"); `es-AR` es voseo ("intentá", "revisá") — nunca copiar la forma tú
  literal en `es-AR`.
- `pt-BR` usa "você", ya el registro informal estándar en portugués brasileño — no hay un registro más
  bajo al que apuntar.
- `en` no tiene distinción gramatical de formalidad en "you" — la informalidad tiene que salir de la
  elección de palabras y construcción de la oración (§3), no de un pronombre, porque el inglés no tiene
  uno que elegir.

## 5. Sin jerga, sin calidez de relleno

Decir "permisos de Drive", nunca "scopes" ni "OAuth". Decir "sincronizar", nunca "hacer sync". La
calidez viene de la estructura razón-primero (§1) y de la tranquilidad (§2), no de signos de
exclamación, emojis o diminutivos — ninguno de esos se usa hoy en la copy de esta app, y agregarlos acá
sería inconsistente con el resto de la UI.

## 6. Ser breve

Una o dos oraciones. Un opener razón-primero que se estira tres cláusulas pierde el punto — el usuario
tiene que entender el "por qué" y la acción en una sola lectura, no parsear un párrafo para encontrar el
botón que necesita tocar.

## 7. Glosario: cuentas, medios de pago y transferencias

"Transferencia" nombra dos cosas distintas en el producto y la copy no puede mezclarlas: el **medio de
pago** (plata que se manda a otra persona o se recibe de ella, `MedioPago.TRANSFERENCIA`) y el
**movimiento de plata entre las cuentas propias** (`Transferencia`), que no es ni gasto ni ingreso. Por
eso:

- La palabra sola ("Transferencia", "Transferência"; en inglés "Bank transfer") es siempre el medio de
  pago. En inglés el medio no se acorta a "Transfer", para que no se lea como la acción interna.
- La transferencia interna siempre lleva "entre cuentas" o "entre tus cuentas": acción "Mover entre
  cuentas", sustantivo "Transferencia entre tus cuentas", y "Entre cuentas" como etiqueta corta donde no
  entra más (chip de tipo en Búsqueda, filas de Historial).
- Pagar una tarjeta es una transferencia interna ("Pagar tarjeta"): las compras ya se registraron como
  gastos, así que la copy nunca lo presenta como un gasto nuevo.
- La deuda de la tarjeta y el consumo del ciclo son estimaciones (el usuario puede no registrar todas las
  compras) y siempre se rotulan así; nunca "Deuda" o "Saldo de la tarjeta" a secas.
- Los nombres de cuenta son del usuario y no se traducen; solo la cuenta de efectivo por defecto nace
  con el nombre del idioma vigente al crearla.

| Concepto | `es` | `es-AR` | `en` | `pt-BR` |
| --- | --- | --- | --- | --- |
| Medio: efectivo | Efectivo | Efectivo | Cash | Dinheiro |
| Medio: débito | Débito | Débito | Debit | Débito |
| Medio: transferencia a/de terceros | Transferencia | Transferencia | Bank transfer | Transferência |
| Medio: crédito | Crédito | Crédito | Credit | Crédito |
| Tipo de cuenta: efectivo | Efectivo | Efectivo | Cash | Dinheiro |
| Tipo de cuenta: banco o billetera | Banco o billetera | Banco o billetera virtual | Bank or wallet | Banco ou carteira digital |
| Tipo de cuenta: tarjeta de crédito | Tarjeta de crédito | Tarjeta de crédito | Credit card | Cartão de crédito |
| Acción de transferencia interna | Mover entre cuentas | Mover entre cuentas | Move between accounts | Mover entre contas |
| Sustantivo de transferencia interna | Transferencia entre tus cuentas | Transferencia entre tus cuentas | Transfer between your accounts | Transferência entre suas contas |
| Etiqueta corta de transferencia interna | Entre cuentas | Entre cuentas | Between accounts | Entre contas |
| Pagar la tarjeta (transferencia interna) | Pagar tarjeta | Pagar tarjeta | Pay card | Pagar cartão |
| Deuda estimada de la tarjeta | Deuda estimada | Deuda estimada | Estimated debt | Dívida estimada |
| Consumo del ciclo actual | Consumo del ciclo | Consumo del ciclo | Spending this cycle | Gastos do ciclo |

Los rótulos de medio y tipo de cuenta viven en `composeResources/values*/strings_accounts.xml`; las
pantallas de cuentas agregan ahí sus claves nuevas en vez de tocar `strings.xml`.
