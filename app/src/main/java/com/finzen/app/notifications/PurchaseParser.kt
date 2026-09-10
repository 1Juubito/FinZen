package com.finzen.app.notifications

object PurchaseParser {

    data class Parsed(
        val amountCents: Long,
        val description: String,
    )

    private val amountRegex = Regex("""R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2})""")

    private val merchantRegex = Regex("""\bem\s+([\p{L}0-9][^\n.;]{1,40})""", RegexOption.IGNORE_CASE)

    private val purchaseKeywords = listOf(
        "compra", "aprovada", "aprovado", "no débito", "no debito",
        "no crédito", "no credito", "débito", "debito", "crédito", "credito",
        "gasto", "você pagou", "voce pagou", "pagamento de", "compra de",
    )

    private val incomeKeywords = listOf(
        "recebeu", "recebido", "recebida", "estorno", "devolv", "depósito", "deposito",
        "entrada", "salário", "salario", "disponível", "disponivel", "saldo",
        "transferência recebida", "transferencia recebida", "pix recebido",
        "fatura", "vence", "vencimento", "limite",
    )

    fun parse(title: String?, text: String?): Parsed? {
        val full = listOfNotNull(title, text)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (full.isBlank()) return null

        val lower = full.lowercase()
        if (incomeKeywords.any { lower.contains(it) }) return null
        if (purchaseKeywords.none { lower.contains(it) }) return null

        val match = amountRegex.find(full) ?: return null
        val cents = brlToCents(match.groupValues[1]) ?: return null
        if (cents <= 0L) return null

        val merchant = extractMerchant(full) ?: "Compra"
        return Parsed(amountCents = cents, description = merchant)
    }

    private fun brlToCents(value: String): Long? =
        value.replace(".", "").replace(",", "").toLongOrNull()

    private fun extractMerchant(text: String): String? {

        val candidate = merchantRegex.findAll(text).lastOrNull()?.groupValues?.get(1)?.trim()
            ?: return null

        return amountRegex.replace(candidate, "").trim().trimEnd(',', '-', ' ').trim()
            .takeIf { it.isNotBlank() }
    }
}
