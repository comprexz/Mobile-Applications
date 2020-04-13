package com.example.tictactoe

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_game.*


class Game : AppCompatActivity() {

    private var buttons : Array<Button>? = null
    private var p1Turn = true
    private var p1Icon: String? = null
    private var p2Icon: String? = null
    private var winner: TextView? = null
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        buttons = arrayOf( findViewById( R.id.button_0 ),
            findViewById( R.id.button_1 ),
            findViewById( R.id.button_2 ),
            findViewById( R.id.button_3 ),
            findViewById( R.id.button_4 ),
            findViewById( R.id.button_5 ),
            findViewById( R.id.button_6 ),
            findViewById( R.id.button_7 ),
            findViewById( R.id.button_8 ) )

        winner = findViewById(R.id.winner)

        p1Icon = intent.getStringExtra( "ICON" )
        p2Icon = if( p1Icon.equals( "X") ) {
            "O"
        } else {
            "X"
        }

    }

    fun onClick(v: View) {
        if ((v as Button).text.toString() != "") {
            return
        }
        if (p1Turn) {
            (v as Button).text = p1Icon
        } else {
            (v as Button).text = p2Icon
        }
        count++

        if (checkForWin()) {
            if (p1Turn) {
                p1Wins()
            } else {
                p2Wins()
            }
        } else if (count == 9) {
            draw()
        } else {
            p1Turn = !p1Turn
        }
    }

    private fun checkForWin(): Boolean {
        val field = arrayOf( button_0.text.toString(),
            button_1.text.toString(),
            button_2.text.toString(),
            button_3.text.toString(),
            button_4.text.toString(),
            button_5.text.toString(),
            button_6.text.toString(),
            button_7.text.toString(),
            button_8.text.toString() )

        if ( field[0] == field[1] && field[1] == field[2] && field[0] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[0] == field[3] && field[3] == field[6] && field[0] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[0] == field[4] && field[4] == field[8] && field[0] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[1] == field[4] && field[4] == field[7] && field[1] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[2] == field[4] && field[4] == field[6] && field[2] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[2] == field[5] && field[5] == field[8] && field[2] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[3] == field[4] && field[4] == field[5] && field[3] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        } else if ( field[6] == field[7] && field[7] == field[8] && field[6] != "" )
        {
            Toast.makeText(applicationContext,"Game is over!", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun p1Wins() {
        winner?.text = "Winner: Player 1!"
        count = 0
    }

    private fun p2Wins() {
        winner?.text = "Winner: Player 2!"
        count = 0
    }

    private fun draw() {
        winner?.text = "It is a Draw!"
    }

    fun reset( view: View ){
        winner?.text = "Winner: Player"
        for (i in 0..8) {
            buttons?.get(i)?.text = ""
        }
        count = 0
    }

}
