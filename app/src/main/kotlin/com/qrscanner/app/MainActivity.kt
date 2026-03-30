package com.qrscanner.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.qrscanner.app.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var isFlashOn = false
    
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { decodeQRFromImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupClicks()
    }

    private fun setupClicks() {
        binding.btnGenerate.setOnClickListener {
            startActivity(Intent(this, GenerateActivity::class.java))
        }

        binding.btnGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvResult.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("QR Result", text))
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val text = binding.tvResult.text.toString()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }
    }

    private fun toggleFlash() {
        camera?.let {
            isFlashOn = !isFlashOn
            it.cameraControl.enableTorch(isFlashOn)
            updateFlashIcon()
        }
    }

    private fun updateFlashIcon() {
        binding.btnFlash.setImageResource(
            if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        BarcodeScanning.getClient().process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes.first()
                    barcode.rawValue?.let { value ->
                        runOnUiThread {
                            showResult(value)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun decodeQRFromImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val image = InputImage.fromBitmap(bitmap, 0)

            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let { value ->
                            showResult(value)
                        }
                    } else {
                        Toast.makeText(this, "QR код не найден", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResult(result: String) {
        binding.tvResult.text = result
        binding.tvResult.visibility = View.VISIBLE
        binding.btnCopy.visibility = View.VISIBLE
        binding.btnShare.visibility = View.VISIBLE

        // Остановить сканирование после успеха
        imageAnalysis?.clearAnalyzer()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
            // Возобновить анализ если результат скрыт
            if (binding.tvResult.visibility != View.VISIBLE) {
                imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        super.onPause()
        camera?.cameraControl?.enableTorch(false)
        isFlashOn = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
