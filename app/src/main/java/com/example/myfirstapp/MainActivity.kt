package com.example.myfirstapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем простой интерфейс прямо в коде, чтобы исключить ошибки разметки XML
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val textView = TextView(this).apply {
            text = "Приложение успешно запущено!"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }

        layout.addView(textView)
        setContentView(layout)
    }
}
