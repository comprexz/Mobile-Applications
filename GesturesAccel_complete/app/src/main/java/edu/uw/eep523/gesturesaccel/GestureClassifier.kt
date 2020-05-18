package edu.uw.eep523.gesturesaccel

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter



//STEPS TO USE THE TFLITE MODEL IN OUR APP
//1. Initialize the interpreter - fun initializeInterpreter()
//2. Prepare the data input in a byte buffer array
//3. Run inference:   interpreter.run(input, output)

class GestureClassifier (private val context: Context) {
    private var interpreter: Interpreter? = null
    var isInitialized = false
    private set

    private var inputNsamples: Int = 0 // will be inferred from TF Lite model
    private var inputNaxis: Int = 0 // will be inferred from TF Lite model
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model


    @Throws(IOException::class)
     fun initializeInterpreter() {
        // Load the TF Lite model from the asset folder
        val assetManager = context.assets
        val model = loadModelFile(assetManager)

        // Initialize TF Lite Interpreter with NNAPI enabled
        val options = Interpreter.Options()
        options.setUseNNAPI(true)
        val interpreter = Interpreter(model, options)

        // Read input shape from model file
        val inputShape = interpreter.getInputTensor(0).shape()
        inputNsamples = inputShape[1]
        inputNaxis = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputNsamples * inputNaxis

        // Finish interpreter initialization
        this.interpreter = interpreter
        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")
    }

     fun classify(data:FloatArray): String {
         initializeInterpreter()
        if (!isInitialized) {
            throw IllegalStateException("TF Lite Interpreter is not initialized yet.")
        }

        val result = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        //Prepare the model input data in a buffer array
         val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
         byteBuffer.order(ByteOrder.nativeOrder())
         for (accel_data in data) {
             byteBuffer.putFloat(accel_data)
         }
       //Run inferece: interpreter.run(input,output)
       interpreter?.run(byteBuffer, result)

        //Return the output to our Main activity to display to the user
        return getOutputString(result[0])

    }

    private fun getOutputString(output: FloatArray): String {
        val maxIndex = output.indices.maxBy { output[it] } ?: -1
        return "Prediction Result: %d\nConfidence: %2f".format(maxIndex, output[maxIndex])
    }

    //Load the model from the asset folder
    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager): ByteBuffer {
        val fileDescriptor = assetManager.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)  }

}