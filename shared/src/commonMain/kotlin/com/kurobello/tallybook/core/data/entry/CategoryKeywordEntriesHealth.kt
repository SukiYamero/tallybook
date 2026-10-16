package com.kurobello.tallybook.core.data.entry

internal val healthKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_salud",
        strong =
            """
            salud, health, saude, bienestar, wellness, gastos de salud, health expenses,
            gastos medicos, healthcare
            """,
    ) +
        keywords(
            "cat_medico",
            strong =
                """
                medico, doctor, doctora, cita medica, consulta medica, consulta, clinica,
                hospital, urgencias, sala de emergencia, odontologo, dentista, dental,
                ortodoncia, brackets, oculista, optometra, examen de laboratorio, laboratorio,
                examenes, radiografia, ecografia, resonancia, psicologo, psicologa, psiquiatra,
                terapia, terapeuta, fisioterapia, fisioterapeuta, kinesiologo, nutricionista,
                pediatra, ginecologo, dermatologo, cardiologo, traumatologo, cuota moderadora,
                doctor visit, clinic, checkup, check up, dentist, therapy, therapist, physio,
                optometrist, glasses, eye exam, lab test, medico general, especialista, cirugia,
                surgery, hospitalizacion, ambulancia, consulta com medico, exame, pronto socorro, upa, psicoterapia, odontologia, oftalmologo, otorrino, lentes,
                gafas
                """,
            weak =
                """
                emergencia, copago, eps, vacuna, receta
                """,
        ) +
        keywords(
            "cat_farmacia",
            strong =
                """
                farmacia, drogueria, botica, pharmacy, drugstore, medicina, medicamento,
                pastillas, remedio, ibuprofeno, paracetamol, acetaminofen, aspirina, vitaminas,
                vitamins, jarabe, antibiotico, preservativos, condones, pruebas covid,
                analgesico, cvs, walgreens, rite aid, farmatodo, cruz verde, farmacias del ahorro,
                similares, fahorro, benavides, san pablo, farmacity, salcobrand, ahumada, fasa,
                inkafarma, mifarma, drogasil, droga raia, drogaria, pacheco, panvel, pague menos,
                locatel, farmacia guadalajara, la rebaja, copidrogas, curitas, tiritas, botiquin,
                inhalador, antigripal, dolex, panadol, tylenol, advil, alka seltzer, suero,
                prescription, medication, cough syrup
                """,
            weak =
                """
                receta, vacuna, crema, gotas, alcohol
                """,
        ) +
        keywords(
            "cat_gimnasio",
            strong =
                """
                gimnasio, gym, membresia gimnasio, mensualidad gimnasio, cuota del gym, crossfit,
                pilates, yoga, spinning, zumba, entrenador, personal trainer, entrenamiento,
                natacion, musculacao, treino, smart fit, smartfit, bodytech, sportlife,
                fitness first, anytime fitness, planet fitness, 24 hour fitness,
                equinox, orange theory, gold's gym, proteina, whey, creatina, suplementos,
                fitness, boxeo, artes marciales, karate, taekwondo, jiu jitsu, muay thai,
                calistenia, bioritmo, selfit, bluefit, workout, trainer, entreno, academia de
                musculacao, cuota gimnasio
                """,
            weak =
                """
                academia, piscina, box, mensualidad, inscripcion, clases
                """,
        ) +
        keywords(
            "cat_seguro",
            strong =
                """
                seguro, seguro medico, seguro de salud, seguro de vida, seguro del carro,
                seguro de auto, seguro del hogar, poliza, insurance, health insurance,
                car insurance, life insurance, home insurance, prima de seguro, prepagada,
                medicina prepagada, plan complementario, prepaga, obra social, isapre, essalud,
                pacifico seguros, rimac, mapfre, allstate, geico, state farm, progressive, axa,
                sura, bolivar seguros, liberty seguros, qualitas, gnp, metlife, plano de saude,
                seguro de viagem, travel insurance, seguro de moto, renter's insurance
                """,
            weak =
                """
                eps, premium, prima
                """,
        )
