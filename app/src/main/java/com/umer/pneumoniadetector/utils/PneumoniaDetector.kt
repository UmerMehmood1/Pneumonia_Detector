package com.umer.pneumoniadetector.utils

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class PneumoniaDetector(context: Context) {

    private var interpreter: Interpreter = Interpreter(loadModelFile(context))
    private val inputBuffer: TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 4), DataType.FLOAT32)
    private val outputBuffer: TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)

    // Load the TensorFlow Lite model from assets
    @Throws(IOException::class)
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd("pneumonia_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Method to set input data and get predictions
    fun predict(context: Context, mq6: Float, mq9: Float, mq135: Float, tgs2602: Float): Int {
        // Load the input data into the input buffer
        val inputData = floatArrayOf(mq6, mq9, mq135, tgs2602)
        inputBuffer.loadArray(inputData)

        // Run inference
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer.rewind())

        // Retrieve the output, which is a probability between 0 and 1
        val outputData = outputBuffer.floatArray
        outputData.forEach {
            Toast.makeText(context, "Confidence Score: $it", Toast.LENGTH_SHORT).show();
        }

        // Return 1 if probability > 0.8 (Pneumonia), otherwise 0 (Normal)
        return if (outputData[0] > 0.8) 1 else 0
    }

    // Optional: Close the interpreter when done
    fun close() {
        interpreter.close()
    }
}
