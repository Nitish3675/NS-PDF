package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {

    fun ensureSamplePdfsExist(context: Context) {
        val docsDir = File(context.filesDir, "documents")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        val welcomeFile = File(docsDir, "Welcome_to_NS_PDF.pdf")
        if (!welcomeFile.exists() || welcomeFile.length() == 0L) {
            generateWelcomeGuide(welcomeFile)
        }

        val invoiceFile = File(docsDir, "Sample_Business_Invoice.pdf")
        if (!invoiceFile.exists() || invoiceFile.length() == 0L) {
            generateSampleInvoice(invoiceFile)
        }

        val contractFile = File(docsDir, "Project_Agreement_Contract.pdf")
        if (!contractFile.exists() || contractFile.length() == 0L) {
            generateContractDoc(contractFile)
        }
    }

    private fun generateWelcomeGuide(outputFile: File) {
        val doc = PdfDocument()
        val pageW = 595 // A4 standard width in points
        val pageH = 842 // A4 standard height in points

        // Page 1: Introduction & Highlights
        val page1 = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        val c1 = page1.canvas
        drawPage1(c1, pageW, pageH)
        doc.finishPage(page1)

        // Page 2: Features & Toolkit Overview
        val page2 = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 2).create())
        val c2 = page2.canvas
        drawPage2(c2, pageW, pageH)
        doc.finishPage(page2)

        // Page 3: Privacy, Tips & Security
        val page3 = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 3).create())
        val c3 = page3.canvas
        drawPage3(c3, pageW, pageH)
        doc.finishPage(page3)

        saveDoc(doc, outputFile)
    }

    private fun drawPage1(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Crimson Banner
        paint.color = Color.parseColor("#D32F2F")
        canvas.drawRect(0f, 0f, w.toFloat(), 120f, paint)

        // Banner Title
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("NS PDF – All-in-One PDF Editor", 40f, 60f, paint)

        paint.textSize = 13f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#FFCDD2")
        canvas.drawText("Edit, Create, Scan, Convert, Organize and Protect PDFs Easily", 40f, 85f, paint)

        // Welcome Box
        paint.color = Color.parseColor("#F5F5F7")
        canvas.drawRoundRect(RectF(40f, 150f, (w - 40).toFloat(), 250f), 12f, 12f, paint)

        paint.color = Color.parseColor("#1B1B1F")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Welcome to Your Offline-First PDF Suite", 60f, 190f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#5F6368")
        canvas.drawText("NS PDF is built with privacy in mind. All operations—including reading,", 60f, 215f, paint)
        canvas.drawText("editing, scanning, OCR, merging, and compression—occur locally on your device.", 60f, 233f, paint)

        // Key Capabilities Section
        paint.color = Color.parseColor("#D32F2F")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("What You Can Do With This Application:", 40f, 290f, paint)

        val features = listOf(
            "• Read & Navigate: Smooth page scrolling, high-res zoom, bookmarks, notes & dark mode.",
            "• Edit & Annotate: Draw, highlight, add text boxes, shapes, sticky notes & signatures.",
            "• Create & Scan: Turn physical receipts, notes, or multi-page photos into clean PDFs.",
            "• Convert & Organize: Merge documents, split pages, reorder, rotate, compress, or export images.",
            "• Protect & Secure: Add digital signatures, permanent redaction, watermarks, and headers."
        )

        var yPos = 325f
        paint.color = Color.parseColor("#212121")
        paint.textSize = 12.5f
        paint.isFakeBoldText = false
        for (feat in features) {
            canvas.drawText(feat, 40f, yPos, paint)
            yPos += 30f
        }

        // Test Callout
        paint.color = Color.parseColor("#E8F5E9")
        canvas.drawRoundRect(RectF(40f, yPos + 20f, (w - 40).toFloat(), yPos + 110f), 10f, 10f, paint)

        paint.color = Color.parseColor("#2E7D32")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Tip: Feel free to annotate or sign this document!", 60f, yPos + 55f, paint)
        paint.textSize = 11.5f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#388E3C")
        canvas.drawText("Tap the Edit icon to test drawing, adding text boxes, or signing.", 60f, yPos + 80f, paint)

        // Footer
        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 10f
        canvas.drawText("NS PDF User Guide • Page 1 of 3", 40f, h - 40f, paint)
    }

    private fun drawPage2(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Page Header
        paint.color = Color.parseColor("#D32F2F")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Editing, Annotation & Markup Tools", 40f, 70f, paint)

        paint.color = Color.parseColor("#757575")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Comprehensive tools designed for contracts, school notes, and enterprise forms.", 40f, 95f, paint)

        // Divider
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawLine(40f, 115f, (w - 40).toFloat(), 115f, paint)

        // Table / Grid of tools
        val tools = listOf(
            Pair("Text & Typewriter", "Add custom text anywhere with custom font sizes, colors, and background badges."),
            Pair("Pencil & Highlighter", "Freehand sketching and semi-transparent highlighting for quick document markups."),
            Pair("Shapes & Arrows", "Draw rectangles, circles, lines, and callout arrows with adjustable stroke widths."),
            Pair("Digital Signature", "Draw your handwritten signature, type your name, or insert saved signatures."),
            Pair("Permanent Redaction", "Black out or white out sensitive information with true irreversibility."),
            Pair("Custom Watermarks", "Stamp diagonal or horizontal text watermarks to designate Confidential or Draft status.")
        )

        var y = 150f
        for ((name, desc) in tools) {
            paint.color = Color.parseColor("#D32F2F")
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("• $name", 40f, y, paint)

            paint.color = Color.parseColor("#424242")
            paint.textSize = 11.5f
            paint.isFakeBoldText = false
            canvas.drawText(desc, 55f, y + 20f, paint)

            y += 55f
        }

        // Footer
        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 10f
        canvas.drawText("NS PDF User Guide • Page 2 of 3", 40f, h - 40f, paint)
    }

    private fun drawPage3(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#D32F2F")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Privacy & File Safety Architecture", 40f, 70f, paint)

        paint.color = Color.parseColor("#757575")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Your documents never leave your phone. Zero cloud dependencies.", 40f, 95f, paint)

        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawLine(40f, 115f, (w - 40).toFloat(), 115f, paint)

        // Privacy Guarantee Card
        paint.color = Color.parseColor("#FFF3E0")
        canvas.drawRoundRect(RectF(40f, 140f, (w - 40).toFloat(), 260f), 12f, 12f, paint)

        paint.color = Color.parseColor("#E65100")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("100% Local Processing Guarantee", 60f, 175f, paint)

        paint.color = Color.parseColor("#5D4037")
        paint.textSize = 11.5f
        paint.isFakeBoldText = false
        canvas.drawText("1. Non-Destructive Editing: The app creates new output files (_Edited.pdf)", 60f, 202f, paint)
        canvas.drawText("   to ensure your original master documents remain safe and intact.", 60f, 218f, paint)
        canvas.drawText("2. On-Device OCR: Text recognition uses on-device Google ML Kit models.", 60f, 238f, paint)

        // Support Contact
        paint.color = Color.parseColor("#212121")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Support & Feedback", 40f, 310f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#616161")
        canvas.drawText("Developer: NS PDF Team", 40f, 335f, paint)
        canvas.drawText("Contact: nitish3675kumar@gmail.com", 40f, 355f, paint)
        canvas.drawText("Version: 1.0 Production Edition", 40f, 375f, paint)

        // Footer
        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 10f
        canvas.drawText("NS PDF User Guide • Page 3 of 3", 40f, h - 40f, paint)
    }

    private fun generateSampleInvoice(outputFile: File) {
        val doc = PdfDocument()
        val pageW = 595
        val pageH = 842

        val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        val c = page.canvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Header
        p.color = Color.parseColor("#1A237E")
        p.textSize = 28f
        p.isFakeBoldText = true
        c.drawText("INVOICE", 40f, 70f, p)

        p.color = Color.parseColor("#757575")
        p.textSize = 11f
        p.isFakeBoldText = false
        c.drawText("Invoice Number: INV-2025-0842", 40f, 95f, p)
        c.drawText("Invoice Date: September 15, 2025", 40f, 112f, p)
        c.drawText("Payment Due: Net 30 Days", 40f, 129f, p)

        // Company Details Right
        p.textAlign = Paint.Align.RIGHT
        p.color = Color.parseColor("#212121")
        p.textSize = 14f
        p.isFakeBoldText = true
        c.drawText("Nova Systems & Solutions", (pageW - 40).toFloat(), 70f, p)
        p.textSize = 10f
        p.isFakeBoldText = false
        p.color = Color.parseColor("#616161")
        c.drawText("100 Innovation Boulevard, Suite 400", (pageW - 40).toFloat(), 88f, p)
        c.drawText("Tech District, CA 94107", (pageW - 40).toFloat(), 103f, p)
        c.drawText("billing@novasystems.io", (pageW - 40).toFloat(), 118f, p)
        p.textAlign = Paint.Align.LEFT

        // Divider
        p.color = Color.parseColor("#E0E0E0")
        c.drawLine(40f, 150f, (pageW - 40).toFloat(), 150f, p)

        // Bill To Box
        p.color = Color.parseColor("#1A237E")
        p.textSize = 12f
        p.isFakeBoldText = true
        c.drawText("BILL TO:", 40f, 175f, p)

        p.color = Color.parseColor("#212121")
        p.textSize = 11f
        p.isFakeBoldText = false
        c.drawText("Acme Global Enterprises", 40f, 195f, p)
        c.drawText("Attn: Accounts Payable Department", 40f, 210f, p)
        c.drawText("742 Evergreen Terrace, Springfield, IL 62704", 40f, 225f, p)

        // Table Header
        val tableTop = 260f
        p.color = Color.parseColor("#F5F5F5")
        c.drawRect(40f, tableTop, (pageW - 40).toFloat(), tableTop + 25f, p)

        p.color = Color.parseColor("#37474F")
        p.textSize = 11f
        p.isFakeBoldText = true
        c.drawText("Description", 50f, tableTop + 17f, p)
        c.drawText("Qty", 320f, tableTop + 17f, p)
        c.drawText("Unit Price", 380f, tableTop + 17f, p)
        c.drawText("Total", 480f, tableTop + 17f, p)

        // Table Rows
        val items = listOf(
            Triple("Cloud Architecture & Security Consulting", "1", "$2,400.00"),
            Triple("Mobile PDF Application Prototyping", "1", "$4,850.00"),
            Triple("On-Device ML Kit Integration Module", "1", "$1,650.00"),
            Triple("Quality Assurance & Unit Test Suite", "1", "$950.00")
        )

        var rowY = tableTop + 50f
        p.isFakeBoldText = false
        p.color = Color.parseColor("#212121")
        for ((desc, qty, price) in items) {
            c.drawText(desc, 50f, rowY, p)
            c.drawText(qty, 325f, rowY, p)
            c.drawText(price, 380f, rowY, p)
            c.drawText(price, 480f, rowY, p)
            p.color = Color.parseColor("#ECEFF1")
            c.drawLine(40f, rowY + 12f, (pageW - 40).toFloat(), rowY + 12f, p)
            p.color = Color.parseColor("#212121")
            rowY += 32f
        }

        // Summary Total Box
        val summaryY = rowY + 30f
        p.textAlign = Paint.Align.RIGHT
        p.color = Color.parseColor("#546E7A")
        p.textSize = 11f
        c.drawText("Subtotal:", 440f, summaryY, p)
        c.drawText("Sales Tax (8.25%):", 440f, summaryY + 20f, p)
        p.textSize = 13f
        p.isFakeBoldText = true
        p.color = Color.parseColor("#1A237E")
        c.drawText("Balance Due:", 440f, summaryY + 45f, p)

        p.color = Color.parseColor("#212121")
        p.textSize = 11f
        p.isFakeBoldText = false
        c.drawText("$9,850.00", (pageW - 50).toFloat(), summaryY, p)
        c.drawText("$812.63", (pageW - 50).toFloat(), summaryY + 20f, p)
        p.textSize = 13f
        p.isFakeBoldText = true
        p.color = Color.parseColor("#1A237E")
        c.drawText("$10,662.63", (pageW - 50).toFloat(), summaryY + 45f, p)
        p.textAlign = Paint.Align.LEFT

        // Signature area
        val signY = summaryY + 110f
        p.color = Color.parseColor("#B0BEC5")
        c.drawLine(40f, signY, 220f, signY, p)
        p.color = Color.parseColor("#78909C")
        p.textSize = 10f
        p.isFakeBoldText = false
        c.drawText("Authorized Signature / Date", 40f, signY + 16f, p)

        doc.finishPage(page)
        saveDoc(doc, outputFile)
    }

    private fun generateContractDoc(outputFile: File) {
        val doc = PdfDocument()
        val pageW = 595
        val pageH = 842

        // Page 1
        val p1 = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        val c1 = p1.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#263238")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        c1.drawText("NON-DISCLOSURE & SERVICES AGREEMENT", 40f, 70f, paint)

        paint.textSize = 11f
        paint.color = Color.parseColor("#546E7A")
        paint.isFakeBoldText = false
        c1.drawText("Standard Professional Agreement • Document Ref: NDA-2025-99B", 40f, 92f, paint)

        paint.color = Color.parseColor("#CFD8DC")
        c1.drawLine(40f, 110f, (pageW - 40).toFloat(), 110f, paint)

        val clauses = listOf(
            Pair("1. PARTIES", "This Agreement is entered into by and between Nova Systems Inc. ('Client') and Independent Contractor ('Provider') effective as of the date of signing."),
            Pair("2. PURPOSE", "The Provider agrees to provide software design, technical architecture, and document engineering services as set forth in the attached Statement of Work."),
            Pair("3. CONFIDENTIALITY", "Each party agrees to hold in confidence all proprietary information, trade secrets, intellectual property, and non-public technical specifications."),
            Pair("4. INTELLECTUAL PROPERTY", "All work product, deliverables, code, algorithms, and documentation prepared by Provider under this agreement shall remain the exclusive property of Client."),
            Pair("5. TERM & TERMINATION", "Either party may terminate this agreement with thirty (30) days written notice. Obligations of confidentiality shall survive termination.")
        )

        var y = 150f
        for ((title, body) in clauses) {
            paint.color = Color.parseColor("#1E88E5")
            paint.textSize = 13f
            paint.isFakeBoldText = true
            c1.drawText(title, 40f, y, paint)

            paint.color = Color.parseColor("#37474F")
            paint.textSize = 11f
            paint.isFakeBoldText = false
            c1.drawText(body, 40f, y + 20f, paint)

            y += 65f
        }

        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 10f
        c1.drawText("Contract Document • Page 1 of 2", 40f, pageH - 40f, paint)
        doc.finishPage(p1)

        // Page 2 - Signatures
        val p2 = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 2).create())
        val c2 = p2.canvas

        paint.color = Color.parseColor("#263238")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        c2.drawText("6. EXECUTION AND SIGNATURES", 40f, 70f, paint)

        paint.textSize = 11f
        paint.color = Color.parseColor("#546E7A")
        paint.isFakeBoldText = false
        c2.drawText("IN WITNESS WHEREOF, the parties hereto have executed this Agreement.", 40f, 95f, paint)

        val signLineY = 220f
        paint.color = Color.parseColor("#90A4AE")
        c2.drawLine(40f, signLineY, 240f, signLineY, paint)
        c2.drawLine(300f, signLineY, 500f, signLineY, paint)

        paint.color = Color.parseColor("#37474F")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        c2.drawText("For: Nova Systems Inc.", 40f, signLineY + 20f, paint)
        c2.drawText("For: Independent Contractor", 300f, signLineY + 20f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 10f
        paint.color = Color.parseColor("#78909C")
        c2.drawText("Date: ________________________", 40f, signLineY + 45f, paint)
        c2.drawText("Date: ________________________", 300f, signLineY + 45f, paint)

        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 10f
        c2.drawText("Contract Document • Page 2 of 2", 40f, pageH - 40f, paint)
        doc.finishPage(p2)

        saveDoc(doc, outputFile)
    }

    private fun saveDoc(doc: PdfDocument, file: File) {
        try {
            val fos = FileOutputStream(file)
            doc.writeTo(fos)
            fos.flush()
            fos.close()
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
