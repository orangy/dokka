package it.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Some Activity implementing [AppCompatActivity] from android x
 */
class IntegrationTestActivity : AppCompatActivity() {
    /**
     * Will show a small happy text
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this)
        textView.text = "I am so happy :)"
        setContentView(textView)
    }
}