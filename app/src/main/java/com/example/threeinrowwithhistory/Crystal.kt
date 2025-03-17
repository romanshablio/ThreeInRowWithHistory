package com.example.threeinrowwithhistory

enum class CrystalColor {
    RED, BLUE, YELLOW, PURPLE, PINK, RAINBOW
}

data class Crystal(
    var color: CrystalColor,
    var row: Int,
    var col: Int
) 