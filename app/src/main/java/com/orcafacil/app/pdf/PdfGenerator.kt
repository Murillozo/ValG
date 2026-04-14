package com.orcafacil.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.orcafacil.app.data.BudgetEntity
import com.orcafacil.app.data.BudgetItemEntity
import com.orcafacil.app.data.ProjectEntity
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

object PdfGenerator {
    fun gerar(context: Context, budget: BudgetEntity, projeto: ProjectEntity?, itens: List<BudgetItemEntity>): File {
        val doc = PdfDocument()
        val primaryColor = Color.rgb(29, 53, 87)
        val accentColor = Color.rgb(69, 123, 157)
        val lightGray = Color.rgb(236, 240, 243)

        val bodyText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.DKGRAY
        }
        val textMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(100, 100, 100)
        }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 21f
            isFakeBoldText = true
            color = Color.WHITE
        }
        val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = Color.WHITE
        }
        val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            isFakeBoldText = true
            color = primaryColor
        }
        val tableHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            isFakeBoldText = true
            color = Color.WHITE
        }
        val valueLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            isFakeBoldText = true
            color = Color.rgb(90, 90, 90)
        }
        val valueBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            isFakeBoldText = true
            color = primaryColor
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(210, 216, 222)
            strokeWidth = 1f
        }
        val tableHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        val zebraBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lightGray }
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 248, 250) }
        val topBarBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        val brMoney = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val tecnicoNome = "Valdecy Ribeiro"
        val tecnicoCargo = "Técnico de manutenção de equipamentos medicinais"
        val tecnicoTelefone = "(62) 99904-1921"

        var pageNumber = 1
        var y = 40
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas

        fun drawFooter(currentPageNumber: Int) {
            canvas.drawLine(40f, 808f, 555f, 808f, linePaint)
            canvas.drawText("Documento gerado por OrçaFácil", 40f, 825f, textMuted)
            canvas.drawText("Página $currentPageNumber", 505f, 825f, textMuted)
        }

        fun header() {
            canvas.drawRect(0f, 0f, 595f, 102f, topBarBg)
            canvas.drawText("ORÇAMENTO", 40f, 42f, title)
            canvas.drawText("Proposta de serviços técnicos", 40f, 64f, subtitle)
            canvas.drawText("Data: ${budget.data}", 430f, 42f, subtitle)
            canvas.drawText("Ref.: ${projeto?.nome ?: budget.titulo}", 322f, 64f, subtitle)

            canvas.drawRect(40f, 116f, 555f, 198f, cardBg)
            canvas.drawText("Cliente", 52f, 136f, valueLabel)
            canvas.drawText(budget.cliente, 52f, 152f, bodyText)
            canvas.drawText("Local", 52f, 173f, valueLabel)
            canvas.drawText(budget.localObra, 52f, 189f, bodyText)
            canvas.drawText("Responsável técnico", 318f, 136f, valueLabel)
            canvas.drawText(tecnicoNome, 318f, 152f, bodyText)
            canvas.drawText(tecnicoCargo, 318f, 168f, bodyText)
            canvas.drawText(tecnicoTelefone, 318f, 184f, bodyText)

            y = 224
            canvas.drawText("Observações", 40f, y.toFloat(), sectionTitle)
            y += 16
            drawWrappedText(
                canvas = canvas,
                text = budget.observacoes.ifBlank { "Sem observações adicionais." },
                x = 40f,
                yStart = y.toFloat(),
                maxChars = 102,
                lineHeight = 15f,
                textPaint = bodyText
            ).also { endY -> y = endY.toInt() + 8 }
            canvas.drawLine(40f, y.toFloat(), 560f, y.toFloat(), linePaint)
            y += 20
            canvas.drawText("Descrição dos itens", 40f, y.toFloat(), sectionTitle)
            y += 10
            canvas.drawRect(40f, y.toFloat(), 555f, (y + 22).toFloat(), tableHeaderBg)
            y += 15
            canvas.drawText("#", 48f, y.toFloat(), tableHeaderText)
            canvas.drawText("Descrição", 72f, y.toFloat(), tableHeaderText)
            canvas.drawText("Qtd", 348f, y.toFloat(), tableHeaderText)
            canvas.drawText("Un", 390f, y.toFloat(), tableHeaderText)
            canvas.drawText("Unitário", 432f, y.toFloat(), tableHeaderText)
            canvas.drawText("Total", 512f, y.toFloat(), tableHeaderText)
            y += 16
        }

        header()
        itens.forEachIndexed { index, item ->
            if (y > 770) {
                drawFooter(pageNumber)
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                header()
            }

            if (index % 2 == 0) {
                canvas.drawRect(40f, (y - 12).toFloat(), 555f, (y + 8).toFloat(), zebraBg)
            }

            canvas.drawText((index + 1).toString(), 48f, y.toFloat(), bodyText)
            drawSingleLineTruncatedText(canvas, item.descricao, 72f, y.toFloat(), 270f, bodyText)
            canvas.drawText(item.quantidade.toString(), 350f, y.toFloat(), bodyText)
            canvas.drawText(item.unidade, 390f, y.toFloat(), bodyText)
            canvas.drawText(brMoney.format(item.valorUnitario), 432f, y.toFloat(), bodyText)
            canvas.drawText(brMoney.format(item.valorTotal), 512f, y.toFloat(), bodyText)
            y += 20
        }

        y += 8
        canvas.drawLine(40f, y.toFloat(), 560f, y.toFloat(), linePaint)
        y += 24
        canvas.drawText("Mão de obra:", 40f, y.toFloat(), valueLabel)
        canvas.drawText(brMoney.format(budget.maoDeObra), 140f, y.toFloat(), bodyText)
        y += 24
        canvas.drawText("TOTAL GERAL", 40f, y.toFloat(), valueBold)
        canvas.drawText(brMoney.format(budget.total), 160f, y.toFloat(), valueBold)
        y += 30
        canvas.drawText("Responsável pelo orçamento", 40f, y.toFloat(), valueLabel)
        y += 18
        canvas.drawText(tecnicoNome, 40f, y.toFloat(), bodyText)
        y += 16
        canvas.drawText(tecnicoCargo, 40f, y.toFloat(), bodyText)
        y += 16
        canvas.drawText("Contato: $tecnicoTelefone", 40f, y.toFloat(), bodyText)

        drawFooter(pageNumber)

        doc.finishPage(page)

        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val sanitized = budget.titulo.replace("\\s+".toRegex(), "_")
        val file = File(dir, "ORCAMENTO_${sanitized}_${LocalDate.now()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        yStart: Float,
        maxChars: Int,
        lineHeight: Float,
        textPaint: Paint
    ): Float {
        var y = yStart
        text.chunked(maxChars).forEach { line ->
            canvas.drawText(line, x, y, textPaint)
            y += lineHeight
        }
        return y
    }

    private fun drawSingleLineTruncatedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        val ellipsis = "..."
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, x, y, paint)
            return
        }

        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated$ellipsis") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        canvas.drawText("$truncated$ellipsis", x, y, paint)
    }
}
