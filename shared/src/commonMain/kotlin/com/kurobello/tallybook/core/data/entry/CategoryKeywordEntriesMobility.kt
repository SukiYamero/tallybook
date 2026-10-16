package com.kurobello.tallybook.core.data.entry

internal val mobilityKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_transporte",
        strong =
            """
            transporte, transport, transportation, movilidad, cambio de aceite, oil change,
            troca de oleo, revision tecnomecanica, tecnomecanica, soat, verificacion
            vehicular, lavado de carro, car wash, lavadero de carros, lavagem
            do carro, llantas, neumaticos, pneus, bateria del carro, frenos, alineacion,
            balanceo, mantenimiento del carro, mantenimiento del auto, mantenimiento de la moto,
            peaje, toll, pedagio, telepeaje, autopista, carro, auto, coche, vehiculo, car,
            vehicle, moto, motocicleta, bicicleta, bike, scooter, patineta, monopatin,
            licencia de conducir, comparendo, multa de transito, ticket de transito,
            repuestos, autopartes, accesorios del carro, polarizado, tuning,
            lubricentro, lubricadora, cambio de llantas, pinchazo
            """,
        weak =
            """
            aceite, oil, oleo, taller, multa, mecanico, mecanica, carburador
            """,
    ) +
        keywords(
            "cat_gasolina",
            strong =
                """
                gasolina, nafta, bencina, combustible, gasoil, diesel, petrol, gas station,
                gasolinera, estacion de servicio, estacion de gasolina, bomba de gasolina,
                tanqueo, tanquear, llenar el tanque, tanque lleno, fuel, refuel, fill up,
                posto de gasolina, abastecimento, abastecer, etanol, gnv, glp, gasolina premium,
                gasolina corriente, gasolina extra, terpel, primax, texaco, shell, mobil,
                chevron, ypf, axion, puma energy, copec, petrobras, pemex, bp, esso, repsol,
                gulf, sunoco, exxon, petroperu, ipiranga, biomax, ecopetrol
                """,
            weak =
                """
                gas, grifo, posto, alcool
                """,
        ) +
        keywords(
            "cat_taxi",
            strong =
                """
                taxi, uber, didi, cabify, indriver, indrive, in driver, bolt, lyft, easy taxi,
                easytaxi, tappsi, picap, mototaxi, moto taxi, taxi app, ride, rideshare,
                ride share, cab, yango, free now, radiotaxi, remis, remise, 99 pop, carrera en
                taxi, viaje en taxi, viaje en uber, uber moto, uber black, uber comfort,
                uberx, didi express, taxi seguro, taxista, motorista, corrida de aplicativo,
                corrida de uber, tarifa dinamica, surge, carona paga
                """,
            weak =
                """
                carrera, viaje, trip
                """,
        ) +
        keywords(
            "cat_transporte_publico",
            strong =
                """
                bus, buses, autobus, colectivo, micro, metrobus, transmilenio, sitp, tren,
                tren ligero, subte, subterraneo, tullave, tarjeta tullave, civica, tarjeta bip,
                bip, tarjeta metro, pasaje de bus, pasaje urbano, combi, tranvia, tram,
                light rail, trolebus, trolley, ferry, lancha, metrocable, cable aereo, ecobici,
                bicicleta publica, onibus, bilhete unico, trem, passagem de onibus, bus fare,
                train fare, train, transit, public transport, transporte publico, metro de
                santiago, metro de lima, metro de medellin, vale transporte, recarga
                tarjeta de transporte, recarga de transporte, oyster, metrocard, abono transporte, monthly pass, bus pass
                """,
            weak =
                """
                metro, subway, pasaje, boleto, ticket, recarga, bilhete
                """,
        ) +
        keywords(
            "cat_parqueadero",
            strong =
                """
                parqueadero, parqueo, estacionamiento, parking, cochera, playa de estacionamiento,
                valet, valet parking, parquimetro, estacionar, estacionamento, vaga de
                estacionamento, aparcamiento, aparcar, parking meter, parking ticket,
                zona azul, zona de parqueo, easypark, parkmobile, parkwhiz, spothero,
                mensualidad parqueadero, parqueadero mensual, parking garage, garage parking,
                parqueadero publico, estacionamiento publico, estacionamento rotativo,
                parquear, parkear
                """,
            weak =
                """
                garaje, garagem, garage, ticket
                """,
        )
