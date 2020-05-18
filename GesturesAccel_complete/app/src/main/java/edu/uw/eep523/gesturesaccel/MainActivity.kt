package edu.uw.eep523.gesturesaccel


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

// Label 0 indicates WING
// Label 1 indicates Unrecognized gesture

   const val TAG = "GestureClassifier"
   const val FLOAT_TYPE_SIZE = 4

   // ********** TO DO ************
    const val MODEL_FILE = "zhangLunjie_2.tflite"
    const val OUTPUT_CLASSES_COUNT = 2
    const val MAX_SAMPLES = 128
// ******************************



class MainActivity : AppCompatActivity() , SensorEventListener {

    private var gestureClassifier = GestureClassifier(this)
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private val linear_acceleration: Array<Float> = arrayOf(0.0f,0.0f,0.0f)
    private val gravity: Array<Float> = arrayOf(0.0f,0.0f,0.0f)

    var nsamples = 0
    val capturedData = FloatArray(MAX_SAMPLES*3) { i -> 0f}
    private var inferenceResult = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize_accel_sensor()

        // Setup digit classifier
        gestureClassifier.initializeInterpreter()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    private fun initialize_accel_sensor(){
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            // Sorry, there are no accelerometers on your device.
            null!!
        }
    }


    //  ``````````` TO DO ````````````````````````
    //1. Capture samples for some time and fill an array with the values
    //2. Stop capturing data when we have enough samples (unregister the listener)
    //3 . Call the model to predict the result
    //4 . Display the prediction result
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER)
            return
        // Isolate the force of gravity with the low-pass filter.
        val alpha: Float = 0.8f

        //1. Capture samples for some time and fill an array with the values
        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0]
        linear_acceleration[1] = event.values[1] * alpha
        linear_acceleration[2] = event.values[2]

        //save data into an array to make inference about the gesture
        //pay attention to the units
        capturedData.set(nsamples + 0, linear_acceleration[0] * 100) // repeat for x, y, z
        capturedData.set(nsamples + 1, linear_acceleration[1] * 100)
        capturedData.set(nsamples + 2, linear_acceleration[2] * 100)

        Log.d(TAG, capturedData.get(nsamples).toString()+"f,"+ capturedData.get(nsamples+1).toString()+"f,"+ capturedData.get(nsamples+2).toString()+"f,...")

        nsamples += 3

        //2. Stop capturing data when we have "enough" samples
        if(nsamples >= MAX_SAMPLES * 3){
            nsamples = 0
            // Unregister listener
            mSensorManager.unregisterListener(this)

            //3. Call the model to get the predicted result
            inferenceResult = gestureClassifier.classify(capturedData)

            // Display the prediction result
            result_view.text = inferenceResult

        }
    }

    fun make_prediction(view: View){

        //Register the accelerometer listener to start recording data
        mSensorManager.registerListener(this, mSensor, 400000)

    }




    fun clear_prediction(view:View){
        result_view.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }


}
