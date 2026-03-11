package com.example.numscan

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var scanLine: View
    private lateinit var scanWindow: View
    private lateinit var numbersContainer: LinearLayout
    private lateinit var tvHint: TextView
    private lateinit var cameraLayout: View
    private lateinit var permissionLayout: View

    private lateinit var cameraExecutor: ExecutorService
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastNumbers = listOf<PhoneNumberResult>()
    private var scanLineAnimator: ObjectAnimator? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera() else showPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        previewView = findViewById(R.id.previewView)
        scanLine = findViewById(R.id.scanLine)
        scanWindow = findViewById(R.id.scanWindow)
        numbersContainer = findViewById(R.id.numbersContainer)
        tvHint = findViewById(R.id.tvHint)
        cameraLayout = findViewById(R.id.cameraLayout)
        permissionLayout = findViewById(R.id.permissionLayout)

        findViewById<FloatingActionButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnGrantPermission).setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            showCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showCamera() {
        cameraLayout.visibility = View.VISIBLE
        permissionLayout.visibility = View.GONE
        startCamera()
        startScanLineAnimation()
    }

    private fun showPermissionDenied() {
        cameraLayout.visibility = View.GONE
        permissionLayout.visibility = View.VISIBLE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ::analyzeImage)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val numbers = PhoneNumberEngine.extractPhoneNumbers(visionText.text)
                if (numbers != lastNumbers) {
                    lastNumbers = numbers
                    runOnUiThread { updateResults(numbers) }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun updateResults(numbers: List<PhoneNumberResult>) {
        numbersContainer.removeAllViews()
        if (numbers.isEmpty()) {
            tvHint.text = getString(R.string.no_numbers_found)
            tvHint.visibility = View.VISIBLE
            return
        }
        tvHint.text = getString(R.string.tap_to_copy)
        tvHint.visibility = View.VISIBLE

        val inflater = layoutInflater
        numbers.forEach { result ->
            val itemView = inflater.inflate(R.layout.item_phone_number, numbersContainer, false)
            itemView.findViewById<TextView>(R.id.tvPhoneNumber).text = result.formattedNumber
            val tvCC = itemView.findViewById<TextView>(R.id.tvCountryCode)
            if (result.countryCode != null) {
                tvCC.text = result.countryCode
                tvCC.visibility = View.VISIBLE
            } else {
                tvCC.visibility = View.GONE
            }
            itemView.findViewById<ImageButton>(R.id.btnCopy).setOnClickListener {
                copyToClipboard(result.formattedNumber)
            }
            itemView.setOnClickListener { copyToClipboard(result.formattedNumber) }
            itemView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
            numbersContainer.addView(itemView)
        }
    }

    private fun copyToClipboard(number: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", number))
        Toast.makeText(this, getString(R.string.copied_to_clipboard, number), Toast.LENGTH_SHORT).show()
        vibrate()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    private fun startScanLineAnimation() {
        scanWindow.post {
            val windowTop = scanWindow.top
            val windowBottom = scanWindow.bottom
            val range = (windowBottom - windowTop).toFloat()

            scanLineAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", -range / 2, range / 2).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanLineAnimator?.cancel()
        cameraExecutor.shutdown()
        recognizer.close()
    }
}
