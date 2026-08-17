package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entities.ParagraphNote
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Generates a genuine Microsoft Word (.docx) file as a byte array.
     */
    fun createDocxBytes(notes: List<ParagraphNote>): ByteArray {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        val groupedNotes = notes.groupBy { it.chapterTitle }

        val documentXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
            append("  <w:body>\n")

            // Main Title
            append("    <w:p>\n")
            append("      <w:pPr>\n")
            append("        <w:jc w:val=\"center\"/>\n")
            append("        <w:bidi/>\n")
            append("        <w:spacing w:after=\"120\" w:before=\"200\"/>\n")
            append("      </w:pPr>\n")
            append("      <w:r>\n")
            append("        <w:rPr>\n")
            append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
            append("          <w:b/>\n")
            append("          <w:color w:val=\"1B365D\"/>\n")
            append("          <w:sz w:val=\"48\"/>\n")
            append("          <w:szCs w:val=\"48\"/>\n")
            append("          <w:rtl/>\n")
            append("        </w:rPr>\n")
            append("        <w:t>הערות וביאורים לספר אורות</w:t>\n")
            append("      </w:r>\n")
            append("    </w:p>\n")

            // Subtitle with Date and Total Count
            append("    <w:p>\n")
            append("      <w:pPr>\n")
            append("        <w:jc w:val=\"center\"/>\n")
            append("        <w:bidi/>\n")
            append("        <w:spacing w:after=\"360\"/>\n")
            append("      </w:pPr>\n")
            append("      <w:r>\n")
            append("        <w:rPr>\n")
            append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
            append("          <w:color w:val=\"666666\"/>\n")
            append("          <w:sz w:val=\"22\"/>\n")
            append("          <w:szCs w:val=\"22\"/>\n")
            append("          <w:rtl/>\n")
            append("        </w:rPr>\n")
            append("        <w:t>${escapeXml("הופק בתאריך $dateString • סה\"כ ${notes.size} הערות")}</w:t>\n")
            append("      </w:r>\n")
            append("    </w:p>\n")

            // Empty line
            append("    <w:p><w:pPr><w:bidi/></w:pPr></w:p>\n")

            // Iterate grouped chapters
            for ((chapterTitle, chapterNotes) in groupedNotes) {
                // Chapter Heading
                append("    <w:p>\n")
                append("      <w:pPr>\n")
                append("        <w:pBdr>\n")
                append("          <w:bottom w:val=\"single\" w:sz=\"12\" w:space=\"4\" w:color=\"1B365D\"/>\n")
                append("        </w:pBdr>\n")
                append("        <w:jc w:val=\"right\"/>\n")
                append("        <w:bidi/>\n")
                append("        <w:spacing w:before=\"280\" w:after=\"160\"/>\n")
                append("      </w:pPr>\n")
                append("      <w:r>\n")
                append("        <w:rPr>\n")
                append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
                append("          <w:b/>\n")
                append("          <w:color w:val=\"1B365D\"/>\n")
                append("          <w:sz w:val=\"32\"/>\n")
                append("          <w:szCs w:val=\"32\"/>\n")
                append("          <w:rtl/>\n")
                append("        </w:rPr>\n")
                append("        <w:t>${escapeXml(chapterTitle)}</w:t>\n")
                append("      </w:r>\n")
                append("    </w:p>\n")

                for (note in chapterNotes) {
                    // Paragraph Header
                    append("    <w:p>\n")
                    append("      <w:pPr>\n")
                    append("        <w:jc w:val=\"right\"/>\n")
                    append("        <w:bidi/>\n")
                    append("        <w:spacing w:before=\"160\" w:after=\"60\"/>\n")
                    append("      </w:pPr>\n")
                    append("      <w:r>\n")
                    append("        <w:rPr>\n")
                    append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
                    append("          <w:b/>\n")
                    append("          <w:color w:val=\"8B5A00\"/>\n")
                    append("          <w:sz w:val=\"26\"/>\n")
                    append("          <w:szCs w:val=\"26\"/>\n")
                    append("          <w:rtl/>\n")
                    append("        </w:rPr>\n")
                    append("        <w:t>${escapeXml("אות ${note.paragraphLetter}")}</w:t>\n")
                    append("      </w:r>\n")
                    append("    </w:p>\n")

                    // Snippet / Original Quote if present
                    if (note.snippet.isNotBlank()) {
                        append("    <w:p>\n")
                        append("      <w:pPr>\n")
                        append("        <w:ind w:right=\"360\" w:left=\"360\"/>\n")
                        append("        <w:jc w:val=\"both\"/>\n")
                        append("        <w:bidi/>\n")
                        append("        <w:spacing w:after=\"80\"/>\n")
                        append("      </w:pPr>\n")
                        append("      <w:r>\n")
                        append("        <w:rPr>\n")
                        append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
                        append("          <w:i/>\n")
                        append("          <w:color w:val=\"555555\"/>\n")
                        append("          <w:sz w:val=\"20\"/>\n")
                        append("          <w:szCs w:val=\"20\"/>\n")
                        append("          <w:rtl/>\n")
                        append("        </w:rPr>\n")
                        append("        <w:t>${escapeXml("ציטוט: \"${note.snippet}\"")}</w:t>\n")
                        append("      </w:r>\n")
                        append("    </w:p>\n")
                    }

                    // Note Content Box / Paragraph
                    val lines = note.noteContent.split("\n")
                    for ((lineIdx, line) in lines.withIndex()) {
                        append("    <w:p>\n")
                        append("      <w:pPr>\n")
                        append("        <w:jc w:val=\"right\"/>\n")
                        append("        <w:bidi/>\n")
                        append("        <w:spacing w:after=\"${if (lineIdx == lines.size - 1) 200 else 60}\"/>\n")
                        append("      </w:pPr>\n")
                        if (lineIdx == 0) {
                            append("      <w:r>\n")
                            append("        <w:rPr>\n")
                            append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
                            append("          <w:b/>\n")
                            append("          <w:color w:val=\"1B365D\"/>\n")
                            append("          <w:sz w:val=\"24\"/>\n")
                            append("          <w:szCs w:val=\"24\"/>\n")
                            append("          <w:rtl/>\n")
                            append("        </w:rPr>\n")
                            append("        <w:t>הערה: </w:t>\n")
                            append("      </w:r>\n")
                        }
                        append("      <w:r>\n")
                        append("        <w:rPr>\n")
                        append("          <w:rFonts w:ascii=\"Arial\" w:cs=\"David\" w:hAnsi=\"Arial\"/>\n")
                        append("          <w:color w:val=\"222222\"/>\n")
                        append("          <w:sz w:val=\"24\"/>\n")
                        append("          <w:szCs w:val=\"24\"/>\n")
                        append("          <w:rtl/>\n")
                        append("        </w:rPr>\n")
                        append("        <w:t>${escapeXml(line)}</w:t>\n")
                        append("      </w:r>\n")
                        append("    </w:p>\n")
                    }
                }
            }

            // Section Properties (A4, RTL, Standard margins)
            append("    <w:sectPr>\n")
            append("      <w:bidi/>\n")
            append("      <w:pgSz w:w=\"11906\" w:h=\"16838\"/>\n")
            append("      <w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/>\n")
            append("    </w:sectPr>\n")
            append("  </w:body>\n")
            append("</w:document>\n")
        }

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zip ->
            // [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(rootRelsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // word/document.xml
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        return byteOut.toByteArray()
    }

    /**
     * Writes notes to an OutputStream (e.g. from FileProvider or ContentResolver).
     */
    fun exportToStream(notes: List<ParagraphNote>, outputStream: OutputStream) {
        val docxBytes = createDocxBytes(notes)
        outputStream.write(docxBytes)
        outputStream.flush()
    }

    /**
     * Saves notes to a temporary file in app cache and creates a share/open Intent.
     */
    fun createShareIntent(context: Context, notes: List<ParagraphNote>): Intent {
        val docDir = File(context.cacheDir, "docs").apply { mkdirs() }
        val file = File(docDir, "Orot_Notes.docx")
        FileOutputStream(file).use { fos ->
            exportToStream(notes, fos)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "הערות וביאורים לספר אורות")
            putExtra(Intent.EXTRA_TEXT, "מצורף קובץ וורד (.docx) המכיל את כל ההערות והביאורים שנכתבו בספר אורות.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
