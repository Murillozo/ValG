package com.orcafacil.app.pdf

import android.content.Context
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
        val paint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val bold = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.BLACK }
        val title = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val section = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK }
        val brMoney = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val tecnicoNome = "Valdecy Ribeiro"
        val tecnicoCargo = "Técnico de manutenção de equipamentos medicinais"
        val tecnicoTelefone = "(62) 99904-1921"

        var pageNumber = 1
        var y = 40
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas

        fun header() {
            canvas.drawText("ORÇAMENTO DE SERVIÇOS", 40f, 35f, title)
            canvas.drawText("Projeto: ${projeto?.nome ?: budget.titulo}", 40f, 56f, paint)
            canvas.drawText("Cliente: ${budget.cliente}", 40f, 74f, paint)
            canvas.drawText("Local: ${budget.localObra}", 40f, 92f, paint)
            canvas.drawText("Data: ${budget.data}", 435f, 92f, paint)
            canvas.drawText("Responsável técnico: $tecnicoNome", 40f, 112f, paint)
            canvas.drawText(tecnicoCargo, 40f, 130f, paint)
            canvas.drawText("Telefone: $tecnicoTelefone", 435f, 130f, paint)
            canvas.drawLine(40f, 140f, 560f, 140f, paint)
            y = 162
            canvas.drawText("Observações", 40f, y.toFloat(), section)
            y += 16
            drawWrappedText(
                canvas = canvas,
                text = budget.observacoes.ifBlank { "Sem observações adicionais." },
                x = 40f,
                yStart = y.toFloat(),
                maxChars = 90,
                lineHeight = 15f,
                textPaint = paint
            ).also { endY -> y = endY.toInt() + 8 }
            canvas.drawLine(40f, y.toFloat(), 560f, y.toFloat(), paint)
            y += 20
            canvas.drawText("Descrição dos itens", 40f, y.toFloat(), section)
            y += 18
            canvas.drawText("#", 42f, y.toFloat(), bold)
            canvas.drawText("Descrição", 70f, y.toFloat(), bold)
            canvas.drawText("Qtd", 330f, y.toFloat(), bold)
            canvas.drawText("Un", 380f, y.toFloat(), bold)
            canvas.drawText("Unitário", 420f, y.toFloat(), bold)
            canvas.drawText("Total", 500f, y.toFloat(), bold)
            y += 20
        }

        header()
        itens.forEachIndexed { index, item ->
            if (y > 780) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                header()
            }
            canvas.drawText((index + 1).toString(), 42f, y.toFloat(), paint)
            canvas.drawText(item.descricao.take(35), 70f, y.toFloat(), paint)
            canvas.drawText(item.quantidade.toString(), 330f, y.toFloat(), paint)
            canvas.drawText(item.unidade, 380f, y.toFloat(), paint)
            canvas.drawText(brMoney.format(item.valorUnitario), 420f, y.toFloat(), paint)
            canvas.drawText(brMoney.format(item.valorTotal), 500f, y.toFloat(), paint)
            y += 18
        }

        y += 18
        canvas.drawLine(40f, y.toFloat(), 560f, y.toFloat(), paint)
        y += 20
        canvas.drawText("Mão de obra: ${brMoney.format(budget.maoDeObra)}", 40f, y.toFloat(), paint)
        y += 18
        canvas.drawText("TOTAL GERAL: ${brMoney.format(budget.total)}", 40f, y.toFloat(), bold)
        y += 24
        canvas.drawText("Responsável pelo orçamento:", 40f, y.toFloat(), paint)
        y += 18
        canvas.drawText(tecnicoNome, 40f, y.toFloat(), bold)
        y += 16
        canvas.drawText(tecnicoCargo, 40f, y.toFloat(), paint)
        y += 16
        canvas.drawText("Contato: $tecnicoTelefone", 40f, y.toFloat(), paint)

        doc.finishPage(page)

        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val sanitized = budget.titulo.replace("\\s+".toRegex(), "_")
        val file = File(dir, "ORCAMENTO_${sanitized}_${LocalDate.now()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
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
}
