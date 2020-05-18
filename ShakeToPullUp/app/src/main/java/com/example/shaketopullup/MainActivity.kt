package com.example.shaketopullup

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.time.Clock
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var pullupDesired: EditText? = null
    private var pullupMade: TextView? = null
    private var status: TextView? = null
    private var stop: Button? = null
    private var freeMode = true
    private var start = false
    private var counter = 0
    private var userDesired = 0


    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSensorG: Sensor

    private var mAccel = 0.0f
    private var mAccelCurr = 0.0f
    private var mAccelLast = 0.0f

    private var yCurr = 0.0f
    private var yLast = 0.0f
    private var timeCurr = 0L
    private var timeLast = 0L
    private var timer: Timer? = null

    private var firstEntry = true
    private var up = false
    private var motion = 0

    val linear_acceleration: Array<Float> = arrayOf(0.0f,0.0f,0.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pullupDesired = findViewById(R.id.editText)
        pullupMade = findViewById(R.id.counter)
        stop = findViewById<Button>(R.id.stop)
        status = findViewById(R.id.status)

        timer = Timer()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            // Sorry, there are no accelerometers on your device.
            null!!
        }
        mSensorG =  (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE))

        mSensorManager.registerListener(this, mSensor, 100000)

        mAccel = 10f;
        mAccelCurr = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            linear_acceleration[0] = event.values[0]
            linear_acceleration[1] = event.values[1]
            linear_acceleration[2] = event.values[2]

            mAccelLast = mAccelCurr;

            mAccelCurr = Math.sqrt(
                Math.pow( linear_acceleration[0].toDouble(), 2.0) + Math.pow(linear_acceleration[1].toDouble(), 2.0) + Math.pow(linear_acceleration[2].toDouble(), 2.0)
            ).toFloat()

            val dif = mAccelCurr - mAccelLast
            mAccel = mAccel * 0.9f + dif;
            if (mAccel > 25) {
                status?.text = "Status: Counting starts!"
                start = true
            }

            if( start )
            {
                if( firstEntry ) {
                    timeLast = System.currentTimeMillis()
                    firstEntry = false
                }
                if( isPullup( linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]))
                {
                    counter++
                    pullupMade?.text = "You made: " + counter.toString()

                    if( pullupDesired?.text.toString().isNotEmpty() )
                    {
                        userDesired = pullupDesired?.text.toString().toInt()

                        if( counter == userDesired )
                        {
                            Toast.makeText(applicationContext, "You made it!!!", Toast.LENGTH_SHORT).show()
                            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 1000 )
                            Thread.sleep(1000)
                            toneGen.release();
                            start = false
                        }
                    }
                }
            }
        } else
        {
            return
        }
    }
    override fun onResume() {
        Log.d("tag","onResume")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("tag","onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }

    private fun isPullup( x:Float, y: Float, z : Float ): Boolean {
        timeCurr = System.currentTimeMillis()

        if( x < 2 && z < 2 && y > 11 && !up )
        {
            motion++
            up = true
        }
        if( x < 2 && z < 2 && y < 8 && y > 6 && up )
        {
            motion++
            if ( ( timeCurr - timeLast ) > 1500 && motion >= 2 )
            {
                motion = 0
                timeLast = timeCurr
                up = false
                return true
            }
        }
        return false
    }

    fun stop(view: View)
    {
        start = false
        status?.text = "Status: Counting stops!"
        pullupMade?.text = "You made: 0"
        counter = 0
        firstEntry = true
    }

}
