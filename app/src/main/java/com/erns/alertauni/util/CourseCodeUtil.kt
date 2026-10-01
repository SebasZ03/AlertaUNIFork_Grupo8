package com.erns.alertauni.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.security.SecureRandom

object CourseCodeUtil {

    // Caracteres alfanuméricos excluyendo O, 0, I, 1, L para evitar confusión al dictar o leer
    private const val ALLOWED_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    private val random = SecureRandom()

    /**
     * Genera un código alfanumérico aleatorio en mayúsculas de longitud [length].
     */
    fun generateRandomCode(length: Int = 6): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            val randomIndex = random.nextInt(ALLOWED_CHARS.length)
            sb.append(ALLOWED_CHARS[randomIndex])
        }
        return sb.toString()
    }

    /**
     * Convierte una cadena de texto [content] en un [Bitmap] de código QR utilizando ZXing.
     */
    fun generateQrCodeBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        } catch (e: Exception) {
            try {
                // Alternativa manual usando MultiFormatWriter por si falla BarcodeEncoder
                val writer = MultiFormatWriter()
                val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
                bmp
            } catch (ex: Exception) {
                null
            }
        }
    }
}
