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

class ModelInterpreter(context: Context) {

    private var interpreter: Interpreter = Interpreter(loadModelFile(context))
    private val inputBuffer: TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 4), DataType.FLOAT32)
    private val outputBuffer: TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)

    // Load the model file from assets
    @Throws(IOException::class)
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd("pneumonia_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Method to set input data as individual parameters and get predictions
    fun predict(mq6: Float, mq9: Float, mq135: Float, tgs2602: Float): Int {
        // Load the input data into the input buffer
        val inputData = floatArrayOf(mq6, mq9, mq135, tgs2602)
        inputBuffer.loadArray(inputData)

        // Run inference
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer.rewind())

        // Retrieve and process the output
        val outputData = outputBuffer.floatArray
        // Assuming output is in the form [prob_negative, prob_positive]
        return if (outputData[1] > outputData[0]) 1 else 0
    }

    // Optional: Close the interpreter when done
    fun close() {
        interpreter.close()
    }
}
