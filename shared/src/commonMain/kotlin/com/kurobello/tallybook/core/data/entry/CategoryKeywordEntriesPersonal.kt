package com.kurobello.tallybook.core.data.entry

internal val personalKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_cuidado_personal",
        strong =
            """
            cuidado personal, personal care, higiene, aseo personal, desodorante, shampoo,
            champu, pasta dental, cepillo de dientes, crema dental, enjuague bucal,
            papel higienico, toallas higienicas, toallitas, pañales, afeitadora, rasuradora,
            maquinilla, cuchillas, gillette, dove, nivea, colgate, oral b, pantene, head and
            shoulders, axe, rexona, tampones, protector solar, bloqueador, hygiene, toothpaste,
            deodorant, razor, sabonete, escova de dentes, fio dental, hilo dental, diapers,
            cuidado del bebe, jabon de manos, jabon de baño
            """,
        weak =
            """
            aseo, jabon, crema
            """,
    ) +
        keywords(
            "cat_peluqueria",
            strong =
                """
                peluqueria, peluquero, barberia, barbero, barber, barbershop, haircut,
                corte de pelo, corte de cabello, cabello, tinte, tintura, balayage, mechas,
                alisado, keratina, brushing, cepillado, tratamiento capilar, estilista,
                hairdresser, cabeleireiro, corte de cabelo, barba, afeitado, rasurado, shave,
                great clips, supercuts, fantastic sams, peinado, rulos, extensiones, hair
                salon, hair cut, corte, cabelo, salon de peluqueria, tintura de cabello
                """,
            weak =
                """
                salon, secado
                """,
        ) +
        keywords(
            "cat_belleza",
            strong =
                """
                belleza, beauty, maquillaje, makeup, make up, cosmeticos, cosmetics, perfume,
                perfumeria, fragancia, crema facial, skincare, cuidado de la piel, uñas,
                manicure, pedicure, esmalte, spa, masaje, massage, depilacion, waxing, cejas,
                pestañas, lashes, microblading, botox, estetica, centro de estetica,
                salon de belleza, salao de beleza, esteticista, sephora, maybelline, loreal,
                mac cosmetics, natura, avon, esika, lbel, cyzone, yanbal, belcorp, boticario,
                eudora, bath and body works, ulta, sally beauty, bronceado, bronceador, tanning,
                tratamiento facial, facial, limpieza facial, peeling, nails, serum,
                labial, rimel, base de maquillaje, sombras, unhas, cilios, sobrancelha
                """,
            weak =
                """
                crema, salon
                """,
        ) +
        keywords(
            "cat_lavanderia",
            strong =
                """
                lavanderia, laundry, laundromat, tintoreria, dry cleaning, lavado en seco,
                lavandaria, lavar ropa, lavado de ropa, planchado, planchar, lavaseco,
                lavatodo, lavamatic, lavanderia automatica, wash and fold, coin laundry,
                tinturaria, lavagem a seco, lavagem de roupa, lavanderia self service,
                lavado y planchado, ropa sucia, dobrado
                """,
            weak =
                """
                detergente, suavizante, suavitel, downy, ariel, lavado, blanqueador,
                quitamanchas
                """,
        )
