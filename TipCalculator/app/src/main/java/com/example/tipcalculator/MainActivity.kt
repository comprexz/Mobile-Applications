package com.example.tipcalculator

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
/*
    var button10: Button = findViewById<Button>(R.id.tip10)
    var button15: Button = findViewById<Button>(R.id.tip15)
    var button20: Button = findViewById<Button>(R.id.tip20)

    var price: TextView = findViewById<TextView>(R.id.price)

*/
    private var button10: Button? = null
    private var button15: Button? = null
    private var button20: Button? = null
    private var spend: EditText? = null
    private var price: TextView? = null
    var userSpend = 0.00
    var finalPrice = 0.00

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button10 = findViewById<Button>(R.id.tip10)
        button15 = findViewById<Button>(R.id.tip15)
        button20 = findViewById<Button>(R.id.tip20)
        spend = findViewById(R.id.editText)
        price = findViewById(R.id.price)
    }

    fun tip10(view: View){
        if (spend?.text.toString().isNotEmpty()) {
            userSpend = spend?.text.toString().toDouble()
            finalPrice = 1.1 * userSpend
            price?.text = finalPrice.toString()
        } else
        {
            finalPrice = 0.00
            price?.text = finalPrice.toString()
        }
    }

    fun tip15(view: View){
        if (spend?.text.toString().isNotEmpty()) {
            userSpend = spend?.text.toString().toDouble()
            finalPrice = 1.15 * userSpend
            price?.text = finalPrice.toString()
        } else
        {
            finalPrice = 0.00
            price?.text = finalPrice.toString()
        }
    }
    fun tip20(view: View){
        if (spend?.text.toString().isNotEmpty()) {
            userSpend = spend?.text.toString().toDouble()
            finalPrice = 1.2 * userSpend
            price?.text = finalPrice.toString()
        } else
        {
            finalPrice = 0.00
            price?.text = finalPrice.toString()
        }
    }
}
