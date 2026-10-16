package com.kurobello.tallybook.core.data.entry

internal val foodKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_comida",
        strong =
            """
            comida, alimentos, alimentacion, food, meal, eating, refeicao, gastos de comida,
            food expenses, comer
            """,
    ) +
        keywords(
            "cat_supermercado",
            strong =
                """
                supermercado, super, hipermercado, supermarket, grocery, groceries, grocery store,
                mercado, mercadito, mercadona, despensa, abarrotes, viveres, verduras, frutas,
                verduleria, fruteria, carniceria, huevos, leche, arroz, compras del super,
                compras de la casa, compra semanal, compra del mes, feira, hortifruti, acougue,
                atacado, atacadao, assai, pao de acucar, whole foods, kroger, safeway, aldi, lidl,
                tesco, trader joe's, costco, sam's club, pricesmart, makro, walmart,
                bodega aurrera, soriana, chedraui, heb, city market, exito, carulla,
                jumbo, olimpica, justo y bueno, surtimax, carrefour, coto, vea,
                changomas, lider, unimarc, santa isabel, tottus, plaza vea, wong, vivanda,
                la sirena, supermercados bravo, pali, ara tiendas, merqueo, jokr,
                oxxo, seven eleven, minimarket, mini market, tienda de barrio
                """,
            weak =
                """
                aceite, oil, oleo, agua, pan, tienda, bodega, snacks, bebidas, cerveza, licor, vino, ron,
                aguardiente, tequila, whisky, carne, asado, pasta, metro, cornershop, disco, alkosto,
                kiosco, almacen
                """,
        ) +
        keywords(
            "cat_restaurante",
            strong =
                """
                restaurante, restaurant, resto, almuerzo, cena, desayuno, brunch, lunch, dinner,
                breakfast, buffet, parrilla, pizzeria, pizza, hamburguesa, burger, sushi, tacos,
                taqueria, ceviche, cevicheria, polleria, chifa, parrillada, comida rapida,
                fast food, comer fuera, salir a comer, menu del dia, corrientazo, bandeja paisa,
                almoco, jantar, rodizio, churrasco, churrascaria, lanchonete, marmita, feijoada,
                hot dog, perro caliente, salchipapa, choripan, milanesa, empanadas, alitas, wings,
                steak, steakhouse, bbq, mcdonalds, burger king, kfc, dominos, papa johns,
                pizza hut, el corral, frisby, crepes y waffles, taco bell, chipotle, carls jr,
                little caesars, bembos, pardos chicken, chilis, applebees, olive garden, outback,
                wendys, popeyes, habibs, bobs, giraffas, madero, vips, sanborns, toks, wok,
                archies, nandos, five guys, panda express, dairy queen, hooters,
                tgi fridays
                """,
            weak =
                """
                propina, tip, reserva, reservacion, reservation, comida del dia, lanche, carne,
                asado, pasta, torta, subway
                """,
        ) +
        keywords(
            "cat_cafe",
            strong =
                """
                cafe, cafecito, cafeteria, coffee, coffee shop, cappuccino, capuchino, latte,
                espresso, americano, mocha, macchiato, cortado, flat white, frappe, frappuccino,
                tinto, chai, matcha, medialunas, croissant, donut, muffin, bagel, brownie,
                cafezinho, cafe com leite, cafe da manha, padaria, bakery, panaderia, pasteleria,
                postre, helado, heladeria, gelato, sorvete, sorveteria, jugo, juice, suco, batido,
                smoothie, bubble tea, boba, tea, starbucks, juan valdez, oma, tostao, dunkin,
                blue bottle, crepes, waffle, churros, cupcake, tres leches, cheesecake,
                pastel de nata, pan de bono, almojabana
                """,
            weak =
                """
                snacks, pan, pan dulce, galletas, torta
                """,
        ) +
        keywords(
            "cat_domicilios",
            strong =
                """
                domicilio, delivery, food delivery, rappi, uber eats, ubereats, didi food,
                pedidosya, pedidos ya, glovo, ifood, domiciliario, repartidor, entregador,
                pedido a domicilio, pedir comida, ordenar comida, takeout, take out, takeaway,
                doordash, grubhub, postmates, just eat, deliveroo, delivery fee, costo de envio,
                propina domiciliario, keeta, rappi turbo, rappi prime, pedido online,
                comida a domicilio, entrega a domicilio
                """,
            weak =
                """
                envio, entrega, mensajero, loggi, pedido, cornershop
                """,
        )
