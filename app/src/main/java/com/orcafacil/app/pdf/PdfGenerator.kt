package com.orcafacil.app.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.orcafacil.app.data.BudgetEntity
import com.orcafacil.app.data.BudgetItemEntity
import com.orcafacil.app.data.ProjectEntity
import java.io.File
import java.time.LocalDate

object PdfGenerator {
    fun gerar(context: Context, budget: BudgetEntity, projeto: ProjectEntity?, itens: List<BudgetItemEntity>): File {
        val doc = PdfDocument()
        val paint = Paint().apply { textSize = 12f }
        val bold = Paint().apply { textSize = 14f; isFakeBoldText = true }

        var pageNumber = 1
        var y = 40
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas

        fun header() {
            canvas.drawText("RELATÓRIO DE ORÇAMENTO", 40f, 30f, bold)
            canvas.drawText("Projeto: ${projeto?.nome ?: budget.titulo}", 40f, 50f, paint)
            canvas.drawText("Cliente: ${budget.cliente}", 40f, 68f, paint)
            canvas.drawText("Local: ${budget.localObra}", 40f, 86f, paint)
            canvas.drawText("Data: ${budget.data}", 430f, 86f, paint)
            canvas.drawLine(40f, 96f, 560f, 96f, paint)
            y = 118
            canvas.drawText("#", 42f, y.toFloat(), bold)
            canvas.drawText("Descrição", 70f, y.toFloat(), bold)
            canvas.drawText("Qtd", 330f, y.toFloat(), bold)
            canvas.drawText("Un", 380f, y.toFloat(), bold)
            canvas.drawText("Unit.", 430f, y.toFloat(), bold)
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
            canvas.drawText("%.2f".format(item.valorUnitario), 430f, y.toFloat(), paint)
            canvas.drawText("%.2f".format(item.valorTotal), 500f, y.toFloat(), paint)
            y += 18
        }

        y += 18
        canvas.drawLine(40f, y.toFloat(), 560f, y.toFloat(), paint)
        y += 20
        canvas.drawText("Mão de obra: R$ %.2f".format(budget.maoDeObra), 40f, y.toFloat(), paint)
        y += 18
        canvas.drawText("TOTAL GERAL: R$ %.2f".format(budget.total), 40f, y.toFloat(), bold)
        y += 24
        canvas.drawText("Observações: ${budget.observacoes}", 40f, y.toFloat(), paint)
        y += 40
        canvas.drawText("Responsável: ___________________________", 40f, y.toFloat(), paint)

        doc.finishPage(page)

        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val sanitized = budget.titulo.replace("\\s+".toRegex(), "_")
        val file = File(dir, "ORCAMENTO_${sanitized}_${LocalDate.now()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
