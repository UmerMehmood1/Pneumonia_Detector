package com.umer.pneumoniadetector.activities

import com.umer.pneumoniadetector.utils.PneumoniaPredictor
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View.VISIBLE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.umer.pneumoniadetector.R
import com.umer.pneumoniadetector.bottomSheets.PermissionBottomSheet
import com.umer.pneumoniadetector.bottomSheets.PermissionListener
import com.umer.pneumoniadetector.databinding.ActivityDeviceSensorDetailsBinding
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class DeviceSensorDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceSensorDetailsBinding
    private lateinit var pneumoniaPredictor: PneumoniaPredictor
    private lateinit var databaseReference: DatabaseReference
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSensorDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        initializeFirebase()
        pneumoniaPredictor = PneumoniaPredictor(this)
        setupListeners()
        registerActivityResults()
        binding.predictButton.isEnabled = false
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().reference
        readDataFromFirebase()
    }

    @SuppressLint("SetTextI18n")
    private fun setupListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.predictButton.setOnClickListener { onPredictButtonClick() }
        binding.shareResult.setOnClickListener { onShareResultClick() }
    }

    private fun onPredictButtonClick() {
        val mq6Value = binding.mq6Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
        val mq9Value = binding.mq9Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
        val mq135Value = binding.mq135Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
        val tgs2602Value = binding.tgs2602Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f

        // Convert sensor values to ByteBuffer
        val byteBuffer = ByteBuffer.allocate(4 * 4).apply {
            asFloatBuffer().put(floatArrayOf(mq6Value, mq9Value, mq135Value, tgs2602Value))
        }

        // Make prediction
        val outputBuffer = pneumoniaPredictor.predict(byteBuffer)
        val prediction = outputBuffer.floatArray[0].toInt() // Assuming the output is a single float representing a binary class

        updatePredictionUI(prediction)
    }

    private fun updatePredictionUI(prediction: Int) {
        if (prediction == 1) {
            binding.predictionValue.text = "Pneumonia Detected"
            binding.predictionValue.setBackgroundResource(R.drawable.red_result_background)
            binding.predictionValue.setTextColor(ContextCompat.getColor(this, R.color.red))
        } else {
            binding.predictionValue.text = "No Pneumonia Detected"
            binding.predictionValue.setBackgroundResource(R.drawable.green_result_background)
            binding.predictionValue.setTextColor(ContextCompat.getColor(this, R.color.addButtonColor))
        }
        binding.shareResult.visibility = VISIBLE
        Log.d("Predictions", "Predictions: $prediction")
    }

    private fun onShareResultClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val filePath = createPdf(
                binding.mq6Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                binding.mq9Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                binding.mq135Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                binding.tgs2602Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                if (binding.predictionValue.text.toString() == "Pneumonia Detected") 1 else 0
            )
            sharePdf(filePath)
        } else {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun registerActivityResults() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onShareResultClick() // Retry sharing the result
            } else {
                showRationaleDialog()
            }
        }

        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, no action needed
            }
        }
    }

    private fun showRationaleDialog() {
        val bottomSheet = PermissionBottomSheet(
            this, true, "Allow write permission to share the result",
            Manifest.permission.WRITE_EXTERNAL_STORAGE, object : PermissionListener {
                override fun onSettingClicked() {
                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    settingsLauncher.launch(settingsIntent)
                }
            }
        )
        bottomSheet.show()
    }

    private fun readDataFromFirebase() {
        databaseReference.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val data = snapshot.value as? Map<*, *>
                Snackbar.make(binding.root, "Data: $data", Snackbar.LENGTH_SHORT).show()
                val mq135Value = data?.get("MQ135")?.toString()?.toFloatOrNull() ?: 0f
                val mq6Value = data?.get("MQ6")?.toString()?.toFloatOrNull() ?: 0f
                val mq9Value = data?.get("MQ9")?.toString()?.toFloatOrNull() ?: 0f
                val tgs2602Value = data?.get("TGS2602")?.toString()?.toFloatOrNull() ?: 0f

                binding.mq135Value.text = "${mq135Value}ppm"
                binding.mq6Value.text = "${mq6Value}ppm"
                binding.mq9Value.text = "${mq9Value}ppm"
                binding.tgs2602Value.text = "${tgs2602Value}ppm"
                binding.predictButton.isEnabled = true

            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Snackbar.make(binding.root, "Failed to read data from Firebase", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    private fun createPdf(mq6: Float, mq9: Float, mq135: Float, tgs2602: Float, predictionResult: Int): String {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { textAlign = Paint.Align.LEFT }

        // Load app icon
        val appIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
            ?: BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
        val iconWidth = 100
        val iconHeight = (appIconBitmap.height.toFloat() / appIconBitmap.width.toFloat() * iconWidth).toInt()
        val iconBitmap = Bitmap.createScaledBitmap(appIconBitmap, iconWidth, iconHeight, false)

        val iconX = (pageInfo.pageWidth - iconWidth) / 2f
        val iconY = 20f
        canvas.drawBitmap(iconBitmap, iconX, iconY, paint)

        paint.apply {
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var yPosition = iconY + iconHeight + 30f

        // Title
        canvas.drawText("Pneumonia Prediction Report", pageInfo.pageWidth / 2f, yPosition, paint)
        yPosition += 40f

        // Sensor Data Heading
        paint.apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Report Data:", 50f, yPosition, paint)
        yPosition += 30f

        // Sensor Data
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("MQ6: $mq6 ppm", 50f, yPosition, paint)
        yPosition += 30f
        canvas.drawText("MQ9: $mq9 ppm", 50f, yPosition, paint)
        yPosition += 30f
        canvas.drawText("MQ135: $mq135 ppm", 50f, yPosition, paint)
        yPosition += 30f
        canvas.drawText("TGS2602: $tgs2602 ppm", 50f, yPosition, paint)
        yPosition += 40f

        // Prediction Result
        val resultText = if (predictionResult == 1) "Pneumonia Detected" else "No Pneumonia Detected"
        paint.apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = if (predictionResult == 1) ContextCompat.getColor(this@DeviceSensorDetailsActivity, R.color.red) else ContextCompat.getColor(this@DeviceSensorDetailsActivity, R.color.addButtonColor)
        }
        canvas.drawText(resultText, pageInfo.pageWidth / 2f , yPosition, paint)
        yPosition += 30f

        pdfDocument.finishPage(page)

        val fileName = "Pneumonia_Report_${System.currentTimeMillis()}.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file.absolutePath
        } catch (e: Exception) {
            Log.e("PDFCreation", "Error creating PDF", e)
            ""
        } finally {
            pdfDocument.close()
        }
    }

    private fun sharePdf(filePath: String) {
        if (filePath.isNotEmpty()) {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/pdf"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
        } else {
            Log.e("SharePDF", "Invalid file path.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pneumoniaPredictor.close()
    }
}
