package ipca.notas.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import usbserial.driver.UsbSerialDriver
import usbserial.driver.UsbSerialPort
import usbserial.driver.UsbSerialProber
import usbserial.util.SerialInputOutputManager
import java.lang.Exception


class MainActivity : AppCompatActivity() , SerialInputOutputManager.Listener{

    var port: UsbSerialPort? = null

    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (Companion.ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {

                            val manager = getSystemService(Context.USB_SERVICE) as UsbManager
                            val deviceList = manager.getDeviceList()
                            for (d in deviceList ){
                                Log.d(TAG, d.toString())
                            }

                        }
                    } else {
                        Log.d(TAG, "permission denied for device $device")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)


        findViewById<Button>(R.id.button).setOnClickListener {
            val manager = getSystemService(Context.USB_SERVICE) as UsbManager

            val availableDrivers: List<UsbSerialDriver> =
                UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            if (availableDrivers.isEmpty()) {
                return@setOnClickListener
            }

            // Open a connection to the first available driver.

            // Open a connection to the first available driver.
            val driver: UsbSerialDriver = availableDrivers[0]
            val connection = manager.openDevice(driver.getDevice())
                ?: // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                return@setOnClickListener

            port =  driver.getPorts().get(0) // Most devices have just one port (port 0)
            port?.setParameters(
                115200,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            port?.open(connection)


            val  usbIoManager =  SerialInputOutputManager(port, this@MainActivity);
            usbIoManager.start();


        }

        findViewById<Button>(R.id.button2).setOnClickListener {
            val START_OPERATION = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xAA.toByte(),0x0E.toByte())
            Log.d(TAG, port?.driver?.device?.manufacturerName?:"nadas")
            Log.d(TAG, port?.driver?.device?.vendorId.toString())
            Log.d(TAG, port?.driver?.device?.productName?:"nadas")
            Log.d(TAG, port?.driver?.device?.productId.toString())

            port?.write(START_OPERATION, 100)
        }




    }

    override fun onResume() {
        super.onResume()




    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        private const val TAG = "MainActivity"
    }

    override fun onNewData(data: ByteArray?) {
        Log.d(TAG, data.toString())

    }

    override fun onRunError(e: Exception?) {
        Log.e(TAG, e.toString())
    }
}