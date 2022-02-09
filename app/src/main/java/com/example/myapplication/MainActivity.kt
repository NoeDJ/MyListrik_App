package com.example.myapplication

//import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.bluetooth.BluetoothAdapter
import android.content.Intent
//import android.os.Build
import android.os.Handler
import android.bluetooth.BluetoothDevice
import java.util.*

//-----------------------------------------
//package Android.Arduino.Bluetooth;
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
import android.view.View
//import android.widget.Button
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
//import android.R


//import java.util.Set;
import java.util.UUID
import android.app.Activity
import android.widget.*
import android.widget.AdapterView

import android.widget.TextView

import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.view.inputmethod.EditorInfo

import android.widget.TextView.OnEditorActionListener


//--------------------------


class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT = 1
    var myLabel: TextView? = null
    var myKWHTextbox: EditText? = null
    var myPassTextbox: EditText? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var mmSocket: BluetoothSocket? = null
    var mmDevice: BluetoothDevice? = null
    var mmOutputStream: OutputStream? = null
    var mmInputStream: InputStream? = null
    var workerThread: Thread? = null
    private lateinit var readBuffer: ByteArray
    var readBufferPosition = 0
    var counter = 0

    @Volatile
    var stopWorker = false
    var bluetoothConnectedStatus = false
    var textSpinnerSelected = false

    var textSpinner: String = " "


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openButton = findViewById<View>(R.id.open) as Button
        val sendButton = findViewById<View>(R.id.send) as Button
        val closeButton = findViewById<View>(R.id.close) as Button
        myLabel = findViewById<View>(R.id.label) as TextView
        myPassTextbox = findViewById<View>(R.id.editTextNumberPassword) as EditText
        myPassTextbox!!.isEnabled = false
        myKWHTextbox = findViewById<View>(R.id.entry) as EditText
        myKWHTextbox!!.isEnabled = false

//        if (!myKWHTextbox!!.isEnabled) {
//            myKWHTextbox!!.hint = ""
//        } else {
//            myKWHTextbox!!.hint = "Masukan Nilai KWH"
//        }

        //----------------START OF bluetoothAdapter--------------------

        if (bluetoothAdapter == null) {
            myLabel?.text = "Unit Tidak Support, Tidak Ada Bluetooth"
            // Device doesn't support Bluetooth
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        //----------------end OF bluetoothAdapter--------------------

        //----------------START OF SPINNER--------------------

        val spinner: Spinner = findViewById(R.id.spinner1)
        SpinnerActivity().also { spinner.onItemSelectedListener = it }

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.planets_array,
            R.layout.my_spinner
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        //----------------END OF SPINNER---------------------

        //----------------PASSWORD CHECK--------------------

        findViewById<EditText>(R.id.editTextNumberPassword).setOnEditorActionListener { _, actionId, event ->
            return@setOnEditorActionListener when (4) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage()
                    return@setOnEditorActionListener when (4) {
                        EditorInfo.IME_ACTION_DONE -> {
                            true
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }

        // set on-click listener
        openButton.setOnClickListener {
            if (textSpinnerSelected) {
                if (!bluetoothConnectedStatus) {
                    try {
                        findBT()
                    } catch (ex: IOException) {
                    }
                } else {
                    try {
                        closeBT()
                        openButton.text = "Connect Bluetooth"
                    } catch (ex: IOException) {
                    }
                }
            } else {
                myLabel!!.text = "Pilih Nomer Kamar"
            }
        }

        //Send Button
        sendButton.setOnClickListener {
            try {
                sendData()
            } catch (ex: IOException) {
            }
        }


        //Close button
        closeButton.setOnClickListener {
            try {
                closeBT()
            } catch (ex: IOException) {
            }
        }

    }


    @SuppressLint("SetTextI18n")
    private fun sendMessage() {
        if (myPassTextbox!!.text.toString() == "123456") {
            myKWHTextbox!!.isEnabled = true
            myPassTextbox!!.requestFocus()
        } else {
            myLabel?.text = "PIN Salah!!"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun findBT() {
//        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(
                this@MainActivity,
                "No bluetooth adapter available",
                Toast.LENGTH_SHORT
            )
                .show()
        }
        if (!bluetoothAdapter?.isEnabled!!) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
        }
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter!!.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                if (device.name == textSpinner) {
                    mmDevice = device
                    myLabel?.text = "Connecting to Already Paired Device"
                    openBT()
                    break
                } else {
                    myLabel?.text = "Nomer Kamar Tidak Ditemukan"
                }
            }
        }//        Toast.makeText(this@MainActivity, "Bluetooth Device Found", Toast.LENGTH_SHORT).show()
//        myLabel?.text = "Bluetooth Device Found"
    }

    @SuppressLint("SetTextI18n")
    @Throws(IOException::class)
    fun openBT() {
        val uuid: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID
//        val mmDevice = device.name = "ESP32test"
//        mmDevice.name = textSpinner
        mmSocket = mmDevice?.createRfcommSocketToServiceRecord(uuid)
        mmSocket?.connect()
        mmOutputStream = mmSocket?.outputStream
        mmInputStream = mmSocket?.inputStream
        beginListenForData()
        myLabel?.text = "Bluetooth Opened"
        val connectButton1 = findViewById<View>(R.id.open) as Button
        connectButton1.text = "Connected to kamar " + textSpinner
        myPassTextbox!!.isEnabled = true
        bluetoothConnectedStatus = true
    }


    fun beginListenForData() {
        val handler = Handler()
        val delimiter: Byte = 10 //This is the ASCII code for a newline character
        stopWorker = false
        readBufferPosition = 0
        readBuffer = ByteArray(1024)
        workerThread = Thread {
            while (!Thread.currentThread().isInterrupted && !stopWorker) {
                try {
                    val bytesAvailable = mmInputStream!!.available()
                    if (bytesAvailable > 0) {
                        val packetBytes = ByteArray(bytesAvailable)
                        mmInputStream!!.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(
                                    readBuffer,
                                    0,
                                    encodedBytes,
                                    0,
                                    encodedBytes.size
                                )
                                val data = String(encodedBytes, charset("UTF-8"))
                                readBufferPosition = 0
                                handler.post { myLabel!!.text = data }
                            } else {
                                readBuffer[readBufferPosition++] = b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    stopWorker = true
                }
            }
        }
        workerThread!!.start()
    }


    @SuppressLint("SetTextI18n")
    @Throws(IOException::class)
    fun sendData() {
        var msg = myKWHTextbox?.text.toString()
//        var msg2 = spinner.selectedItem.toString()
        println(msg)
//        println(textSpinner)
        msg += "\n"
        mmOutputStream?.write(msg.toByteArray())
        myLabel!!.text = "Data Sent"
    }

    @SuppressLint("SetTextI18n")
    @Throws(IOException::class)
    fun closeBT() {
        stopWorker = true
        mmOutputStream!!.close()
        mmInputStream!!.close()
        mmSocket!!.close()
        myLabel!!.text = "Bluetooth Closed"
    }


    //----------------START OF SPINNER CLASS--------------------
    inner class SpinnerActivity : Activity(), AdapterView.OnItemSelectedListener {

        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            // An item was selected. You can retrieve the selected item using
//             parent.getItemAtPosition(pos)
            myLabel!!.text = parent.getItemAtPosition(pos).toString()
            textSpinner = parent.getItemAtPosition(pos).toString()
            textSpinnerSelected = true
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // Another interface callback
            textSpinnerSelected = false
        }
    }

    //----------------END OF SPINNER CLASS---------------------

}



