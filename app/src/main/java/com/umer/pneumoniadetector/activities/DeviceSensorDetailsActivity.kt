package com.umer.pneumoniadetector.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*
import com.umer.pneumoniadetector.R
import com.umer.pneumoniadetector.bottomSheets.PermissionBottomSheet
import com.umer.pneumoniadetector.bottomSheets.PermissionListener
import com.umer.pneumoniadetector.databinding.ActivityDeviceSensorDetailsBinding
import com.umer.pneumoniadetector.listeners.OnInternetStateChanged
import com.umer.pneumoniadetector.recievers.NetworkChangeReceiver
import com.umer.pneumoniadetector.utils.PneumoniaPredictor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat


class DeviceSensorDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceSensorDetailsBinding
    private lateinit var pneumoniaPredictor: PneumoniaPredictor
    private lateinit var databaseReference: DatabaseReference
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private val predictions = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSensorDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        binding.progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED))

        initializeFirebase()
        pneumoniaPredictor = PneumoniaPredictor(this)
        setupNetworkChangeReceiver()
        setupListeners()
        registerActivityResults()
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
        Log.d("Firebase", "Firebase initialized: $databaseReference")
        readDataFromFirebase()
    }

    @SuppressLint("SetTextI18n")
    private fun setupNetworkChangeReceiver() {
        networkChangeReceiver = NetworkChangeReceiver(object : OnInternetStateChanged {
            override fun onConnected() {
                binding.internetState.animate().alpha(0f).setDuration(300).start()
                binding.internetState.text = "Internet Connected"
                binding.internetState.setBackgroundColor(getColor(R.color.addButtonColor))
                readDataFromFirebase()
                binding.internetState.visibility = GONE
            }

            override fun onDisconnected() {
                binding.internetState.visibility = VISIBLE
                binding.internetState.animate().alpha(1f).setDuration(300).start()
                binding.internetState.text = "Connect internet to get live data"
                binding.internetState.setBackgroundColor(getColor(R.color.red))
            }
        })
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)
    }

    private fun setupListeners() {
        binding.predictButton.setOnClickListener { onPredictButtonClick() }
        binding.shareResult.setOnClickListener { onShareResultClick() }
    }

    private fun onPredictButtonClick() {
        // Disable the prediction button and show the progress bar
        binding.predictButton.isEnabled = false
        binding.progressBar.visibility = VISIBLE

        // Clear previous predictions
        predictions.clear()

        // Start a coroutine to collect predictions
        CoroutineScope(Dispatchers.Main).launch {
            repeat(10) {
                delay(1000) // 1-second delay between each prediction

                // Fetch sensor values
                val mq6Value = binding.mq6Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
                val mq9Value = binding.mq9Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
                val mq135Value = binding.mq135Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
                val tgs2602Value = binding.tgs2602Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f

                // Prepare input for prediction
                val byteBuffer = ByteBuffer.allocate(4 * 4).apply {
                    asFloatBuffer().put(floatArrayOf(mq6Value, mq9Value, mq135Value, tgs2602Value))
                }

                // Get prediction from the predictor
                val outputBuffer = pneumoniaPredictor.predict(byteBuffer)
                val prediction = outputBuffer.floatArray[0].toInt()

                // Add prediction to the list
                predictions.add(prediction)
            }

            // Once all predictions are collected, update the UI
            updatePredictionUIBasedOnMajority(predictions)

            // Enable the prediction button and hide the progress bar
            binding.predictButton.isEnabled = true
            binding.progressBar.visibility = GONE

            // Show the share result button
            binding.shareResult.visibility = VISIBLE
        }
    }

    private fun updatePredictionUIBasedOnMajority(predictions: List<Int>) {
        val countOnes = predictions.count { it == 1 }
        val countZeros = predictions.count { it == 0 }

        val predictionToShow = if (countOnes > countZeros) 1 else 0

        updatePredictionUI(predictionToShow)
    }

    @SuppressLint("SetTextI18n")
    private fun updatePredictionUI(prediction: Int) {
        when (prediction) {
            1 -> {
                binding.predictionValue.text = "Pneumonia Detected"
                binding.predictionValue.setBackgroundResource(R.drawable.red_result_background)
                binding.predictionValue.setTextColor(ContextCompat.getColor(this, R.color.red))
            }
            else -> {
                binding.predictionValue.text = "No Pneumonia Detected"
                binding.predictionValue.setBackgroundResource(R.drawable.green_result_background)
                binding.predictionValue.setTextColor(ContextCompat.getColor(this, R.color.addButtonColor))
            }
        }
        binding.shareResult.visibility = VISIBLE
        Log.d("Predictions", "Majority Prediction: $prediction")
    }

    private fun onShareResultClick() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
                onShareResultClick()
            } else {
                showRationaleDialog()
            }
        }

        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    }

    private fun showRationaleDialog() {
        val bottomSheet = PermissionBottomSheet(
            this, true, "Storage Permission Required",
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
        Log.d("Firebase", "Reading data from Firebase")
        databaseReference.addValueEventListener(object : ValueEventListener {

            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as? Map<*, *>
                Log.d("Firebase", "Snapshot: ${data.toString()}")
                if (data != null) {
                    val mq6 = data["mq6"]?.toString() ?: "0.0 ppm"
                    val mq9 = data["mq9"]?.toString() ?: "0.0 ppm"
                    val mq135 = data["mq135"]?.toString() ?: "0.0 ppm"
                    val tgs2602 = data["tgs2602"]?.toString() ?: "0.0 ppm"
                    val lastUpdated = "Last Updated: " +SimpleDateFormat("hh:mm a").format(System.currentTimeMillis())

                    binding.mq6Value.text = mq6
                    binding.mq9Value.text = mq9
                    binding.mq135Value.text = mq135
                    binding.tgs2602Value.text = tgs2602

                    binding.mq6lastUpdated.text = lastUpdated
                    binding.mq9lastUpdated.text = lastUpdated
                    binding.mq135lastUpdated.text = lastUpdated
                    binding.tgs2602lastUpdated.text = lastUpdated
                } else {
                    Log.d("Firebase", "No data found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error reading data: ${error.message}")
            }
        })
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
        val paint = Paint().apply { textAlign = Paint.Align.LEFT }

        // Load app icon
        val appIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
            ?: BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
        val iconWidth = 100
        val iconHeight =
            (appIconBitmap.height.toFloat() / appIconBitmap.width.toFloat() * iconWidth).toInt()
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
        val resultText =
            if (predictionResult == 1) "Pneumonia Detected" else "No Pneumonia Detected"
        paint.apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = if (predictionResult == 1) ContextCompat.getColor(
                this@DeviceSensorDetailsActivity,
                R.color.red
            ) else ContextCompat.getColor(this@DeviceSensorDetailsActivity, R.color.addButtonColor)
        }
        canvas.drawText(resultText, pageInfo.pageWidth / 2f, yPosition, paint)
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
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share PDF")
        val resInfoList =
            this.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            this.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        startActivity(chooser)
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
    }
}
