package com.example.threeinrowwithhistory

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import android.widget.Button

class LevelsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        // Обработка нажатия кнопки "Назад"
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Загрузка прогресса игрока
        val currentLevel = loadCurrentLevel()

        // Настройка внешнего вида уровней
        setupLevels(currentLevel)
    }

    private fun loadCurrentLevel(): Int {
        val sharedPrefs = getSharedPreferences("game_progress", MODE_PRIVATE)
        return sharedPrefs.getInt("current_level", 1)
    }

    private fun setupLevels(currentLevel: Int) {
        for (i in 1..10) {
            val levelButton = findViewById<ShapeableImageView>(
                resources.getIdentifier("level$i", "id", packageName)
            )
            
            if (i <= currentLevel) {
                levelButton.setOnClickListener {
                    if (i == 1) showLevelOneDialog() else startLevel(i)
                }
                if (i == currentLevel) {
                    levelButton.startGlowAnimation()
                }
            }
        }
    }

    private fun showLevelOneDialog() {
        AlertDialog.Builder(this)
            .setTitle("Уровень 1")
            .setMessage("Это первый уровень. Ваша задача набрать 100 очков.")
            .setPositiveButton("Начать") { _, _ ->
                startLevel(1)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun startLevel(levelNumber: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL_NUMBER", levelNumber)
        startActivity(intent)
    }

    private fun ShapeableImageView.startGlowAnimation() {
        val glowAnimation = AnimationUtils.loadAnimation(context, R.anim.glow)
        this.startAnimation(glowAnimation)
    }
} 