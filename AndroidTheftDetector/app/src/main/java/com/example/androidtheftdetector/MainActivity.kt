package com.example.androidtheftdetector


import android.Manifest
import android.Manifest.permission.CALL_PHONE
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

const val DEVICE_NAME = "GZ0329"
const val EMERGENCY_CONTACT = "2066613476"

class MainActivity : AppCompatActivity(), BLEControl.Callback {


    // Bluetooth
    private var ble: BLEControl? = null
    private var messages: TextView? = null
    private var rssiAverage:Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
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

    }
    //Function that reads the RSSI value associated with the bluetooth connection between the phone and the Arudino board
    //If you use the RSSI to calculate distance, you may want to record a set of values over a period of time
    //and obtain the average
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


    /**
     * Press button to receive the temperature value form the board
     */
    fun buttTouch(v: View) {
        ble!!.send("readtemp")
        Log.i("BLE", "READ TEMP")
    }

    /**
     * Press button to set the lEDs to color red (see arduino code)
     */
    fun buttRed(v: View) {
        ble!!.send("red")
        Log.i("BLE", "SEND RED")
    }


    /**
     * Writes a line to the messages textbox
     * @param text: the text that you want to write
     */
    private fun writeLine(text: CharSequence) {
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
    private fun alert()
    {
        writeLine("Inside alert" )
        runOnUiThread {


            alertDialog = this?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setNegativeButton("cancel",
                            DialogInterface.OnClickListener { dialog, id ->
                                userCancel = true
                                ble!!.send("cancel\n")
                            })
                }
// Set other dialog properties
                        .setMessage("Do you want to cancel the alarm")
                        .setTitle("Arduino Alert")
// Create the AlertDialog
                builder.create()
            }
            alertDialog?.show()

            writeLine("Showed")
        }
    }

    private fun call()
    {
        writeLine("Inside call" )
        runOnUiThread {
            try {
                val uri: String = "tel:$EMERGENCY_CONTACT"
                val intent = Intent(Intent.ACTION_CALL, Uri.parse(uri))
                if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CALL_PHONE
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions( this, arrayOf(CALL_PHONE), 1)
                } else {
                    startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                writeLine("Activity not found")
            }

        }
    }

    override fun onReceive(ble: BLEControl, rx: BluetoothGattCharacteristic) {

        var rxVal = rx.getStringValue(0)
        writeLine("Received value: " + rxVal )

        if ( rxVal.contains( "DO") )
        {
            writeLine("Door Opened " )
            alert()
        } else if ( rxVal.contains( "BC") )
        {
            alertDialog?.dismiss()
            writeLine("Button Canceled " )
            //alert?.dismiss()
        } else if ( rxVal.contains( "TD") )
        {
            writeLine("Theft Detected " )
            call()
        }
    }

    companion object {
        private val REQUEST_ENABLE_BT = 0
    }
}