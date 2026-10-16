package com.kurobello.tallybook.core.data.entry

internal val learningKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_educacion",
        strong =
            """
            educacion, education, estudio, estudiar, gastos educativos, ensino, universidad,
            university, universidade, matricula universitaria, semestre, posgrado, maestria,
            doctorado, carrera universitaria, tesis, icfes, icetex, tuition, college, facultad,
            pregrado, vestibular, enem, pos graduacao
            """,
    ) +
        keywords(
            "cat_cursos",
            strong =
                """
                curso, course, bootcamp, certificacion, certification, udemy, coursera, platzi,
                domestika, edx, linkedin learning, duolingo, babbel, rosetta stone, skillshare,
                masterclass, codecademy, crehana, alura, tutor, tutoria, profesor particular,
                clase particular, idiomas, ingles, english class,
                language class, curso de ingles, capacitacion, seminario, webinar, congreso,
                mba, diplomado, escuela de manejo, clases de manejo, clases de piano,
                clases de guitarra, clases de baile, dance class, music lessons, piano
                lessons, lesson, curso online, online course, workshop, aula de ingles, aula,
                pluralsight, datacamp, treehouse, toefl, ielts, cambridge, clases de idiomas
                """,
            weak =
                """
                clase, taller, academia, instituto, inscripcion, matricula, mensualidad,
                conferencia
                """,
        ) +
        keywords(
            "cat_libros",
            strong =
                """
                libro, book, libreria, bookstore, ebook, audiolibro, audiobook, libro de texto,
                textbook, novela, comic, manga, magazine, livro, livraria, sebo, saraiva,
                amazon books, panamericana, lerner, libreria nacional, casa del libro, gandhi,
                el ateneo, cuspide, sbs, crisol, barnes and noble, waterstones, kobo, wattpad,
                enciclopedia, diccionario, kindle unlimited, libros usados, used books, cuentos, poesia
                """,
            weak =
                """
                revista, apuntes, fotocopias, periodico
                """,
        ) +
        keywords(
            "cat_colegio",
            strong =
                """
                colegio, escuela, school, pension escolar, utiles escolares, uniforme escolar,
                libros escolares, kinder, jardin infantil, guarderia, daycare, preschool,
                preescolar, nursery, creche, escolinha, mensalidade escolar, material escolar,
                material didactico, cuaderno, lapiz, lapices, lonchera, transporte escolar,
                ruta escolar, bus escolar, paseo escolar, tarea, proyecto escolar,
                feria de ciencias, graduacion, acto escolar, colegiatura, cuota escolar,
                cuota del colegio, pagamento escola, escola, cuota de inscripcion, school
                supplies, school fees, back to school, utiles, lunchbox
                """,
            weak =
                """
                matricula, mensualidad, inscripcion, papeleria, apuntes, fotocopias, mochila,
                uniforme, clase, instituto, pension, excursion
                """,
        )
