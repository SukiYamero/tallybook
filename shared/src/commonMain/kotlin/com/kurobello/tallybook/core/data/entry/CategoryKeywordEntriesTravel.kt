package com.kurobello.tallybook.core.data.entry

internal val travelKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_viajes",
        strong =
            """
            travel, vacaciones, vacation, holiday, turismo, tourism, viaje de negocios,
            business trip, agencia de viajes, travel agency, paquete turistico, all inclusive,
            todo incluido, crucero, cruise, pasaporte, passport, viajar, viagem,
            viagens, gastos de viaje, travel expenses, travel budget, vacaciones familiares,
            luna de miel, honeymoon, road trip, mochileo, backpacking
            """,
        weak =
            """
            viaje, trip, maleta, equipaje, luggage
            """,
    ) +
        keywords(
            "cat_vuelos",
            strong =
                """
                vuelo, flight, avion, boleto de avion, tiquete aereo, tiquete de avion,
                pasaje aereo, pasaje de avion, ticket de avion, aerolinea, airline,
                equipaje extra, maleta extra, tasa aeroportuaria, tarifa de embarque,
                latam, avianca, aeromexico, volaris, viva aerobus, viva air, jetsmart,
                sky airline, flybondi, aerolineas argentinas, copa airlines, wingo, satena,
                easyfly, iberia, air europa, klm, lufthansa, american airlines, southwest,
                jetblue, ryanair, easyjet, vueling, voepass, passagem aerea, voo, aviao,
                skyscanner, kayak, despegar, decolar, cheaptickets, millas, milhas,
                flight ticket, plane ticket, round trip, ida y vuelta, one way, air ticket,
                boarding pass, pase de abordar
                """,
            weak =
                """
                pasaje, boleto, ticket, bilhete, tiquete, reserva, reservacion, reservation,
                equipaje, maleta, luggage, expedia, aeropuerto, airport
                """,
        ) +
        keywords(
            "cat_hospedaje",
            strong =
                """
                hotel, hostal, hostel, airbnb, booking, hospedaje, alojamiento, posada, pousada,
                motel, resort, cabaña, cabin, hospedagem, noche de hotel, vrbo, trivago,
                lodging, accommodation, bnb, bed and breakfast, glamping, alojamento, estadia,
                inn, lodge, casa rural, hotel boutique, oyo, marriott, hilton, hyatt, ibis,
                decameron, sheraton, holiday inn, best western, radisson, accor, intercontinental,
                fiesta americana, city express, wyndham, melia, barcelo, iberostar,
                diaria de hotel, apartamento turistico, check out, late check out
                """,
            weak =
                """
                reserva, reservacion, reservation, camping, expedia
                """,
        ) +
        keywords(
            "cat_paseos",
            strong =
                """
                paseo, tour, tour guiado, guia turistico, entrada museo, museo, museum,
                zoologico, zoo, parque nacional, national park, parque tematico, theme park,
                disneyland, disney world, universal studios, six flags, xcaret, xel ha,
                machu picchu, chichen itza, city tour, free tour, walking tour, guided tour,
                snorkel, buceo, diving, scuba, rafting, parapente, paragliding, zipline,
                tirolesa, senderismo, hiking, trekking, admission, entry fee, sightseeing,
                atraccion, attraction, mirador, teleferico, cable car, funicular, safari,
                pasadia, day trip, balneario, termales, termas, hot springs, ingresso parque,
                passeio, parque aquatico, water park, viator, getyourguide, civitatis
                """,
            weak =
                """
                entrada, ticket, boleto, excursion, playa, acuario, camping, parque
                """,
        )
