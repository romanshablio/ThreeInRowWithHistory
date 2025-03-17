package com.example.threeinrowwithhistory

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.startButton).setOnClickListener {
            // Анимация пульсации для кнопки
            val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            it.startAnimation(pulseAnimation)

            // Переход к карте уровней с современной анимацией
            val intent = Intent(this, LevelMapActivity::class.java)
            
            // Создаем объект опций с нужными анимациями
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this, 
                R.anim.fade_in, 
                R.anim.fade_out
            )
            
            // Запускаем активность с анимацией
            startActivity(intent, options.toBundle())
        }
    }
} 