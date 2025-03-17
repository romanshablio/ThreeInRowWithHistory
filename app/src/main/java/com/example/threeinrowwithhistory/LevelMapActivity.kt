package com.example.threeinrowwithhistory

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class LevelMapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_map)

        // Настраиваем кнопку "Назад"
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Получаем текущий доступный уровень из SharedPreferences
        val sharedPrefs = getSharedPreferences("game_progress", MODE_PRIVATE)
        val currentLevel = sharedPrefs.getInt("current_level", 1)

        // Настраиваем кнопки уровней
        setupLevelButtons(currentLevel)
    }

    private fun setupLevelButtons(currentLevel: Int) {
        // Массив ID кнопок уровней
        val levelButtonIds = arrayOf(
            R.id.level1Button, R.id.level2Button, R.id.level3Button,
            R.id.level4Button, R.id.level5Button, R.id.level6Button,
            R.id.level7Button, R.id.level8Button, R.id.level9Button,
            R.id.level10Button
        )

        // Настраиваем каждую кнопку
        levelButtonIds.forEachIndexed { index, buttonId ->
            val levelNumber = index + 1
            val levelButton = findViewById<ImageView>(buttonId)

            // Если уровень доступен
            if (levelNumber <= currentLevel) {
                levelButton.alpha = 1.0f
                levelButton.setOnClickListener {
                    startLevel(levelNumber)
                }
            } else {
                // Если уровень недоступен, делаем кнопку полупрозрачной
                levelButton.alpha = 0.5f
                levelButton.setOnClickListener(null)
            }
        }
    }

    private fun startLevel(levelNumber: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL_NUMBER", levelNumber)
        startActivity(intent)
    }
} 