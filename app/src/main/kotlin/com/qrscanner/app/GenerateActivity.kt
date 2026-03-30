package com.qrscanner.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.qrscanner.app.databinding.ActivityGenerateBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Hashtable

class GenerateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateBinding
    private var fgColor = Color.BLACK
    private var bgColor = Color.WHITE
    private var qrSize = 300
    private var generatedQR: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupColorPickers()
        setupSlider()
        setupClicks()
    }

    private fun setupColorPickers() {
        binding.cardFgColor.setOnClickListener {
            showColorPicker(true)
        }

        binding.cardBgColor.setOnClickListener {
            showColorPicker(false)
        }
    }

    private fun showColorPicker(isForegroundColor: Boolean) {
        val currentColor = if (isForegroundColor) fgColor else bgColor
        // Простая реализация - можно добавить полноценный ColorPickerDialog
        val colors = arrayOf(
            Color.BLACK,
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.parseColor("#6200EE"),
            Color.parseColor("#03DAC5"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#795548")
        )

        val colorNames = arrayOf("Чёрный", "Синий", "Красный", "Зелёный", "Фиолетовый", "Бирюза", "Оранж", "Коричнев")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(if (isForegroundColor) "Цвет QR" else "Цвет фона")
            .setItems(colorNames) { _, which ->
                if (isForegroundColor) {
                    fgColor = colors[which]
                    binding.cardFgColor.setCardBackgroundColor(fgColor)
                } else {
                    bgColor = colors[which]
                    binding.cardBgColor.setCardBackgroundColor(bgColor)
                }
                if (generatedQR != null) {
                    generateQR()
                }
            }
            .show()
    }

    private fun setupSlider() {
        binding.sizeSlider.addOnChangeListener { _, value, fromUser ->
            qrSize = value.toInt()
            binding.tvSize.text = "Размер: ${qrSize}dp"
            if (generatedQR != null) {
                generateQR()
            }
        }
    }

    private fun setupClicks() {
        binding.btnGenerate.setOnClickListener {
            val content = binding.etContent.text.toString().trim()
            if (content.isEmpty()) {
                binding.etContent.error = "Введите текст"
                return@setOnClickListener
            }
            generateQR()
        }

        binding.btnSave.setOnClickListener {
            saveQR()
        }
    }

    private fun generateQR() {
        val content = binding.etContent.text.toString().trim()
        if (content.isEmpty()) return

        try {
            val hints = Hashtable<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1

            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)
            
            val bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) fgColor else bgColor)
                }
            }

            generatedQR = bitmap
            binding.ivQrPreview.setImageBitmap(bitmap)
            binding.ivQrPreview.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQR() {
        generatedQR?.let { bitmap ->
            try {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val qrDir = File(picturesDir, "QR_Codes")
                qrDir.mkdirs()

                val fileName = "QR_${System.currentTimeMillis()}.png"
                val file = File(qrDir, fileName)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Уведомить медиа-сканер
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                Toast.makeText(this, "QR сохранён: $fileName", Toast.LENGTH_LONG).show()

                // Предложить поделиться
                val shareIntent = android.content.Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться"))

            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
