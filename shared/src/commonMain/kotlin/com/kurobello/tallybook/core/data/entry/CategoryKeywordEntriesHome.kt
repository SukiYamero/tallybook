package com.kurobello.tallybook.core.data.entry

internal val homeKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_hogar",
        strong =
            """
            hogar, casa, home, household, gastos de casa, gastos del hogar, house expenses,
            home expenses, cuota de administracion, empleada domestica, empleada del hogar,
            señora del aseo, nana, mucama, housekeeper, cleaning lady, faxina, diarista,
            jardinero, jardineria, gardener, productos de limpieza, limpieza del hogar,
            fumigacion, casa nueva, lar
            """,
        weak =
            """
            administracion, aseo, limpieza, mudanza, decoracion, condominio
            """,
    ) +
        keywords(
            "cat_arriendo",
            strong =
                """
                arriendo, alquiler, renta, rent, aluguel, arrendamiento, pago de renta,
                pago del arriendo, alquiler del apartamento, arriendo apartamento,
                alquiler de departamento, rent payment, house rent, apartment rent, hipoteca,
                mortgage, cuota hipoteca, dividendo hipotecario, financiamento imobiliario,
                expensas, gastos comunes, cuota de mantenimiento, deposito de arriendo, fianza,
                landlord, arrendador, casero, inmobiliaria, housing, vivienda
                """,
            weak =
                """
                mantenimiento, administracion, deposito, condominio
                """,
        ) +
        keywords(
            "cat_servicios",
            strong =
                """
                luz, electricidad, energia, energia electrica, acueducto, alcantarillado,
                gas natural, gas domiciliario, gas de cilindro, cilindro de gas, garrafa,
                balon de gas, tanque de gas, recibo de luz, recibo del agua, factura de luz,
                factura del agua, servicios publicos, servicios, utilities, electricity,
                water bill, electric bill, power bill, gas bill, sewer, trash, basura,
                enel, codensa, epm, celsia, emcali, edenor, edesur, cfe, cge, chilectra,
                luz del sur, edeeste, edenorte, naturgy, vanti, metrogas, camuzzi, sabesp,
                copasa, cemig, energisa, sedapal, aguas andinas, esval, aysa, agua potable,
                conta de luz, conta de agua, conta de gas, agua y luz, luz y agua
                """,
            weak =
                """
                gas, agua, aseo, recibo, factura, recarga, claro, movistar, tigo, entel, telcel,
                personal, vivo, tim, wom, telefono, celular, plan celular
                """,
        ) +
        keywords(
            "cat_internet",
            strong =
                """
                internet, wifi, wi fi, fibra, fibra optica, broadband, banda ancha,
                plan de internet, plan de datos, router, modem, mensualidad internet,
                internet movil, datos moviles, mobile data, data plan, cable e internet,
                triple play, directv, starlink, netlife, etb, une, tigo une, claro hogar,
                movistar hogar, telmex, izzi, totalplay, megacable, axtel, telecentro, fibertel,
                iplan, movistar fibra, vtr, gtd, entel hogar, bitel, vivo fibra, oi fibra,
                brisanet, spectrum, comcast, xfinity, verizon fios, cox, frontier, optimum
                """,
            weak =
                """
                claro, movistar, tigo, entel, telcel, personal, vivo, tim, wom, telefono,
                celular, plan celular, flow
                """,
        ) +
        keywords(
            "cat_reparaciones",
            strong =
                """
                reparacion, arreglo de la casa, plomero, plomeria, fontanero, fontaneria,
                electricista, cerrajero, cerrajeria, pintor, pintura, pintar la casa, albañil,
                remodelacion, ferreteria, herramientas, tornillos, taladro, home depot, sodimac,
                leroy merlin, lowes, ace hardware, construrama, cemex,
                tlapaleria, pegamento, cemento, plumber, electrician, handyman, repair,
                contractor, painter, locksmith, hardware store, tools, bombillo, foco,
                gotera, filtracion, encanador, eletricista, pedreiro, marceneiro, conserto,
                reparo, cerradura, techo, humedad, impermeabilizante, obra negra, obra blanca,
                construccion
                """,
            weak =
                """
                taller, mecanico, mantenimiento, tecnico, arreglo, tinta, reforma, lampara, homecenter
                """,
        )
