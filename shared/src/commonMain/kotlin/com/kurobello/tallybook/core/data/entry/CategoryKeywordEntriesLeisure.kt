package com.kurobello.tallybook.core.data.entry

internal val leisureKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_ocio",
        strong =
            """
            ocio, entretenimiento, entertainment, diversion, lazer, fun, hobby, pasatiempo,
            gastos de ocio, espectaculo, show, evento, events, recreacion, recreation
            """,
        weak =
            """
            weekend, fin de semana
            """,
    ) +
        keywords(
            "cat_cine",
            strong =
                """
                cine, cinema, pelicula, movie, movie theater, entradas de cine, boletas de cine,
                boleto de cine, ticket de cine, cinemark, cinepolis, cinecolombia, procinal,
                cineplanet, cinemex, hoyts, showcase, amc, regal, cinemastar, kinepolis,
                ingresso de cinema, palomitas, pochoclos, canchita, popcorn,
                cabritas, cinesa, cineplex, imax, estreno, filme, funcion de cine, pipoca,
                sesion de cine, movie night, noche de cine, cinema vip, sala de cine
                """,
            weak =
                """
                entrada, boleto, ticket, bilhete
                """,
        ) +
        keywords(
            "cat_streaming",
            strong =
                """
                streaming, netflix, spotify, disney, disney plus, hbo, hbo max, prime video,
                amazon prime, paramount, paramount plus, apple tv, apple music, youtube premium,
                youtube music, deezer, tidal, crunchyroll, star plus, claro video, vix, mubi,
                twitch, audible, dazn, espn plus, directv go, movistar tv, globoplay, telecine,
                premiere, starzplay, peacock, hulu, discovery plus, apple one, podcast,
                suscripcion de musica, suscripcion de video, music subscription, tv por
                streaming, amazon music, soundcloud
                """,
            weak =
                """
                suscripcion, subscription, membresia, mensualidad, patreon, plan familiar, amazon
                """,
        ) +
        keywords(
            "cat_salidas",
            strong =
                """
                salidas, bar, pub, discoteca, antro, boliche, nightclub, boite, balada,
                festa, fiesta, party, concierto, concert, festival, karaoke, bowling, billar,
                trago, coctel, chelas, birra, cerveja, drinks, happy hour, rumba,
                parranda, reventon, previa, afterparty, cover, teatro, obra de teatro, musical,
                stand up, parque de diversiones, parque de atracciones, feria, rooftop, lounge,
                vinoteca, cantina, taberna, pulqueria, cerveceria, brewery, ticketmaster,
                tuboleta, eventbrite, passline, ticketek, sympla, livepass, salir de fiesta,
                salir de rumba, noche de bares, tomar algo, after office, afterwork
                """,
            weak =
                """
                cerveza, licor, vino, ron, aguardiente, tequila, whisky, disco, entrada,
                cumpleaños, ticket, boleto, casino
                """,
        ) +
        keywords(
            "cat_juegos",
            strong =
                """
                videojuegos, juegos, game, gaming, steam, epic games, playstation plus, ps plus,
                xbox game pass, game pass, nintendo eshop, eshop, psn, xbox live, fortnite, v
                bucks, robux, roblox, minecraft, valorant, league of legends, free fire, pubg,
                fifa, ea sports, genshin, supercell, clash royale, candy crush, juegos de mesa,
                board game, ps, gamestop, gamer, joystick, dlc, battle pass, pase de batalla,
                loot box, apuestas, bet, bingo, poker, jogo, videogame, tragamonedas,
                esports, ranked, skins, steam wallet, google play juegos
                """,
            weak =
                """
                consola, playstation, xbox, nintendo, switch, casino, loteria,
                lottery
                """,
        )
