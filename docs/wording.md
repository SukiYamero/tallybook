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
