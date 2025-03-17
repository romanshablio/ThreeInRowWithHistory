package com.example.threeinrowwithhistory

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs
import kotlin.random.Random

class GameActivity : AppCompatActivity() {
    private lateinit var gameGrid: GridLayout
    private lateinit var scoreText: TextView
    private lateinit var timerText: TextView
    private var score = 0
    private var selectedCrystal: Crystal? = null
    private var selectedView: ImageView? = null
    private var gridWidth = 5
    private var gridHeight = 5
    private lateinit var crystals: Array<Array<Crystal>>
    private lateinit var fieldShape: Array<BooleanArray>
    private var handler = Handler(Looper.getMainLooper())
    private var canInteract = true
    private var levelNumber = 1
    private var scoreGoal = 100
    private var showedTrickMessage = false
    private var levelCompleted = false
    private var backgroundIndex = 0
    private lateinit var gestureDetector: GestureDetector
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeStartView: ImageView? = null
    private var swipeStartCrystal: Crystal? = null
    private var timer: CountDownTimer? = null
    private var timeRemaining: Long = 15000 // 15 seconds in milliseconds
    private val timeBonus: Long = 2000 // 2 seconds bonus in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Инициализируем views
        gameGrid = findViewById(R.id.gameGrid)
        scoreText = findViewById(R.id.scoreText)
        timerText = findViewById(R.id.timerText)

        // Получаем номер уровня и устанавливаем размер поля
        levelNumber = intent.getIntExtra("LEVEL_NUMBER", 1)
        setupGridSize()
        
        // Устанавливаем цель уровня в зависимости от номера уровня
        scoreGoal = when (levelNumber) {
            1 -> 100
            2 -> 500
            else -> 1000
        }
        
        initializeGrid()
        updateUI()
        
        while (checkMatches()) {
            removeMatches()
            fillEmptySpaces()
        }

        findViewById<ImageView>(R.id.surrenderButton).setOnClickListener {
            showSurrenderConfirmationDialog()
        }

        // Инициализируем детектор жестов
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || !canInteract || levelCompleted) return false
                
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                // Определяем направление свайпа
                if (abs(deltaX) > abs(deltaY)) {
                    // Горизонтальный свайп
                    handleSwipe(deltaX, deltaY)
                } else {
                    // Вертикальный свайп
                    handleSwipe(deltaX, deltaY)
                }
                return true
            }
        })

        // Показываем диалог с целью уровня перед началом игры
        showLevelObjectiveDialog()
    }

    private fun setRandomBackground() {
        // Метод оставлен пустым, так как фон теперь устанавливается через layout
    }

    private fun setupGridSize() {
        when (levelNumber) {
            1 -> {
                gridWidth = 5
                gridHeight = 5
            }
            2 -> {
                gridWidth = 7
                gridHeight = 7
            }
            3 -> {
                gridWidth = 7
                gridHeight = 9
            }
            4 -> {
                gridWidth = 9
                gridHeight = 7
            }
            else -> {
                gridWidth = 9
                gridHeight = 9
            }
        }
        
        // Инициализируем массивы с новыми размерами
        crystals = Array(gridHeight) { row ->
            Array(gridWidth) { col ->
                Crystal(randomColor(), row, col)
            }
        }
        
        fieldShape = Array(gridHeight) { BooleanArray(gridWidth) { true } }
    }

    private fun initializeGrid() {
        gameGrid.removeAllViews()
        
        gameGrid.columnCount = gridWidth
        gameGrid.rowCount = gridHeight
        
        // Вычисляем размер ячейки
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val availableHeight = screenHeight * 0.7
        
        val cellSize = Math.min(
            (screenWidth * 0.9 / gridWidth).toInt(),
            (availableHeight / gridHeight).toInt()
        )
        
        // Создаем и добавляем все кристаллы
        for (row in 0 until gridHeight) {
            for (col in 0 until gridWidth) {
                if (fieldShape[row][col]) {
                    val crystal = crystals[row][col]
                    val imageView = createCrystalView(crystal)
                    
                    val params = GridLayout.LayoutParams()
                    params.rowSpec = GridLayout.spec(row)
                    params.columnSpec = GridLayout.spec(col)
                    params.width = cellSize
                    params.height = cellSize
                    params.setMargins(2, 2, 2, 2)
                    imageView.layoutParams = params
                    
                    // Устанавливаем начальную видимость
                    imageView.alpha = 1f
                    // Устанавливаем цвет кристалла
                    imageView.setColorFilter(getColorForCrystal(crystal.color))
                    
                    gameGrid.addView(imageView)
                }
            }
        }
        
        // Обновляем UI после инициализации
        handler.post {
            updateUI()
        }
    }

    private fun createCrystalView(crystal: Crystal): ImageView {
        return ImageView(this).apply {
            setImageResource(R.drawable.crystal_base)
            setColorFilter(getColorForCrystal(crystal.color))
            tag = crystal
            
            // Добавляем обработчик касаний для поддержки как тапов, так и свайпов
            setOnTouchListener { view, event ->
                if (!canInteract || levelCompleted) return@setOnTouchListener false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Сохраняем начальную позицию касания
                        swipeStartX = event.x
                        swipeStartY = event.y
                        swipeStartView = view as ImageView
                        swipeStartCrystal = view.tag as Crystal
                        true
                    }
                    
                    MotionEvent.ACTION_UP -> {
                        val deltaX = event.x - swipeStartX
                        val deltaY = event.y - swipeStartY
                        
                        // Если движение было минимальным, обрабатываем как обычное нажатие
                        if (Math.abs(deltaX) < 20 && Math.abs(deltaY) < 20) {
                            handleTap(view as ImageView)
                        } else {
                            // Иначе обрабатываем как свайп
                            handleSwipe(deltaX, deltaY)
                        }
                        true
                    }
                    
                    else -> false
                }
            }
            
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(8, 8, 8, 8)
        }
    }
    
    // Получаем цвет для кристалла на основе его типа
    private fun getColorForCrystal(color: CrystalColor): Int {
        return when (color) {
            CrystalColor.RED -> Color.argb(180, 255, 0, 0)
            CrystalColor.BLUE -> Color.argb(180, 0, 120, 255)
            CrystalColor.YELLOW -> Color.argb(180, 255, 215, 0)
            CrystalColor.PURPLE -> Color.argb(180, 148, 0, 211)
            CrystalColor.PINK -> Color.argb(180, 255, 20, 147)
            CrystalColor.RAINBOW -> {
                // Для разноцветного кристалла не используем фильтр
                // Вместо этого можно создать специальное радужное изображение
                Color.argb(0, 255, 255, 255) // Прозрачный фильтр
            }
        }
    }
    
    // Применяем специальный эффект для выделенного кристалла
    private fun applySelectedEffect(imageView: ImageView, isSelected: Boolean) {
        if (isSelected) {
            // Создаем эффект свечения для выделенного кристалла
            imageView.setBackgroundResource(R.drawable.crystal_selected_glow)
        } else {
            // Убираем эффект
            imageView.background = null
        }
    }

    private fun handleTap(view: ImageView) {
        val clickedCrystal = view.tag as Crystal
        
        if (selectedCrystal == null) {
            // Первый выбор
            selectedCrystal = clickedCrystal
            selectedView = view
            applySelectedEffect(view, true)
        } else {
            // Второй выбор
            if (areAdjacent(selectedCrystal!!, clickedCrystal)) {
                canInteract = false
                // Анимируем обмен
                animateSwap(selectedView!!, view) {
                    swapCrystals(selectedCrystal!!, clickedCrystal)
                    
                    // Проверяем, образовалось ли совпадение
                    if (checkMatches()) {
                        processMatches()
                    } else {
                        // Возвращаем кристаллы на места
                        animateSwap(view, selectedView!!) {
                            swapCrystals(clickedCrystal, selectedCrystal!!)
                            applySelectedEffect(selectedView!!, false)
                            selectedCrystal = null
                            selectedView = null
                            canInteract = true
                        }
                    }
                }
            } else {
                // Если кристаллы не соседние, снимаем выделение с предыдущего
                applySelectedEffect(selectedView!!, false)
                selectedCrystal = clickedCrystal
                selectedView = view
                applySelectedEffect(view, true)
            }
        }
    }

    private fun handleSwipe(deltaX: Float, deltaY: Float) {
        if (swipeStartCrystal == null || swipeStartView == null) return
        
        // Определяем направление свайпа
        val direction = when {
            Math.abs(deltaX) > Math.abs(deltaY) -> {
                if (deltaX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
            }
            else -> {
                if (deltaY > 0) SwipeDirection.DOWN else SwipeDirection.UP
            }
        }
        
        val currentCrystal = swipeStartCrystal!!
        val currentView = swipeStartView!! // Сохраняем ссылку на текущий view
        val row = currentCrystal.row
        val col = currentCrystal.col
        
        // Определяем координаты целевого кристалла
        val targetRow = when (direction) {
            SwipeDirection.UP -> row - 1
            SwipeDirection.DOWN -> row + 1
            else -> row
        }
        val targetCol = when (direction) {
            SwipeDirection.LEFT -> col - 1
            SwipeDirection.RIGHT -> col + 1
            else -> col
        }
        
        // Проверяем, что координаты в пределах поля
        if (targetRow in 0 until gridHeight && targetCol in 0 until gridWidth && 
            fieldShape[targetRow][targetCol]) {
            
            val targetCrystal = crystals[targetRow][targetCol]
            val targetView = findViewForCrystal(targetCrystal)
            
            if (targetView != null) {
                canInteract = false
                // Анимируем обмен
                animateSwap(currentView, targetView) {
                    swapCrystals(currentCrystal, targetCrystal)
                    
                    // Проверяем, образовалось ли совпадение
                    if (checkMatches()) {
                        processMatches()
                    } else {
                        // Возвращаем кристаллы на места
                        animateSwap(targetView, currentView) {
                            swapCrystals(targetCrystal, currentCrystal)
                            canInteract = true
                        }
                    }
                }
            }
        }
        
        // Сбрасываем состояние свайпа
        swipeStartView = null
        swipeStartCrystal = null
    }

    private fun findViewForCrystal(crystal: Crystal): ImageView? {
        for (i in 0 until gameGrid.childCount) {
            val view = gameGrid.getChildAt(i) as? ImageView
            val viewCrystal = view?.tag as? Crystal
            if (viewCrystal == crystal) {
                return view
            }
        }
        return null
    }

    private fun animateSwap(view1: ImageView, view2: ImageView, onComplete: () -> Unit) {
        val crystal1 = view1.tag as Crystal
        val crystal2 = view2.tag as Crystal
        
        // Получаем координаты обоих представлений
        val loc1 = IntArray(2)
        val loc2 = IntArray(2)
        view1.getLocationInWindow(loc1)
        view2.getLocationInWindow(loc2)
        
        // Рассчитываем смещение для анимации
        val deltaX = loc2[0] - loc1[0]
        val deltaY = loc2[1] - loc1[1]
        
        // Создаем анимации для первого вида
        val anim1X = ObjectAnimator.ofFloat(view1, "translationX", 0f, deltaX.toFloat())
        val anim1Y = ObjectAnimator.ofFloat(view1, "translationY", 0f, deltaY.toFloat())
        
        // Создаем анимации для второго вида
        val anim2X = ObjectAnimator.ofFloat(view2, "translationX", 0f, -deltaX.toFloat())
        val anim2Y = ObjectAnimator.ofFloat(view2, "translationY", 0f, -deltaY.toFloat())
        
        // Объединяем анимации в наборы
        val set1 = AnimatorSet()
        set1.playTogether(anim1X, anim1Y)
        
        val set2 = AnimatorSet()
        set2.playTogether(anim2X, anim2Y)
        
        // Настраиваем и запускаем анимацию
        val mainSet = AnimatorSet()
        mainSet.playTogether(set1, set2)
        mainSet.duration = 300
        mainSet.interpolator = AccelerateDecelerateInterpolator()
        
        mainSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Сбрасываем смещения
                view1.translationX = 0f
                view1.translationY = 0f
                view2.translationX = 0f
                view2.translationY = 0f
                
                // Вызываем обратный вызов завершения
                onComplete()
            }
        })
        
        mainSet.start()
    }

    private fun areAdjacent(c1: Crystal, c2: Crystal): Boolean {
        return (Math.abs(c1.row - c2.row) == 1 && c1.col == c2.col) ||
                (Math.abs(c1.col - c2.col) == 1 && c1.row == c2.row)
    }

    private fun swapCrystals(c1: Crystal, c2: Crystal) {
        val tempColor = c1.color
        c1.color = c2.color
        c2.color = tempColor
        updateUI()
    }
    
    private fun processMatches() {
        // Начинаем цепочку обработки совпадений
        handler.post(object : Runnable {
            override fun run() {
                if (checkMatches()) {
                    // Сохраняем предыдущий счет для сравнения
                    val previousScore = score
                    // Удаляем совпадения, добавляем очки и обновляем UI
                    removeMatches()
                    score += 30
                    updateUI()
                    
                    // Если получено 30 или более очков, добавляем время
                    if (score - previousScore >= 30) {
                        addTimeBonus()
                    }
                    
                    // Проверяем достижение цели для первого уровня
                    checkLevelGoal()
                    
                    // Обработка падения кристаллов и заполнение пустых мест
                    handler.postDelayed({
                        fillEmptySpaces()
                        updateUI()
                        
                        // Проверка новых совпадений через небольшую задержку
                        handler.postDelayed({
                            if (checkMatches()) {
                                handler.post(this)
                            } else {
                                // Проверяем, не завершен ли уровень
                                if (!levelCompleted) {
                                    // Проверяем, что selectedView и selectedCrystal не равны null
                                    if (selectedView != null && selectedCrystal != null) {
                                        applySelectedEffect(selectedView!!, false)
                                    }
                                    selectedCrystal = null
                                    selectedView = null
                                    canInteract = true
                                }
                            }
                        }, 300)
                    }, 300)
                } else {
                    // Проверяем, не завершен ли уровень
                    if (!levelCompleted) {
                        // Проверяем, что selectedView и selectedCrystal не равны null
                        if (selectedView != null && selectedCrystal != null) {
                            applySelectedEffect(selectedView!!, false)
                        }
                        selectedCrystal = null
                        selectedView = null
                        canInteract = true
                    }
                }
            }
        })
    }
    
    private fun checkLevelGoal() {
        if (levelNumber == 1 && score >= 100 && !showedTrickMessage) {
            showedTrickMessage = true
            canInteract = false
            
            // Показываем трюк-сообщение
            handler.postDelayed({
                showTrickMessage()
            }, 500)
            
            // Меняем цель уровня
            scoreGoal = 1000
        } else if (score >= scoreGoal && !levelCompleted) {
            // Показываем сообщение о прохождении уровня
            levelCompleted = true
            canInteract = false
            
            handler.postDelayed({
                showLevelCompleteDialog()
            }, 500)
        }
    }
    
    private fun showTrickMessage() {
        timer?.cancel() // Останавливаем таймер
        
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_trick_message)
        
        // Устанавливаем фон диалога
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Создаем градиентный фон для диалога
        val containerView = dialog.findViewById<View>(R.id.dialogContainer)
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 20f
        gradientDrawable.colors = intArrayOf(
            Color.parseColor("#673AB7"), // Фиолетовый
            Color.parseColor("#3F51B5")  // Синий
        )
        gradientDrawable.orientation = GradientDrawable.Orientation.TL_BR
        containerView.background = gradientDrawable
        
        // Настраиваем кнопку продолжения
        val continueButton = dialog.findViewById<Button>(R.id.continueButton)
        continueButton.setOnClickListener {
            dialog.dismiss()
            canInteract = true
            startTimer() // Возобновляем таймер после закрытия диалога
        }
        
        dialog.show()
    }
    
    private fun showLevelCompleteDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_level_complete)
        
        // Сохраняем прогресс игрока сразу при завершении уровня
        val sharedPrefs = getSharedPreferences("game_progress", MODE_PRIVATE)
        val currentMaxLevel = sharedPrefs.getInt("current_level", 1)
        if (levelNumber + 1 > currentMaxLevel) {
            sharedPrefs.edit().putInt("current_level", levelNumber + 1).apply()
        }
        
        // Устанавливаем фон диалога
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Создаем градиентный фон для диалога
        val containerView = dialog.findViewById<View>(R.id.dialogContainer)
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 20f
        gradientDrawable.colors = intArrayOf(
            Color.parseColor("#4CAF50"), // Зелёный (для победы)
            Color.parseColor("#009688")  // Бирюзовый
        )
        gradientDrawable.orientation = GradientDrawable.Orientation.TL_BR
        containerView.background = gradientDrawable
        
        // Обновляем текст с результатом
        val scoreResultText = dialog.findViewById<TextView>(R.id.scoreResultText)
        scoreResultText.text = "Вы набрали $score очков!"
        
        // Настраиваем кнопку возврата в меню
        val menuButton = dialog.findViewById<Button>(R.id.menuButton)
        menuButton.setOnClickListener {
            dialog.dismiss()
            // Возвращаемся в меню уровней
            finish()
        }
        
        // Настраиваем кнопку перехода к следующему уровню
        val continueButton = dialog.findViewById<Button>(R.id.continueButton)
        continueButton.setOnClickListener {
            dialog.dismiss()
            // Переходим к следующему уровню
            val nextLevelIntent = Intent(this, GameActivity::class.java)
            nextLevelIntent.putExtra("LEVEL_NUMBER", levelNumber + 1)
            startActivity(nextLevelIntent)
            finish()
        }
        
        dialog.show()
    }

    private fun checkMatches(isAfterSwap: Boolean = false): Boolean {
        var hasMatches = false
        // Проверка по горизонтали
        for (row in 0 until gridHeight) {
            for (col in 0..gridWidth - 3) {
                if (fieldShape[row][col] && fieldShape[row][col + 1] && fieldShape[row][col + 2]) {
                    if (crystals[row][col].color == crystals[row][col + 1].color &&
                        crystals[row][col].color == crystals[row][col + 2].color) {
                        hasMatches = true
                    }
                }
            }
        }
        // Проверка по вертикали
        for (row in 0..gridHeight - 3) {
            for (col in 0 until gridWidth) {
                if (fieldShape[row][col] && fieldShape[row + 1][col] && fieldShape[row + 2][col]) {
                    if (crystals[row][col].color == crystals[row + 1][col].color &&
                        crystals[row][col].color == crystals[row + 2][col].color) {
                        hasMatches = true
                    }
                }
            }
        }
        
        // Проверяем необходимость перемешивания только если это не проверка после свапа
        if (!isAfterSwap && !hasMatches && !checkPossibleMoves()) {
            shuffleField()
            return checkMatches(true) // Предотвращаем повторное перемешивание
        }
        
        return hasMatches
    }

    private fun checkPossibleMoves(): Boolean {
        for (row in 0 until gridHeight) {
            for (col in 0 until gridWidth) {
                if (!fieldShape[row][col]) continue
                
                // Проверяем горизонтальный обмен
                if (col < gridWidth - 1 && fieldShape[row][col + 1]) {
                    // Временно меняем местами
                    val temp = crystals[row][col].color
                    crystals[row][col].color = crystals[row][col + 1].color
                    crystals[row][col + 1].color = temp
                    
                    // Проверяем, образуется ли комбинация
                    if (checkMatches(true)) {
                        // Возвращаем кристаллы на места
                        crystals[row][col + 1].color = crystals[row][col].color
                        crystals[row][col].color = temp
                        return true
                    }
                    
                    // Возвращаем кристаллы на места
                    crystals[row][col + 1].color = crystals[row][col].color
                    crystals[row][col].color = temp
                }
                
                // Проверяем вертикальный обмен
                if (row < gridHeight - 1 && fieldShape[row + 1][col]) {
                    // Временно меняем местами
                    val temp = crystals[row][col].color
                    crystals[row][col].color = crystals[row + 1][col].color
                    crystals[row + 1][col].color = temp
                    
                    // Проверяем, образуется ли комбинация
                    if (checkMatches(true)) {
                        // Возвращаем кристаллы на места
                        crystals[row + 1][col].color = crystals[row][col].color
                        crystals[row][col].color = temp
                        return true
                    }
                    
                    // Возвращаем кристаллы на места
                    crystals[row + 1][col].color = crystals[row][col].color
                    crystals[row][col].color = temp
                }
            }
        }
        return false
    }

    private fun shuffleField() {
        // Создаем список всех цветов на поле
        val colors = mutableListOf<CrystalColor>()
        for (row in 0 until gridHeight) {
            for (col in 0 until gridWidth) {
                if (fieldShape[row][col]) {
                    colors.add(crystals[row][col].color)
                }
            }
        }
        
        // Перемешиваем список цветов
        colors.shuffle()
        
        // Анимация исчезновения всех кристаллов
        for (i in 0 until gameGrid.childCount) {
            val view = gameGrid.getChildAt(i) as ImageView
            val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.start()
        }
        
        // Распределяем перемешанные цвета обратно на поле
        var colorIndex = 0
        for (row in 0 until gridHeight) {
            for (col in 0 until gridWidth) {
                if (fieldShape[row][col]) {
                    crystals[row][col].color = colors[colorIndex]
                    colorIndex++
                }
            }
        }
        
        // Анимация появления перемешанных кристаллов
        handler.postDelayed({
            for (i in 0 until gameGrid.childCount) {
                val view = gameGrid.getChildAt(i) as ImageView
                val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
                fadeIn.duration = 300
                fadeIn.start()
            }
            updateUI()
        }, 300)
    }

    private fun createParticle(x: Float, y: Float, color: Int): ImageView {
        return ImageView(this).apply {
            setImageResource(R.drawable.crystal_particle)
            setColorFilter(color)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0.8f
            
            // Устанавливаем начальную позицию
            translationX = x
            translationY = y
            
            // Устанавливаем случайный размер частицы
            val size = (10..20).random()
            layoutParams = ConstraintLayout.LayoutParams(size, size)
        }
    }

    private fun animateParticle(particle: ImageView, startX: Float, startY: Float) {
        // Генерируем случайное направление движения
        val angle = Random.nextDouble(2 * Math.PI)
        val speed = (100..200).random().toFloat()
        
        // Рассчитываем конечную позицию
        val endX = startX + (speed * Math.cos(angle)).toFloat()
        val endY = startY + (speed * Math.sin(angle)).toFloat()
        
        // Создаем анимации движения
        val translateX = ObjectAnimator.ofFloat(particle, "translationX", startX, endX)
        val translateY = ObjectAnimator.ofFloat(particle, "translationY", startY, endY)
        
        // Добавляем вращение
        val rotation = ObjectAnimator.ofFloat(particle, "rotation", 0f, (-360..360).random().toFloat())
        
        // Добавляем исчезновение
        val fade = ObjectAnimator.ofFloat(particle, "alpha", 0.8f, 0f)
        
        // Объединяем анимации
        val animSet = AnimatorSet()
        animSet.playTogether(translateX, translateY, rotation, fade)
        animSet.duration = 500
        animSet.interpolator = AccelerateDecelerateInterpolator()
        
        // Удаляем частицу после завершения анимации
        animSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                val parent = particle.parent as ViewGroup
                parent.removeView(particle)
            }
        })
        
        animSet.start()
    }

    private fun createExplosionEffect(view: ImageView, color: Int) {
        // Получаем координаты view на экране
        val location = IntArray(2)
        view.getLocationInWindow(location)
        
        // Создаем частицы
        val numParticles = 12
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        
        // Центр view
        val centerX = location[0] + view.width / 2f
        val centerY = location[1] + view.height / 2f
        
        // Создаем и анимируем частицы
        repeat(numParticles) {
            val particle = createParticle(centerX, centerY, color)
            rootView.addView(particle)
            animateParticle(particle, centerX, centerY)
        }
    }

    private fun removeMatches() {
        // Создаем сетку для маркировки совпадений
        val matched = Array(gridHeight) { BooleanArray(gridWidth) { false } }
        var matchLength = 0 // Длина текущего совпадения
        var totalMatches = 0 // Общее количество совпадений
        
        // Отмечаем горизонтальные совпадения
        for (row in 0 until gridHeight) {
            var currentLength = 1
            for (col in 1 until gridWidth) {
                if (fieldShape[row][col] && fieldShape[row][col - 1] &&
                    crystals[row][col].color == crystals[row][col - 1].color) {
                    currentLength++
                } else {
                    if (currentLength >= 3) {
                        // Отмечаем все кристаллы в совпадении
                        for (i in 0 until currentLength) {
                            matched[row][col - i - 1] = true
                        }
                        matchLength = maxOf(matchLength, currentLength)
                        totalMatches++
                    }
                    currentLength = 1
                }
            }
            // Проверяем последнее совпадение в строке
            if (currentLength >= 3) {
                for (i in 0 until currentLength) {
                    matched[row][gridWidth - i - 1] = true
                }
                matchLength = maxOf(matchLength, currentLength)
                totalMatches++
            }
        }
        
        // Отмечаем вертикальные совпадения
        for (col in 0 until gridWidth) {
            var currentLength = 1
            for (row in 1 until gridHeight) {
                if (fieldShape[row][col] && fieldShape[row - 1][col] &&
                    crystals[row][col].color == crystals[row - 1][col].color) {
                    currentLength++
                } else {
                    if (currentLength >= 3) {
                        // Отмечаем все кристаллы в совпадении
                        for (i in 0 until currentLength) {
                            matched[row - i - 1][col] = true
                        }
                        matchLength = maxOf(matchLength, currentLength)
                        totalMatches++
                    }
                    currentLength = 1
                }
            }
            // Проверяем последнее совпадение в столбце
            if (currentLength >= 3) {
                for (i in 0 until currentLength) {
                    matched[gridHeight - i - 1][col] = true
                }
                matchLength = maxOf(matchLength, currentLength)
                totalMatches++
            }
        }
        
        // Создаем список для хранения всех анимаций
        val animations = mutableListOf<ObjectAnimator>()
        var matchedCount = 0 // Количество уничтоженных кристаллов
        
        // Заменяем совпавшие кристаллы и создаем анимации
        for (row in 0 until gridHeight) {
            for (col in 0 until gridWidth) {
                if (matched[row][col] && fieldShape[row][col]) {
                    matchedCount++
                    // Находим соответствующую View для кристалла
                    val view = findViewForCrystal(crystals[row][col])
                    if (view != null) {
                        // Создаем эффект взрыва
                        createExplosionEffect(view, getColorForCrystal(crystals[row][col].color))
                        
                        // Создаем анимацию исчезновения
                        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
                        fadeOut.duration = 200
                        animations.add(fadeOut)
                    }
                    
                    // Генерируем новый случайный цвет для кристалла
                    crystals[row][col].color = randomColor()
                }
            }
        }
        
        // Рассчитываем бонусные очки
        val basePoints = 10 // Базовые очки за один кристалл
        val lengthBonus = when {
            matchLength >= 5 -> 3.0 // Тройной бонус за 5+ кристаллов
            matchLength == 4 -> 2.0 // Двойной бонус за 4 кристалла
            else -> 1.0 // Без бонуса за 3 кристалла
        }
        val comboBonus = if (totalMatches > 1) 1.5 else 1.0 // Бонус за несколько комбинаций
        
        // Начисляем очки
        val points = (basePoints * matchedCount * lengthBonus * comboBonus).toInt()
        score += points
        
        // Показываем бонусные очки
        if (lengthBonus > 1.0 || comboBonus > 1.0) {
            showBonusPoints(points)
        }
        
        // Запускаем все анимации одновременно
        animations.forEach { it.start() }
    }

    private fun showBonusPoints(points: Int) {
        // Создаем TextView для отображения бонусных очков
        val bonusText = TextView(this).apply {
            text = "+$points"
            textSize = 24f
            setTextColor(Color.YELLOW)
            alpha = 1f
        }
        
        // Добавляем TextView на экран
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.topMargin = 100
        bonusText.layoutParams = params
        rootView.addView(bonusText)
        
        // Анимируем появление и исчезновение текста
        val fadeOut = ObjectAnimator.ofFloat(bonusText, "alpha", 1f, 0f)
        val moveUp = ObjectAnimator.ofFloat(bonusText, "translationY", 0f, -200f)
        
        val animSet = AnimatorSet()
        animSet.playTogether(fadeOut, moveUp)
        animSet.duration = 1000
        animSet.interpolator = AccelerateDecelerateInterpolator()
        
        animSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                rootView.removeView(bonusText)
            }
        })
        
        animSet.start()
    }

    private fun fillEmptySpaces() {
        // В будущем можно реализовать анимацию падения кристаллов
        updateUI()
    }

    private fun updateUI() {
        // Обновляем текст с очками
        scoreText.text = "Очки: $score / $scoreGoal"
        
        // Обновляем все кристаллы
        for (i in 0 until gameGrid.childCount) {
            val view = gameGrid.getChildAt(i) as? ImageView
            val crystal = view?.tag as? Crystal
            if (view != null && crystal != null) {
                view.clearColorFilter()
                view.setColorFilter(getColorForCrystal(crystal.color))
                
                // Если кристалл невидим, делаем его видимым с анимацией
                if (view.alpha < 1f) {
                    val fadeIn = ObjectAnimator.ofFloat(view, "alpha", view.alpha, 1f)
                    fadeIn.duration = 200
                    fadeIn.start()
                }
            }
        }
    }

    private fun randomColor(): CrystalColor {
        return CrystalColor.values()[Random.nextInt(CrystalColor.values().size)]
    }

    private fun showSurrenderConfirmationDialog() {
        timer?.cancel() // Pause timer during dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Подтверждение")
            .setMessage("Вы уверены, что хотите сдаться?")
            .setPositiveButton("Да") { _, _ ->
                finish()
            }
            .setNegativeButton("Нет") { _, _ ->
                startTimer() // Resume timer if user cancels
            }
            .setCancelable(false)
            .show()
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                // Показываем только цифры
                timerText.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                showGameOverDialog()
            }
        }.start()
    }

    private fun addTimeBonus() {
        timeRemaining += timeBonus
        timer?.cancel()
        startTimer()
    }

    private fun showGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Время вышло!")
            .setMessage("К сожалению, время закончилось. Хотите попробовать снова?")
            .setPositiveButton("Да") { _, _ ->
                recreate()
            }
            .setNegativeButton("Нет") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLevelObjectiveDialog() {
        // Останавливаем таймер на время показа диалога
        timer?.cancel()
        
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_level_objective)
        
        // Устанавливаем фон диалога
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Создаем градиентный фон для диалога
        val containerView = dialog.findViewById<View>(R.id.dialogContainer)
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 20f
        gradientDrawable.colors = intArrayOf(
            Color.parseColor("#2196F3"), // Голубой
            Color.parseColor("#3F51B5")  // Синий
        )
        gradientDrawable.orientation = GradientDrawable.Orientation.TL_BR
        containerView.background = gradientDrawable
        
        // Настраиваем текст цели в зависимости от уровня
        val objectiveText = dialog.findViewById<TextView>(R.id.objectiveText)
        objectiveText.text = when (levelNumber) {
            1 -> "Для успешного прохождения уровня необходимо набрать 100 очков"
            2 -> "Для успешного прохождения уровня необходимо набрать 500 очков"
            else -> "Для успешного прохождения уровня необходимо набрать 1000 очков"
        }
        
        // Настраиваем кнопку начала
        val startButton = dialog.findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            dialog.dismiss()
            // Инициализируем и запускаем таймер только после закрытия диалога
            timeRemaining = 15000 // Сбрасываем время до начального значения
            startTimer()
        }
        
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    private enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }
} 