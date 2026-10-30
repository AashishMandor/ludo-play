package com.example.ludoplay

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(243, 244, 246)
        window.navigationBarColor = Color.rgb(243, 244, 246)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(LudoView(this))
    }
}

private data class Cell(val row: Int, val col: Int)
private data class TokenHit(val bounds: RectF, val player: Int, val token: Int)

private data class PlayerSpec(
    val name: String,
    val color: Int,
    val light: Int,
    val start: Int,
    val lane: List<Cell>,
    val yardSlots: List<Pair<Float, Float>>
)

private class LudoView(activity: Activity) : View(activity) {
    private val bg = Color.rgb(243, 244, 246)
    private val ink = Color.rgb(31, 41, 55)
    private val muted = Color.rgb(107, 114, 128)
    private val white = Color.WHITE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hitBoxes = mutableListOf<TokenHit>()

    private val sharedPath = listOf(
        Cell(6, 1), Cell(6, 2), Cell(6, 3), Cell(6, 4), Cell(6, 5),
        Cell(5, 6), Cell(4, 6), Cell(3, 6), Cell(2, 6), Cell(1, 6), Cell(0, 6),
        Cell(0, 7), Cell(0, 8), Cell(1, 8), Cell(2, 8), Cell(3, 8), Cell(4, 8), Cell(5, 8),
        Cell(6, 9), Cell(6, 10), Cell(6, 11), Cell(6, 12), Cell(6, 13), Cell(6, 14),
        Cell(7, 14), Cell(8, 14), Cell(8, 13), Cell(8, 12), Cell(8, 11), Cell(8, 10), Cell(8, 9),
        Cell(9, 8), Cell(10, 8), Cell(11, 8), Cell(12, 8), Cell(13, 8), Cell(14, 8),
        Cell(14, 7), Cell(14, 6), Cell(13, 6), Cell(12, 6), Cell(11, 6), Cell(10, 6), Cell(9, 6),
        Cell(8, 5), Cell(8, 4), Cell(8, 3), Cell(8, 2), Cell(8, 1), Cell(8, 0), Cell(7, 0), Cell(6, 0)
    )

    private val players = listOf(
        PlayerSpec(
            "Red", Color.rgb(220, 55, 65), Color.rgb(255, 218, 220), 0,
            (0..6).map { Cell(7, it) },
            listOf(2.1f to 2.1f, 4.0f to 2.1f, 2.1f to 4.0f, 4.0f to 4.0f)
        ),
        PlayerSpec(
            "Green", Color.rgb(35, 160, 95), Color.rgb(211, 245, 225), 13,
            (0..6).map { Cell(it, 7) },
            listOf(11.0f to 2.1f, 12.9f to 2.1f, 11.0f to 4.0f, 12.9f to 4.0f)
        ),
        PlayerSpec(
            "Blue", Color.rgb(55, 115, 220), Color.rgb(215, 230, 255), 26,
            (14 downTo 8).map { Cell(7, it) },
            listOf(11.0f to 11.0f, 12.9f to 11.0f, 11.0f to 12.9f, 12.9f to 12.9f)
        ),
        PlayerSpec(
            "Yellow", Color.rgb(232, 176, 35), Color.rgb(255, 242, 194), 39,
            (14 downTo 8).map { Cell(it, 7) },
            listOf(2.1f to 11.0f, 4.0f to 11.0f, 2.1f to 12.9f, 4.0f to 12.9f)
        )
    )

    // -1 = yard, 0..51 = shared track, 52..58 = home lane, 59 = finished.
    private val tokenProgress = Array(4) { IntArray(4) { -1 } }
    private var playerCount = 4
    private var turnOrder = listOf(0, 1, 2, 3)
    private var turnPointer = 0
    private var diceValue: Int? = null
    private var hasRolled = false
    private var winner: Int? = null
    private var status = "Red's turn — roll the dice"
    private var waitingForPass = false
    private var pendingPass: Runnable? = null

    private var density = resources.displayMetrics.density
    private var viewWidth = 0f
    private var viewHeight = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
