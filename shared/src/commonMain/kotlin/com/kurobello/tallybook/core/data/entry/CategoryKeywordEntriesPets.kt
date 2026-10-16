package com.kurobello.tallybook.core.data.entry

internal val petKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_mascotas",
        strong =
            """
            mascota, pet, perro, gato, dog, cat, cachorro, puppy, kitten, perrito, gatito,
            pet shop, petshop, pet store, tienda de mascotas, animal, hamster, conejo, pajaro,
            canario, tortuga, cobaya, gatinho, cao
            """,
    ) +
        keywords(
            "cat_comida_mascota",
            strong =
                """
                croquetas, alimento para perro, comida de perro, comida de gato, alimento para
                gato, dog food, cat food, pet food, kibble, pienso, balanceado, alimento
                balanceado, racao, whiskas, pedigree, dog chow, cat chow, purina, royal canin,
                hills, eukanuba, pro plan, nutrecan, ringo, dogui, snacks para perro, premios
                para perro, arena para gatos, arena sanitaria, cat litter, litter, areia
                higienica, granulado sanitario, concentrado para perro, concentrado para gato,
                comida para mascotas, treats, huesos
                """,
            weak =
                """
                concentrado, petco, petsmart, chewy, petz, cobasi, pet zone
                """,
        ) +
        keywords(
            "cat_veterinario",
            strong =
                """
                veterinario, veterinaria, vet, veterinary, clinica veterinaria, consulta
                veterinaria, desparasitante, desparasitacion, antipulgas, pipeta,
                esterilizacion, castracion, spay, neuter, microchip, peluqueria canina,
                grooming, baño canino, guarderia canina, paseador de perros, dog walker,
                adiestramiento, adiestrador, dog trainer, hotel para perros, pension canina,
                pet sitter, veterinario de emergencia, banho e tosa, tosa, petlove,
                cirugia veterinaria, vacunas del perro, vacunas del gato, dog grooming, baño
                de perro
                """,
            weak =
                """
                vacuna, petco, petsmart, chewy, petz, cobasi, pet zone
                """,
        ) +
        keywords(
            "cat_accesorios_mascota",
            strong =
                """
                correa, arnes, placa de identificacion, cama para perro, camita, jaula,
                transportadora, rascador, juguete para perro, juguete para gato,
                juguetes para mascotas, comedero, bebedero, plato para perro, bozal,
                ropa para perro, impermeable para perro, casita para perro, pecera, terrario,
                cortaunas, cepillo para perro, shampoo para perro, bolsas para popo,
                bolsas de caca, dog toy, leash, harness, cat tree, pet bed, crate, coleira,
                brinquedo para cachorro, caminha, bebedouro, comedouro, accesorios para perro,
                accesorios para gato, pet accessories, cat toy
                """,
            weak =
                """
                collar, petco, petsmart, chewy, petz, cobasi, pet zone
                """,
        )
