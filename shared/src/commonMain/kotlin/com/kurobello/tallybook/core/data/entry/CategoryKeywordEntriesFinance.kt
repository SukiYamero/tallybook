package com.kurobello.tallybook.core.data.entry

internal val financeKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_finanzas",
        strong =
            """
            finanzas, finance, financial, gastos financieros, cuenta bancaria, bank account,
            conta bancaria, financas, finanzas personales, personal finance
            """,
        weak =
            """
            banco, bank
            """,
    ) +
        keywords(
            "cat_impuestos",
            strong =
                """
                impuesto, tax, taxes, predial, iva, isr, dian, sunat, afip, arba, hacienda,
                declaracion de renta, declaracion de impuestos, impuesto de renta,
                impuesto predial, impuesto vehicular, tenencia vehicular, tenencia,
                permiso de circulacion, contribucion, tasa municipal, irs, hmrc, tax return,
                property tax, income tax, sales tax, vat, gravamen, iptu, ipva, irpf, darf,
                timbre, estampillas, contador, accountant, contabilidad, contabilidade,
                impuesto de timbre, impuestos municipales
                """,
            weak =
                """
                multa, patente, monotributo, seguridad social
                """,
        ) +
        keywords(
            "cat_comisiones",
            strong =
                """
                comision, cuota de manejo, comision bancaria, bank fee, fee, service fee,
                monthly fee, annual fee, cuota anual, mantenimiento de cuenta, cargo bancario,
                cargo por servicio, cargo, gmf, comision por retiro, retiro cajero,
                comision cajero, atm fee, atm, comision de transferencia, wire fee, swift,
                tarifa bancaria, tasa de cambio, casa de cambio, currency exchange,
                foreign exchange fee, fx fee, cambio de divisas, paypal fee, stripe fee,
                comision de plataforma, western union, remittance, tarifa de servicio, taxa,
                anuidade, tarifa de manutencao, iof, cobro bancario, overdraft fee,
                sobregiro, comision por sobregiro
                """,
            weak =
                """
                cajero, transferencia, tarifa, remesa, wise, mercado pago, interes, banco
                """,
        ) +
        keywords(
            "cat_ahorro",
            strong =
                """
                ahorro, savings, ahorrar, fondo de emergencia, emergency fund, alcancia,
                hucha, cdt, plazo fijo, deposito a plazo, inversion, investment, invest, acciones,
                stocks, etf, fondo de inversion, fondo mutuo, fci, pension voluntaria, roth, cesantias, fondo de pensiones, tesouro direto, poupanca, cdb, fii,
                bitcoin, crypto, cripto, criptomonedas, btc, binance, coinbase, cuenta de ahorro,
                meta de ahorro, savings goal, guardado, apartado, apartar, separar plata,
                guardar plata, aporte a inversion, reserva de emergencia
                """,
            weak =
                """
                retiro, reserva, afp
                """,
        ) +
        keywords(
            "cat_deuda",
            strong =
                """
                deuda, debt, prestamo, loan, credito, pago de tarjeta, pago tarjeta de credito,
                tarjeta de credito, cuota del prestamo, cuota del credito, abono a deuda,
                libranza, financiacion, financiamento, emprestimo, fatura do cartao,
                cartao de credito, pagamento de fatura, amex,
                american express, mora, pago minimo, minimum payment, credit card bill,
                card payment, payoff, student loan, car loan, prestamo personal,
                credito de consumo, gota a gota, prestamista, usura, fiado, adelanto,
                adiantamento, pagare, cobranza, refinanciacion, debt consolidation,
                tarjeta rotativa, rotativo, credit card
                """,
            weak =
                """
                tarjeta, cuota, abono, interes, visa, mastercard, parcela, banco
                """,
        ) +
        keywords(
            "cat_caja_menor",
            strong =
                """
                caja menor, petty cash, caixinha, gastos menores, gastos hormiga, gastos varios,
                plata de bolsillo, pocket expenses, fondo de caja, cash float, cash on hand,
                dinero en efectivo, imprevistos, efectivo para imprevistos,
                gastos pequeños, small expenses, cash expenses, gastos en efectivo,
                dinheiro vivo, trocados
                """,
            weak =
                """
                efectivo, cash, sencillo, menudo, vuelto, monedas, billetera, wallet, propina,
                retiro
                """,
        )
