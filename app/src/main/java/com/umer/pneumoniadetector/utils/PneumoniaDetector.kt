package com.umer.pneumoniadetector.utils

import android.content.Context
import com.umer.pneumoniadetector.ml.PneumoniaModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer

class PneumoniaPredictor(context: Context) {

    // Instance of the PneumoniaModel
    private val model: PneumoniaModel = PneumoniaModel.newInstance(context)

    /**
     * Processes input data to make a prediction.
     *
     * @param byteBuffer Input data in ByteBuffer format.
     * @return The model's output as a TensorBuffer.
     */
    fun predict(byteBuffer: ByteBuffer): TensorBuffer {
        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 4), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        return outputs.outputFeature0AsTensorBuffer
    }

    /**
     * Releases model resources.
     */
    fun close() {
        model.close()
    }
}
