package com.kurobello.tallybook.core.data.entry

internal val incomeKeywords: List<CategoryKeywordEntry> =
    keywords(
        "cat_ingresos",
        strong =
            """
            ingreso, income, renda, dinero recibido, pago recibido, payment received,
            transferencia recibida, ingreso extra, extra income, side income, renda extra,
            propina recibida, dividendos, dividends, intereses ganados, interest earned,
            rendimiento, yield, jubilacion, retirement pay, aposentadoria,
            subsidio, beca, scholarship, bolsa familia, apoyo economico, auxilio, ganancias,
            profit, utilidades, reparto de utilidades, ingresos adicionales
            """,
        weak =
            """
            pension, premio, deposito, abono, cobro
            """,
    ) +
        keywords(
            "cat_sueldo",
            strong =
                """
                sueldo, salario, salary, nomina, payroll, paycheck, wage, quincena,
                pago mensual, pago quincenal, pago de nomina, salario mensual, bono, bonus,
                bonificacion, prima de servicios, aguinaldo, gratificacion, decimo tercer sueldo,
                decimo tercero, pago de sueldo, holerite, contracheque, horas extras, overtime,
                empleo, employer, empleador, pago de la empresa, liquidacion,
                finiquito, severance, pago del trabajo, take home pay, net pay, salario neto,
                pago de salario
                """,
            weak =
                """
                paga, empresa
                """,
        ) +
        keywords(
            "cat_freelance",
            strong =
                """
                freelance, freelancer, freela, trabajo independiente, independiente,
                consultoria, consulting, consultor, honorarios, pago de cliente, client payment,
                gig, side hustle, upwork, fiverr, workana, toptal, payoneer, deel, chamba,
                changa, bico, gig economy, ganancias de uber, ganancias de rappi,
                ganancias de didi, trabajo extra, trabajito, desarrollo web, web development,
                diseño grafico, traduccion, translation, redaccion, copywriting, proyecto
                freelance, factura freelance, honorarios profesionales
                """,
            weak =
                """
                cliente, proyecto, invoice, diseño, paypal, wise
                """,
        ) +
        keywords(
            "cat_ventas",
            strong =
                """
                venta, venta de garage, garage sale, yard sale, venta de usados,
                facebook marketplace, olx, vinted, wallapop, depop, poshmark, shopify,
                tienda online, ingresos por ventas, venda, revenda, negocio, business income, ingreso del negocio, ventas del dia, daily sales,
                sales revenue, comision por ventas, reventa, resale,
                segunda mano, second hand, venta de carro, venta de moto, venta de celular,
                venta de ropa, venta de muebles, dropshipping, emprendimiento, caja del dia
                """,
            weak =
                """
                marketplace, etsy, ebay, mercadolibre, usados, sale
                """,
        ) +
        keywords(
            "cat_regalo",
            strong =
                """
                regalo recibido, gift received, dinero de regalo, cash gift, mesada, allowance,
                herencia, inheritance, legado, birthday money, dinero de cumpleaños,
                pocket money, regalo en efectivo, presente recibido, dinheiro de presente
                """,
            weak =
                """
                regalo, gift, cumpleaños, donacion, premio, navidad
                """,
        ) +
        keywords(
            "cat_reembolso",
            strong =
                """
                reembolso, reintegro, devolucion, refund, reimbursement, reembolso de gastos,
                reintegro de gastos, gastos reembolsados, cashback, cash back, devolucao,
                estorno, ressarcimento, nota credito, credit note, chargeback, contracargo,
                money back, devolucion de impuestos, tax refund, devolucion iva,
                saldo a favor, reintegro de salud, reembolso eps, reintegro seguro,
                prestamo devuelto, devolucion de prestamo, loan repayment, reembolso de viaje
                """,
        )
