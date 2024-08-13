package com.umer.pneumoniadetector.activities

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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View.VISIBLE
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.umer.pneumoniadetector.R
import com.umer.pneumoniadetector.bottomSheets.PermissionBottomSheet
import com.umer.pneumoniadetector.bottomSheets.PermissionListener
import com.umer.pneumoniadetector.databinding.ActivityDeviceSensorDetailsBinding
import com.umer.pneumoniadetector.utils.PneumoniaDetector
import java.io.File
import java.io.FileOutputStream

class DeviceSensorDetailsActivity : AppCompatActivity() {
    private val WRITE_STORAGE: Int = 101
    private lateinit var binding: ActivityDeviceSensorDetailsBinding
    private var pneumoniaDetector: PneumoniaDetector? = null
    private lateinit var databaseReference: DatabaseReference
    private lateinit var permissionBottomSheet : PermissionBottomSheet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDeviceSensorDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.title.text = intent.getStringExtra("deviceName")

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().reference

        // Read data from Firebase
        readDataFromFirebase()

        pneumoniaDetector = PneumoniaDetector(this)
        setListeners()
    }

    @SuppressLint("SetTextI18n")
    private fun setListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.predictButton.setOnClickListener {
            // Retrieve sensor data from views
            val mq6Value = binding.mq6Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
            val mq9Value = binding.mq9Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
            val mq135Value =
                binding.mq135Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
            val tgs2602Value =
                binding.tgs2602Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f

            // Use the sensor data to make predictions
            val predictions =
                pneumoniaDetector?.predict(this, mq6Value, mq9Value, mq135Value, tgs2602Value)
            if (predictions == 1) {
                binding.predictionValue.text = "Pneumonia Detected"
                binding.predictionValue.background = ContextCompat.getDrawable(
                    this@DeviceSensorDetailsActivity,
                    R.drawable.red_result_background
                )
                binding.predictionValue.setTextColor(getColor(R.color.red))
            } else {
                binding.predictionValue.text = "No Pneumonia Detected"
                binding.predictionValue.background = ContextCompat.getDrawable(
                    this@DeviceSensorDetailsActivity,
                    R.drawable.green_result_background
                )
                binding.predictionValue.setTextColor(getColor(R.color.addButtonColor))
            }
            binding.shareResult.visibility = VISIBLE
            Log.d("Predictions", "Predictions: $predictions")
        }
        binding.shareResult.setOnClickListener {
            val filePath = createPdf(
                binding.mq6Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                binding.mq9Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                binding.mq135Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                binding.tgs2602Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f,
                if (binding.predictionValue.text.toString() == "Pneumonia Detected") 1 else 0
            )
            sharePdf(filePath)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showRationaleDialog()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_STORAGE)
            }
        }
    }

    private fun showRationaleDialog() {
        permissionBottomSheet = PermissionBottomSheet(this, true, "Allow write permission to share the result", Manifest.permission.WRITE_EXTERNAL_STORAGE, object :
            PermissionListener {
            override fun onSettingClicked() {
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                settingsIntent.setData(uri)
                startActivityForResult(
                    settingsIntent,
                    101,
                    null
                )
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            permissionBottomSheet.dismiss()
        }
    }
    private fun readDataFromFirebase() {
        databaseReference.addValueEventListener(object :
            com.google.firebase.database.ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                // Check if data exists
                if (snapshot.exists()) {
                    val data = snapshot.value as? Map<*, *>
                    val mq135Value = data?.get("MQ135")?.toString()?.toFloatOrNull() ?: 0f
                    val mq6Value = data?.get("MQ6")?.toString()?.toFloatOrNull() ?: 0f
                    val mq9Value = data?.get("MQ9")?.toString()?.toFloatOrNull() ?: 0f
                    val tgs2602Value = data?.get("TGS2602")?.toString()?.toFloatOrNull() ?: 0f
                    Log.d(
                        "FirebaseData",
                        "MQ135: $mq135Value, MQ6: $mq6Value, MQ9: $mq9Value, TGS2602: $tgs2602Value"
                    )
                    // Update the UI with the retrieved data
                    binding.mq135Value.text = "${mq135Value}ppm"
                    binding.mq6Value.text = "${mq6Value}ppm"
                    binding.mq9Value.text = "${mq9Value}ppm"
                    binding.tgs2602Value.text = "${tgs2602Value}ppm"
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.w("FirebaseError", "Failed to read value.", error.toException())
            }
        })
    }

    private fun sharePdf(filePath: String) {
        val file = File(filePath)
        val uri =
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Result"))
    }

    private fun createPdf(
        mq6: Float,
        mq9: Float,
        mq135: Float,
        tgs2602: Float,
        predictionResult: Int
    ): String {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()

        // Load app icon from mipmap, fall back to a different icon if null
        var appIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round) // Try primary icon
        if (appIconBitmap == null) {
            appIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground) // Fall back to secondary icon
        }
        val iconWidth = 100 // Width of the icon
        val iconHeight = (appIconBitmap.height.toFloat() / appIconBitmap.width.toFloat() * iconWidth).toInt()
        val iconBitmap = Bitmap.createScaledBitmap(appIconBitmap, iconWidth, iconHeight, false)

        // Centered horizontally, positioned at top
        val iconX = (pageInfo.pageWidth - iconWidth) / 2f + 25
        val iconY = 20f
        canvas.drawBitmap(iconBitmap, iconX, iconY, paint)

        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Make title bold
        var yPosition = iconY + iconHeight + 30f // Adjusted to make space for the icon and title

        // Title
        canvas.drawText("Pneumonia Prediction Report", pageInfo.pageWidth / 2f+ 25, yPosition, paint)
        yPosition += 40f

        // Sensor Data Heading
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Make heading bold
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Sensor Data:", 50f, yPosition, paint)
        yPosition += 30f

        // Sensor Data
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) // Regular text
        canvas.drawText("MQ6: $mq6 ppm", 50f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("MQ9: $mq9 ppm", 50f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("MQ135: $mq135 ppm", 50f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("TGS2602: $tgs2602 ppm", 50f, yPosition, paint)
        yPosition += 20f

        // Prediction Result
        yPosition += 20f
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Make result bold
        canvas.drawText(
            "Prediction Result: ${if (predictionResult == 1) "Pneumonia Detected" else "No Pneumonia"}",
            50f,
            yPosition,
            paint
        )

        // Watermark
        paint.textSize = 12f
        paint.alpha = 100 // Set transparency for watermark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) // Italicize watermark text
        paint.textAlign = Paint.Align.CENTER
        yPosition = pageInfo.pageHeight - 30f
        canvas.drawText("Generated by Pneumonia Detector App", pageInfo.pageWidth / 2f + 25, yPosition, paint)

        pdfDocument.finishPage(page)

        // Save the document
        val filePath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}/PneumoniaPredictionReport.pdf"
        val file = File(filePath)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return filePath
    }





    override fun onDestroy() {
        super.onDestroy()
        pneumoniaDetector?.close()
    }
}
