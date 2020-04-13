package com.example.tictactoe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var icon: String? = null
    private var iconChosen = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vs.setOnTouchListener(object : OnSwipeTouchListener() {
            override fun onSwipeLeft() {
                Log.d("ViewSwipe", "Left")
                Toast.makeText(applicationContext,"Pick X",Toast.LENGTH_SHORT).show()
                iconChosen = true
                icon = "X"
            }

            override fun onSwipeRight() {
                Log.d("ViewSwipe", "Right")
                Toast.makeText(applicationContext,"Pick O",Toast.LENGTH_SHORT).show()
                iconChosen = true
                icon = "O"
            }
        })
    }

    fun clickX(view: View){
        //Create intent
        val i = Intent(this, Game::class.java)
        if ( !iconChosen )
        {
            icon = "X"
            iconChosen = false
        }
        i.putExtra( "ICON", icon)
        startActivity( i )
    }

    fun clickO(view: View){
        //Create intent
        val i = Intent(this, Game::class.java)
        if ( !iconChosen )
        {
            icon = "O"
            iconChosen = false
        }
        i.putExtra( "ICON", icon)
        startActivity( i )
    }
}
