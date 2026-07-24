package com.example.myfirstapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textViewCount = findViewById<TextView>(R.id.textViewCount)
        val buttonClick = findViewById<Button>(R.id.buttonClick)

        buttonClick.setOnClickListener {
            count++
            textViewCount.text = count.toString()
        }
    }
}