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
        // Use an explicit model instead of Pair/list destructuring. This avoids
        // Kotlin type-inference ambiguity in the cloud Gradle compiler.
        val triangles = listOf(
            CenterTriangle(0, floatArrayOf(left, top, left, bottom, cx, cy)),
            CenterTriangle(1, floatArrayOf(left, top, right, top, cx, cy)),
            CenterTriangle(2, floatArrayOf(right, top, right, bottom, cx, cy)),
            CenterTriangle(3, floatArrayOf(left, bottom, right, bottom, cx, cy))
        )
        triangles.forEach { triangle ->
            val points = triangle.points
            val path = Path().apply {
                moveTo(points[0], points[1])
                lineTo(points[2], points[3])
                lineTo(points[4], points[5])
                close()
            }
            paint.color = players[triangle.playerIndex].color
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
