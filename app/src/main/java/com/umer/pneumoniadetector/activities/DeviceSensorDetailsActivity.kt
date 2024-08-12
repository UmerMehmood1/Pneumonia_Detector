package com.umer.pneumoniadetector.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.umer.pneumoniadetector.R
import com.umer.pneumoniadetector.databinding.ActivityDeviceSensorDetailsBinding
import com.umer.pneumoniadetector.utils.ModelInterpreter

class DeviceSensorDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceSensorDetailsBinding
    private var modelInterpreter: ModelInterpreter? = null

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
        when {
            intent.getStringExtra("deviceName") == "Device 1" -> {
                binding.mq6Value.text = "100ppm"
                binding.mq9Value.text = "100ppm"
                binding.mq135Value.text = "100ppm"
                binding.tgs2602Value.text = "100ppm"
            }
            intent.getStringExtra("deviceName") == "Device 2" -> {
                binding.mq6Value.text = "50ppm"
                binding.mq9Value.text = "40ppm"
                binding.mq135Value.text = "30ppm"
                binding.tgs2602Value.text = "20ppm"
            }
            intent.getStringExtra("deviceName") == "Device 3" -> {
                binding.mq6Value.text = "200ppm"
                binding.mq9Value.text = "150ppm"
                binding.mq135Value.text = "100ppm"
                binding.tgs2602Value.text = "50ppm"
            }
            intent.getStringExtra("deviceName") == "Device 4" -> {
                binding.mq6Value.text = "150ppm"
                binding.mq9Value.text = "120ppm"
                binding.mq135Value.text = "90ppm"
                binding.tgs2602Value.text = "60ppm"
            }
            intent.getStringExtra("deviceName") == "Device 5" -> {
                binding.mq6Value.text = "80ppm"
                binding.mq9Value.text = "60ppm"
                binding.mq135Value.text = "30ppm"
                binding.tgs2602Value.text = "10ppm"
            }
            intent.getStringExtra("deviceName") == "Device 6" -> {
                binding.mq6Value.text = "0ppm"
                binding.mq9Value.text = "0ppm"
                binding.mq135Value.text = "0ppm"
                binding.tgs2602Value.text = "0ppm"
            }
        }
        modelInterpreter = ModelInterpreter(this)
        setListeners()
    }

    private fun setListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.predictButton.setOnClickListener {
            // Retrieve sensor data from views
            val mq6Value = binding.mq6Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
            val mq9Value = binding.mq9Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
            val mq135Value = binding.mq135Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f
            val tgs2602Value = binding.tgs2602Value.text.toString().replace("ppm", "").toFloatOrNull() ?: 0f

            // Use the sensor data to make predictions
            val predictions = modelInterpreter?.predict(mq6Value, mq9Value, mq135Value, tgs2602Value)
            if (predictions == 1){
                binding.predictionValue.text = "Pneumonia Detected"
                binding.predictionValue.background = ContextCompat.getDrawable(this@DeviceSensorDetailsActivity, R.drawable.red_result_background)
                binding.predictionValue.setTextColor(getColor(R.color.red))
            }else{
                binding.predictionValue.text = "No Pneumonia Detected"
                binding.predictionValue.background = ContextCompat.getDrawable(this@DeviceSensorDetailsActivity, R.drawable.green_result_background)
                binding.predictionValue.setTextColor(getColor(R.color.addButtonColor))
            }
            Log.d("Predictions", "Predictions: $predictions")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        modelInterpreter?.close()
    }
}
