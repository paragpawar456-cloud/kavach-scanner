package com.kavach.scanner.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** What we could read from a screenshot or photo. Everything runs on the phone (ML Kit on-device models). */
data class ImageContent(val text: String, val qrCodes: List<String>)

object ImageScanner {

    val barcodeOptions: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC, Barcode.FORMAT_DATA_MATRIX, Barcode.FORMAT_PDF417)
        .build()

    suspend fun read(context: Context, uri: Uri): ImageContent {
        val image = InputImage.fromFilePath(context, uri)
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val hindi = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        val barcodes = BarcodeScanning.getClient(barcodeOptions)
        try {
            val latinText = runCatching { latin.process(image).await().text }.getOrDefault("")
            // The Devanagari model also reads Latin letters; use it when the screenshot contains Hindi/Marathi.
            val devText = runCatching { hindi.process(image).await().text }.getOrDefault("")
            val hasDevanagari = devText.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.DEVANAGARI }
            val text = if (hasDevanagari) devText + "\n" + latinText else latinText
            val codes = runCatching { barcodes.process(image).await().mapNotNull { it.rawValue } }.getOrDefault(emptyList())
            return ImageContent(cleanOcr(text), codes)
        } finally {
            latin.close(); hindi.close(); barcodes.close()
        }
    }

    /** OCR often splits long links across lines ("https://sbi-kyc.\nxyz/login"); join obvious breaks. */
    private fun cleanOcr(t: String): String = t
        .replace(Regex("""(https?://\S*[./\-_=?&])\n(\S)"""), "$1$2")
        .replace(Regex("""(\S\.)\n(com|in|net|org|xyz|top|co|info|online|site|shop|live)\b"""), "$1$2")
}
