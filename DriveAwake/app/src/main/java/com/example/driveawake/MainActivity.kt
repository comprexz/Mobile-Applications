package com.example.driveawake

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

const val DEVICE_NAME = "GZ0329"
const val EMERGENCY_CONTACT = "2066613476"

class MainActivity : AppCompatActivity(), BLEControl.Callback  {

    var CameraOn = false
    // Bluetooth
    var ble: BLEControl? = null
    var messages: TextView? = null
    private var rssiAverage:Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val adapter: BluetoothAdapter?
        adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (!adapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            }        }

        // Get Bluetooth
        messages = findViewById(R.id.bluetoothText)
        messages!!.movementMethod = ScrollingMovementMethod()
        ble = BLEControl(applicationContext, DEVICE_NAME)

        // Check permissions
        ActivityCompat.requestPermissions(this,
            arrayOf( Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)


        butStart.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

                // Don't have permission to draw over other apps yet - ask user to give permission
                val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivityForResult(settingsIntent, CODE_PERM_SYSTEM_ALERT_WINDOW)
                return@setOnClickListener
            }

            if (!isServiceRunning(this, CameraService::class.java)) {
                writeLine("Service starts with preview")
                notifyService(CameraService.ACTION_START)
            } else
            {
                writeLine("Stop the ongoing service first")
            }
        }

        butStartNoCam.setOnClickListener {

            if (!isServiceRunning(this, CameraService::class.java)) {
                writeLine("Service starts without preview")
                notifyService(CameraService.ACTION_START_NO_CAM)
            } else
            {
                writeLine("Stop the ongoing service first")
            }
        }

        butStop.setOnClickListener {
            stopService(Intent(this, CameraService::class.java))
            writeLine("Service stopped")
        }

        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // We don't have camera permission yet. Request it from the user.
            ActivityCompat.requestPermissions(this, arrayOf(permission), CODE_PERM_CAMERA)
        }

        instance = this
    }

    override fun onRSSIread(uart:BLEControl,rssi:Int){
        rssiAverage = rssi.toDouble()
        writeLine("RSSI $rssiAverage")
    }
    fun getRSSI (v:View){
        ble!!.getRSSI()
    }

    fun clearText (v:View){
        messages!!.text=""

    }

    override fun onResume() {
        super.onResume()
        //updateButtons(false)
        ble!!.registerCallback(this)
    }

    override fun onStop() {
        super.onStop()
        ble!!.unregisterCallback(this)
        ble!!.disconnect()
    }

    fun connect(v: View) {
        startScan()
    }

    private fun startScan() {
        writeLine("Scanning for devices ...")
        ble!!.connectFirstAvailable()
    }

     fun writeLine(text: CharSequence) {
        runOnUiThread {
            messages!!.append(text)
            messages!!.append("\n")
        }
    }

    /**
     * Called when a UART device is discovered (after calling startScan)
     * @param device: the BLE device
     */
    override fun onDeviceFound(device: BluetoothDevice) {
        writeLine("Found device : " + device.name)
        writeLine("Waiting for a connection ...")
    }

    /**
     * Prints the devices information
     */
    override fun onDeviceInfoAvailable() {
        writeLine(ble!!.deviceInfo)
    }

    /**
     * Called when UART device is connected and ready to send/receive data
     * @param ble: the BLE UART object
     */
    override fun onConnected(ble: BLEControl) {
        writeLine("Connected!")

    }

    /**
     * Called when some error occurred which prevented UART connection from completing
     * @param ble: the BLE UART object
     */
    override fun onConnectFailed(ble: BLEControl) {
        writeLine("Error connecting to device!")
    }

    /**
     * Called when the UART device disconnected
     * @param ble: the BLE UART object
     */
    override fun onDisconnected(ble: BLEControl) {
        writeLine("Disconnected!")
    }

    var alertDialog: AlertDialog? = null
    var userCancel = false
    fun eyeClosed()
    {
        writeLine("Eyes closed!" )
        runOnUiThread {

            ble!!.send("EC\n")
            alertDialog = this?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setNegativeButton("ok",
                        DialogInterface.OnClickListener { dialog, id ->
                            userCancel = true
                            ble!!.send("Cancel\n")
                        })
                }
// Set other dialog properties
                    .setMessage("Time to take a break!")
                    .setTitle("Alert")
// Create the AlertDialog
                builder.create()
            }
            if ( !this.isFinishing ) {
                alertDialog?.show()
            }

            //writeLine("Showed")
        }
    }

    override fun onReceive(ble: BLEControl, rx: BluetoothGattCharacteristic) {

        var rxVal = rx.getStringValue(0)
        //writeLine("Received value: " + rxVal )

        if ( rxVal.contains( "BC") )
        {
            alertDialog?.dismiss()
            writeLine("Button Canceled " )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CODE_PERM_CAMERA -> {
                if (grantResults?.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "No Camera Permission!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun notifyService(action: String) {

        val intent = Intent(this, CameraService::class.java)
        intent.action = action
        startService(intent)
    }

    // Helper function to check whether the service is running or not.
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            for (service in manager!!.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    companion object {
        //var ble: BLEControl? = null
        var instance: MainActivity? = null

        val CODE_PERM_SYSTEM_ALERT_WINDOW = 6111
        val CODE_PERM_CAMERA = 6112

        val REQUEST_ENABLE_BT = 0
    }

}
