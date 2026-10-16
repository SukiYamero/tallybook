package com.kurobello.tallybook.core.data.entry

internal val shoppingKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_compras",
        strong =
            """
            compras, shopping, centro comercial, mall, tienda por departamentos,
            outlet, black friday, cyber monday, hot sale, cyber day, compras online,
            online shopping, retail, shopping center, loja de departamentos
            """,
    ) +
        keywords(
            "cat_ropa",
            strong =
                """
                ropa, ropa interior, camisa, camiseta, playera, remera, polera, pantalon, jeans,
                vaqueros, vestido, falda, short, chaqueta, chamarra, campera, abrigo, sweater,
                buzo, sudadera, hoodie, zapatos, zapatillas, tenis, botas, sandalias, medias,
                calcetines, ropa deportiva, traje, corbata, cinturon, gorra, sombrero, bufanda,
                pijama, lenceria, clothes, clothing, shoes, sneakers, shirt, dress, pants, jacket,
                roupa, camisa polo, sapatos, calca, vestuario, zara, nike, adidas, puma,
                levis, uniqlo, bershka, stradivarius, shein, forever 21, arturo calle,
                offcorss, studio f, koaj, velez, lacoste, tommy hilfiger, calvin klein, vans,
                converse, new balance, reebok, under armour, gap, old navy, zalando, renner,
                riachuelo, marisa, hering, centauro, netshoes, dafiti, pull and bear
                """,
            weak =
                """
                falabella, ripley, paris, cartera, tallas, arreglo, disfraz, uniforme, mercado
                libre, amazon, aliexpress, temu, ebay, shopee, magalu, americanas
                """,
        ) +
        keywords(
            "cat_electronica",
            strong =
                """
                celular nuevo, telefono celular, smartphone, iphone, samsung, galaxy, xiaomi,
                motorola, huawei, laptop, portatil, computador, computadora, notebook, tablet,
                ipad, macbook, monitor, teclado, mouse, audifonos, auriculares, headphones,
                airpods, parlante, bocina, speaker, altavoz, cargador, memoria usb, usb,
                disco duro, ssd, impresora, printer, camara, gopro, televisor, television, smart
                tv, pantalla, smartwatch, apple watch, reloj inteligente, drone, electrodomestico,
                licuadora, microondas, nevera, refrigerador, heladera, lavadora, estufa,
                aire acondicionado, ventilador, plancha, aspiradora, freidora de aire, air fryer,
                cafetera, batidora, tostadora, kindle, funda celular, protector de pantalla,
                apple, best buy, mediamarkt, fnac, ktronix, mac center, steren, coolbox, lg, sony,
                philips, dell, hp, lenovo, asus, acer, logitech, jbl, bose, casas bahia, ponto
                frio, fast shop, kabum, geladeira, fogao, fone de ouvido
                """,
            weak =
                """
                alkosto, falabella, ripley, mercado libre, amazon, aliexpress, temu, ebay,
                shopee, magalu, americanas, tecnico, consola, playstation, xbox, nintendo, switch,
                cable, bateria, pilas
                """,
        ) +
        keywords(
            "cat_muebles",
            strong =
                """
                mueble, sofa, sillon, silla, cama, colchon, closet, ropero, escritorio, estante,
                repisa, comoda, mesa de noche, mesita, comedor, juego de comedor, cortinas,
                alfombra, tapete, espejo, ikea, tugo, rimax, muebles dico, jamar, furniture, couch,
                mattress, bed, desk, wardrobe, bookshelf, dresser, nightstand, rug, curtains,
                colchao, guarda roupa, poltrona, moveis, movel, mesa de jantar, home decor,
                tok stok, etna, madeira madeira, sala de estar, sofa cama, litera, cajonera,
                zapatero, perchero
                """,
            weak =
                """
                mesa, decoracion, cuadro, lampara, decor, homecenter, falabella
                """,
        ) +
        keywords(
            "cat_varios",
            strong =
                """
                varios, miscelanea, juguete, floristeria, flores, ramo de flores,
                peluche, globos, tarjeta de regalo, gift card, bazar, miniso, daiso, flying
                tiger, dollar tree, compras varias, cositas, souvenirs, recuerdos, velas,
                incienso, linio, presentes, brinquedo, misc, miscellaneous,
                odds and ends, party supplies, bolsas de regalo, papel de regalo, toys
                """,
            weak =
                """
                regalo, gift, cumpleaños, papeleria, mercado libre, amazon, aliexpress,
                temu, ebay, shopee, magalu, americanas, target, envio, encomienda, mudanza, paquete,
                etsy
                """,
        )
