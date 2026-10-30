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
    private var boardSize = 0f
    private var cellSize = 0f
    private var newGameRect = RectF()
    private var playersRect = RectF()
    private var rollRect = RectF()
    private var diceCenterX = 0f
    private var diceCenterY = 0f

    private val safeIndices = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    init {
        isFocusable = true
        setBackgroundColor(bg)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        density = resources.displayMetrics.density
        viewWidth = width / density
        viewHeight = height / density
        canvas.save()
        canvas.scale(density, density)
        canvas.drawColor(bg)
        drawHeader(canvas)

        boardSize = min(viewWidth - 28f, viewHeight - 248f).coerceAtLeast(230f)
        boardLeft = (viewWidth - boardSize) / 2f
        boardTop = 76f
        cellSize = boardSize / 15f
        drawBoard(canvas)
        drawTokens(canvas)
        drawControls(canvas)
        canvas.restore()
    }

    private fun drawHeader(canvas: Canvas) {
        paint.color = ink
        paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        paint.textSize = 27f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("LUDO", 18f, 43f, paint)
        paint.color = muted
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("OFFLINE PLAY", 20f, 59f, paint)

        playersRect = RectF(viewWidth - 174f, 17f, viewWidth - 99f, 53f)
        newGameRect = RectF(viewWidth - 91f, 17f, viewWidth - 17f, 53f)
        drawPill(canvas, playersRect, white, Color.rgb(229, 231, 235))
        drawPill(canvas, newGameRect, Color.rgb(79, 70, 229), Color.rgb(79, 70, 229))
        drawTextCentered(canvas, "${playerCount} PLAYERS", playersRect.centerX(), 39f, 10f, ink, true)
        drawTextCentered(canvas, "NEW GAME", newGameRect.centerX(), 39f, 10f, white, true)
    }

    private fun drawBoard(canvas: Canvas) {
        val board = RectF(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize)
        paint.color = white
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(board, 10f, 10f, paint)
        paint.color = Color.rgb(209, 213, 219)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(board, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        drawBase(canvas, 0, 0, 0)
        drawBase(canvas, 0, 9, 1)
        drawBase(canvas, 9, 9, 2)
        drawBase(canvas, 9, 0, 3)

        sharedPath.forEachIndexed { index, cell ->
            val owner = when (index) {
                0 -> 0
                13 -> 1
                26 -> 2
                39 -> 3
                else -> -1
            }
            val fill = if (owner >= 0) players[owner].light else white
            drawCell(canvas, cell, fill)
            if (safeIndices.contains(index)) {
                drawTextCentered(
                    canvas, "★", xOf(cell.col + 0.5f), yOf(cell.row + 0.5f) + cellSize * 0.13f,
                    cellSize * 0.44f, if (owner >= 0) players[owner].color else Color.rgb(156, 163, 175), true
                )
            }
        }

        players.forEachIndexed { playerIndex, player ->
            player.lane.forEach { cell -> drawCell(canvas, cell, player.light) }
        }
        drawCenter(canvas)
    }

    private fun drawBase(canvas: Canvas, row: Int, col: Int, playerIndex: Int) {
        val player = players[playerIndex]
        val outer = RectF(
            xOf(col.toFloat()) + 1f,
            yOf(row.toFloat()) + 1f,
            xOf((col + 6).toFloat()) - 1f,
            yOf((row + 6).toFloat()) - 1f
        )
        paint.color = player.color
        canvas.drawRoundRect(outer, cellSize * 0.22f, cellSize * 0.22f, paint)
        val inner = RectF(
            xOf((col + 1).toFloat()), yOf((row + 1).toFloat()),
            xOf((col + 5).toFloat()), yOf((row + 5).toFloat())
        )
        paint.color = white
        canvas.drawRoundRect(inner, cellSize * 0.18f, cellSize * 0.18f, paint)
        paint.color = player.light
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        canvas.drawRoundRect(inner, cellSize * 0.18f, cellSize * 0.18f, paint)
        paint.style = Paint.Style.FILL

        player.yardSlots.forEach { slot ->
            paint.color = player.light
            canvas.drawCircle(xOf(slot.first), yOf(slot.second), cellSize * 0.36f, paint)
            paint.color = Color.argb(45, 0, 0, 0)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawCircle(xOf(slot.first), yOf(slot.second), cellSize * 0.36f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawCell(canvas: Canvas, cell: Cell, fill: Int) {
        val inset = cellSize * 0.035f
        val rect = RectF(
            xOf(cell.col.toFloat()) + inset,
            yOf(cell.row.toFloat()) + inset,
            xOf(cell.col + 1f) - inset,
            yOf(cell.row + 1f) - inset
        )
        paint.color = fill
        canvas.drawRect(rect, paint)
        paint.color = Color.rgb(209, 213, 219)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRect(rect, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawCenter(canvas: Canvas) {
        val left = xOf(6f)
        val top = yOf(6f)
        val right = xOf(9f)
        val bottom = yOf(9f)
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val triangles = listOf(
            Triple(0, floatArrayOf(left, top, left, bottom, cx, cy)),
            Triple(1, floatArrayOf(left, top, right, top, cx, cy)),
            Triple(2, floatArrayOf(right, top, right, bottom, cx, cy)),
            Triple(3, floatArrayOf(left, bottom, right, bottom, cx, cy))
        )
        triangles.forEach { (index, points) ->
            val path = Path().apply {
                moveTo(points[0], points[1])
                lineTo(points[2], points[3])
                lineTo(points[4], points[5])
                close()
            }
            paint.color = players[index].color
            canvas.drawPath(path, paint)
        }
        paint.color = white
        canvas.drawCircle(cx, cy, cellSize * 0.10f, paint)
        paint.color = Color.rgb(209, 213, 219)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(left, top, right, bottom, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTokens(canvas: Canvas) {
        hitBoxes.clear()
        val visiblePlayers = turnOrder.toSet()
        for (playerIndex in players.indices) {
            if (!visiblePlayers.contains(playerIndex)) continue
            for (tokenIndex in 0 until 4) {
                val progress = tokenProgress[playerIndex][tokenIndex]
                val position = tokenPosition(playerIndex, tokenIndex)
                val offset = stackOffset(playerIndex, tokenIndex)
                val cx = xOf(position.first + offset.first)
                val cy = yOf(position.second + offset.second)
                val radius = cellSize * if (progress == -1) 0.32f else 0.34f

                if (progress == -1) {
                    paint.color = players[playerIndex].color
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawCircle(cx, cy, radius + 1.2f, paint)
                    paint.style = Paint.Style.FILL
                }

                paint.color = Color.argb(45, 0, 0, 0)
                canvas.drawCircle(cx + 1f, cy + 2f, radius, paint)
                paint.color = players[playerIndex].color
                canvas.drawCircle(cx, cy, radius, paint)
                paint.color = white
                canvas.drawCircle(cx - radius * 0.22f, cy - radius * 0.24f, radius * 0.19f, paint)
                paint.color = Color.argb(65, 0, 0, 0)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawCircle(cx, cy, radius, paint)
                paint.style = Paint.Style.FILL

                val canSelect = winner == null && hasRolled && playerIndex == currentPlayer() && visiblePlayers.contains(playerIndex) && isMovable(playerIndex, tokenIndex)
                if (canSelect) {
                    paint.color = Color.rgb(17, 24, 39)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawCircle(cx, cy, radius + 3f, paint)
                    paint.style = Paint.Style.FILL
                }
                hitBoxes.add(TokenHit(RectF(cx - radius * 1.5f, cy - radius * 1.5f, cx + radius * 1.5f, cy + radius * 1.5f), playerIndex, tokenIndex))
            }
        }
    }

    private fun drawControls(canvas: Canvas) {
        val panelTop = boardTop + boardSize + 13f
        val panel = RectF(16f, panelTop, viewWidth - 16f, viewHeight - 14f)
        paint.color = white
        canvas.drawRoundRect(panel, 20f, 20f, paint)
        paint.color = Color.rgb(229, 231, 235)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(panel, 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        val title = when {
            winner != null -> "${players[winner!!].name} wins! 🎉"
            else -> status
        }
        drawTextCentered(canvas, title, viewWidth / 2f, panelTop + 31f, 15f, ink, true)
        drawTextCentered(
            canvas,
            if (hasRolled) "Choose one of the outlined tokens" else "Pass the phone after your turn",
            viewWidth / 2f, panelTop + 51f, 10f, muted, false
        )

        diceCenterX = viewWidth / 2f
        diceCenterY = panelTop + 101f
        val diceRect = RectF(diceCenterX - 29f, diceCenterY - 29f, diceCenterX + 29f, diceCenterY + 29f)
        paint.color = Color.rgb(249, 250, 251)
        canvas.drawRoundRect(diceRect, 13f, 13f, paint)
        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(diceRect, 13f, 13f, paint)
        paint.style = Paint.Style.FILL
        drawDice(canvas, diceCenterX, diceCenterY, diceValue ?: 1)

        rollRect = RectF(viewWidth / 2f - 82f, panelTop + 143f, viewWidth / 2f + 82f, panelTop + 188f)
        val buttonEnabled = winner == null && !hasRolled && !waitingForPass
        drawPill(canvas, rollRect, if (buttonEnabled) Color.rgb(79, 70, 229) else Color.rgb(165, 160, 220), Color.TRANSPARENT)
        drawTextCentered(
            canvas,
            when {
                winner != null -> "GAME FINISHED"
                hasRolled -> "SELECT A TOKEN"
                else -> "ROLL DICE"
            },
            rollRect.centerX(), rollRect.centerY() + 4f, 13f, white, true
        )
        if (winner != null) {
            drawTextCentered(canvas, "Tap NEW GAME to play again", viewWidth / 2f, panel.bottom - 12f, 10f, muted, false)
        }
    }

    private fun drawDice(canvas: Canvas, cx: Float, cy: Float, value: Int) {
        val pip = Color.rgb(31, 41, 55)
        val d = 9f
        val positions = when (value) {
            1 -> listOf(0f to 0f)
            2 -> listOf(-1f to -1f, 1f to 1f)
            3 -> listOf(-1f to -1f, 0f to 0f, 1f to 1f)
            4 -> listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)
            5 -> listOf(-1f to -1f, 1f to -1f, 0f to 0f, -1f to 1f, 1f to 1f)
            else -> listOf(-1f to -1f, 1f to -1f, -1f to 0f, 1f to 0f, -1f to 1f, 1f to 1f)
        }
        paint.color = pip
        positions.forEach { (x, y) -> canvas.drawCircle(cx + x * d, cy + y * d, 3.2f, paint) }
    }

    private fun drawPill(canvas: Canvas, rect: RectF, fill: Int, stroke: Int) {
        paint.color = fill
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        if (stroke != Color.TRANSPARENT) {
            paint.color = stroke
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(rect, 12f, 12f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawTextCentered(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean) {
        paint.color = color
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        canvas.drawText(text, x, y, paint)
    }

    private fun xOf(gridX: Float): Float = boardLeft + gridX * cellSize
    private fun yOf(gridY: Float): Float = boardTop + gridY * cellSize

    private fun tokenPosition(playerIndex: Int, tokenIndex: Int): Pair<Float, Float> {
        val progress = tokenProgress[playerIndex][tokenIndex]
        val player = players[playerIndex]
        if (progress == -1) return player.yardSlots[tokenIndex]
        if (progress == 59) {
            val center = 7.5f to 7.5f
            return when (playerIndex) {
                0 -> center.first - 0.26f to center.second
                1 -> center.first to center.second - 0.26f
                2 -> center.first + 0.26f to center.second
                else -> center.first to center.second + 0.26f
            }
        }
        val cell = if (progress <= 51) {
            sharedPath[(player.start + progress) % 52]
        } else {
            player.lane[(progress - 52).coerceIn(0, 6)]
        }
        return cell.col + 0.5f to cell.row + 0.5f
    }

    private fun tokenKey(playerIndex: Int, tokenIndex: Int): String {
        val progress = tokenProgress[playerIndex][tokenIndex]
        if (progress == -1) return "yard-$playerIndex-$tokenIndex"
        if (progress == 59) return "finish-$playerIndex"
        if (progress <= 51) return "track-${(players[playerIndex].start + progress) % 52}"
        return "lane-$playerIndex-${progress - 52}"
    }

    private fun stackOffset(playerIndex: Int, tokenIndex: Int): Pair<Float, Float> {
        val key = tokenKey(playerIndex, tokenIndex)
        val same = mutableListOf<Pair<Int, Int>>()
        for (p in 0..3) for (t in 0..3) if (tokenKey(p, t) == key) same.add(p to t)
        if (same.size < 2) return 0f to 0f
        val rank = same.indexOf(playerIndex to tokenIndex)
        val offsets = listOf(-0.13f to -0.13f, 0.13f to -0.13f, -0.13f to 0.13f, 0.13f to 0.13f)
        return offsets[rank.coerceIn(0, 3)]
    }

    private fun currentPlayer(): Int = turnOrder[turnPointer % turnOrder.size]

    private fun isMovable(playerIndex: Int, tokenIndex: Int): Boolean {
        val value = diceValue ?: return false
        val progress = tokenProgress[playerIndex][tokenIndex]
        if (progress == 59) return false
        if (progress == -1) return value == 6
        return progress + value <= 59
    }

    private fun rollDice() {
        if (winner != null || hasRolled || waitingForPass) return
        val value = Random.nextInt(1, 7)
        diceValue = value
        val player = currentPlayer()
        val movable = (0 until 4).filter { isMovable(player, it) }
        if (movable.isEmpty()) {
            status = if (value == 6) "You rolled a 6 — roll again" else "No valid move — turn passes"
            if (value != 6) {
                waitingForPass = true
                val task = Runnable {
                    pendingPass = null
                    waitingForPass = false
                    if (winner == null) nextTurn()
                    invalidate()
                }
                pendingPass = task
                postDelayed(task, 900L)
            } else {
                waitingForPass = true
                val task = Runnable {
                    pendingPass = null
                    waitingForPass = false
                    if (!hasRolled && winner == null) {
                        diceValue = null
                        status = "${players[currentPlayer()].name}'s turn — roll again"
                        invalidate()
                    }
                }
                pendingPass = task
                postDelayed(task, 900L)
            }
        } else {
            hasRolled = true
            status = "${players[player].name} rolled $value — pick a token"
        }
        invalidate()
    }

    private fun makeMove(playerIndex: Int, tokenIndex: Int) {
        val roll = diceValue ?: return
        if (!isMovable(playerIndex, tokenIndex)) return
        val old = tokenProgress[playerIndex][tokenIndex]
        val newProgress = if (old == -1) 0 else old + roll
        tokenProgress[playerIndex][tokenIndex] = newProgress

        var captured = false
        if (newProgress in 0..51) {
            val landingIndex = (players[playerIndex].start + newProgress) % 52
            if (!safeIndices.contains(landingIndex)) {
                for (opponent in 0..3) {
                    if (opponent == playerIndex) continue
                    for (otherToken in 0 until 4) {
                        val otherProgress = tokenProgress[opponent][otherToken]
                        if (otherProgress in 0..51) {
                            val otherIndex = (players[opponent].start + otherProgress) % 52
                            if (otherIndex == landingIndex) {
                                tokenProgress[opponent][otherToken] = -1
                                captured = true
                            }
                        }
                    }
                }
            }
        }

        val playerWon = tokenProgress[playerIndex].all { it == 59 }
        if (playerWon) {
            winner = playerIndex
            status = "${players[playerIndex].name} wins the game!"
        } else {
            val extraTurn = roll == 6 || captured || newProgress == 59
            hasRolled = false
            diceValue = null
            if (extraTurn) {
                status = if (captured) "Token captured! ${players[playerIndex].name} rolls again" else "Nice move! ${players[playerIndex].name} rolls again"
            } else {
                nextTurn()
            }
        }
        invalidate()
    }

    private fun nextTurn() {
        hasRolled = false
        diceValue = null
        turnPointer = (turnPointer + 1) % turnOrder.size
        status = "${players[currentPlayer()].name}'s turn — roll the dice"
    }

    private fun resetGame() {
        pendingPass?.let { removeCallbacks(it) }
        pendingPass = null
        for (player in 0..3) for (token in 0..3) tokenProgress[player][token] = -1
        turnOrder = when (playerCount) {
            2 -> listOf(0, 2)
            3 -> listOf(0, 1, 2)
            else -> listOf(0, 1, 2, 3)
        }
        turnPointer = 0
        diceValue = null
        hasRolled = false
        winner = null
        waitingForPass = false
        status = "${players[currentPlayer()].name}'s turn — roll the dice"
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x / density
        val y = event.y / density
        when {
            newGameRect.contains(x, y) -> resetGame()
            playersRect.contains(x, y) -> {
                playerCount = if (playerCount == 4) 2 else playerCount + 1
                resetGame()
            }
            rollRect.contains(x, y) -> rollDice()
            hasRolled -> {
                val hit = hitBoxes.asReversed().firstOrNull { it.bounds.contains(x, y) }
                if (hit != null && hit.player == currentPlayer() && isMovable(hit.player, hit.token)) {
                    makeMove(hit.player, hit.token)
                }
            }
        }
        return true
    }
}
