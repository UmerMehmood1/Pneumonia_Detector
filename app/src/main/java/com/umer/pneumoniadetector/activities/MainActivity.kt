package com.umer.pneumoniadetector.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.umer.pneumoniadetector.adapters.DeviceAdapter
import com.umer.pneumoniadetector.databinding.ActivityMainBinding
import com.umer.pneumoniadetector.models.Device

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val adapter = DeviceAdapter()

        binding.DeviceRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.DeviceRecyclerView.adapter = adapter

        val listOfDevices = listOf(
            Device("Device 1"),
            Device("Device 2"),
            Device("Device 3"),
            Device("Device 4"),
            Device("Device 5"),
            Device("Device 6"),
        )

        adapter.submitList(listOfDevices)
    }
}