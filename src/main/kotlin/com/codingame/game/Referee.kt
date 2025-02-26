package com.codingame.game

import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.AbstractReferee
import com.codingame.gameengine.core.SoloGameManager
import com.codingame.gameengine.module.entities.GraphicEntityModule
import com.codingame.gameengine.module.entities.Sprite
import com.google.inject.Inject
import kotlin.random.Random

val variants = mapOf(
    "F" to listOf("......", "..OO..", ".O....", ".O....", "..OO..", ".O....", ".O....", "......", "......",),
    "H" to listOf("......", "......", ".O....", ".O....", "..OO..", ".O..O.", ".O..O.", "......", "......",),
    "I" to listOf("...", "...", ".O.", ".O.", "...", ".O.", ".O.", "...", ".O.", ".O.", "...", ".O.", ".O.", "...", "..."),
    "J" to listOf("......", "......", "....O.", "....O.", "......", ".O..O.", ".O..O.", "..OO..", "......"),
    "L" to listOf("......", "......", ".O....", ".O....", "......", ".O....", ".O....", "......", ".O....", ".O....", "..OO..", "......"),
    "N" to listOf("......", "......", ".O....", ".O....", "..OO..", "....O.", "....O.", "......", "....O.", "....O.", "......", "......",),
    "O" to listOf("......", "..OO..", ".O..O.", ".O..O.", "..OO..", "......"),
    "P" to listOf("......","..OO..","....O.","....O.","..OO..",".O....",".O....","......","......"),
    "R" to listOf(".........", "..OO.....", "....O....", "....O....", ".....OO..", "....O....", "....O....", ".........", "........."),
    "T" to listOf(".........", "..OO.OO..", "....O....", "....O....", ".........", "....O....", "....O....", ".........", "........."),
    "U" to listOf(".........", ".........", ".O.....O.", ".O.....O.", "..OO.OO..", "........."),
    "V" to listOf(".........", ".........", ".......O.", ".......O.", ".........", ".......O.", ".......O.", "..OO.OO..", "........."),
    "W" to listOf(".........", ".....OO..", "....O....", "....O....", "..OO.....", ".O.......", ".O.......", ".........", "........."),
    "X" to listOf(".........", ".........", "....O....", "....O....", "..OO.OO..", "....O....", "....O....", ".........", "........."),
    "Y" to listOf("......", "......", ".O....", ".O....", "..OO..", ".O....", ".O....", "......", ".O....", ".O....", "......", "......"),
    "Z" to listOf(".........", "..OO.....", "....O....", "....O....", ".........", "....O....", "....O....", ".....OO..", ".........")
)

private fun transformTetrastick(shape: List<String>, flip: Boolean, rightRotations: Int): List<String> {
    var transformed = shape
    if (flip) { transformed = transformed.map { it.reversed() } }
    repeat(rightRotations % 4) { transformed = rotateRight(transformed) }
    return transformed
}

private fun rotateRight(shape: List<String>): List<String> {
    val height = shape.size
    val width = shape[0].length
    val rotated = MutableList(width) { StringBuilder(height) }
    for (y in shape.indices) { for (x in shape[y].indices) { rotated[x].insert(0, shape[y][x]) } }
    return rotated.map { it.toString() }
}

private fun Array<Array<Char>>.chunked(chunkSize: Int = 3): List<List<String>> {
    val windows = mutableListOf<List<String>>()
    for (y in indices step chunkSize) {
        for (x in get(0).indices step chunkSize) {
            windows += (0..<chunkSize).map{ dy -> (0..<chunkSize).map { this[y + dy][x + it] }.joinToString("") }
        }
    }
    return windows
}

class Referee : AbstractReferee() {

    @Inject
    private lateinit var gameManager: SoloGameManager<Player>

    @Inject
    private lateinit var graphicEntityModule: GraphicEntityModule

    private var remainingTiles: List<Char> = listOf()
    private var board: Array<Array<Char>> = Array(18) { Array(18) { '.' } }
    private val h get() = board.size
    private val w get() = board[0].size

    data class Placement(val id: String, val flip: Boolean, val rotate: Int, val x: Int, val y: Int)

    override fun init() {
        gameManager.firstTurnMaxTime = 2000
        gameManager.turnMaxTime = 50
        remainingTiles = gameManager.testCaseInput[0].split("").mapNotNull { it.getOrNull(0) }
        val placed = if (gameManager.testCaseInput.size == 1) emptyList() else gameManager.testCaseInput[1].split("|").map {
            val data = it.split(" ")
            Placement(data[0], data[1] == "1", data[2].toInt(), data[4].toInt(), data[3].toInt())
        }
        forcePutOnBoard(placed)
        initVisual(placed)
    }

    private fun forcePutOnBoard(placed: List<Placement>) {
        for ((tileName, flip, rotate, x, y) in placed) {
            val tetrastickDefaultShape = variants[tileName] ?: throw IllegalStateException("Won't happen")
            val tile = transformTetrastick(tetrastickDefaultShape, flip, rotate)
            outer@ for (dy in tile.indices) {
                for (dx in tile[0].indices) {
                    if (tile[dy][dx] == 'O') board[y * 3 + dy][x * 3 + dx] = tileName[0]
                }
            }
        }
    }

    override fun gameTurn(turn: Int) {

        // input processing
        gameManager.player.sendInputLine(remainingTiles.size.toString())
        gameManager.player.sendInputLine(remainingTiles.joinToString(" "))
        gameManager.player.sendInputLine("${board.size} ${board[0].size}")
        board.forEach { gameManager.player.sendInputLine(it.joinToString("")) }

        try {
            // execution
            gameManager.player.execute()

            // processing outputs
            val output = gameManager.player.outputs[0].split(" ")

            // name, flip, rotations, x , y
            if (output.size != 5) { gameManager.loseGame("Invalid output - check game statement"); return }
            if (output[0] !in variants.keys) { gameManager.loseGame("Tetrastick ${output[0]} unavailable"); return }
            if (output[0][0] !in remainingTiles) { gameManager.loseGame("Tetrastick ${output[0]} was already used"); return }
            if (output[1].toIntOrNull() == null || output[1].toInt() !in 0..1) { gameManager.loseGame("Flip (2nd part of output) can only have values of 0 or 1"); return }
            if (output[2].toIntOrNull() == null || output[2].toInt() !in 0..3) { gameManager.loseGame("Rotations (3rd part of output) can only have values of 0-3"); return }
            if (output[3].toIntOrNull() == null || output[3].toInt() !in 0..5) { gameManager.loseGame("Row (4th part of output) can only have values of 0-5"); return }
            if (output[4].toIntOrNull() == null || output[4].toInt() !in 0..5) { gameManager.loseGame("Column (5th part of output) can only have values of 0-5"); return }

            val tileName = output[0]
            val flip = output[1].toInt() == 1
            val rightRotations = output[2].toInt()
            val y = 3 * output[3].toInt()
            val x = 3 * output[4].toInt()

            // check can place
            val tetrastickDefaultShape = variants[tileName] ?: throw IllegalStateException("Won't happen")
            val tile = transformTetrastick(tetrastickDefaultShape, flip, rightRotations)

            var incorrectPlacement = false
            outer@ for (dy in tile.indices) {
                for (dx in tile[0].indices) {
                    if (y + dy !in 0..<h || x + dx !in 0..<w) { incorrectPlacement = true; break@outer } // out of bounds error
                    if (tile[dy][dx] == 'O' && board[y + dy][x + dx] != '.') { incorrectPlacement = true; break@outer } // tetra-sticks clash error
                    if (tile[dy][dx] == 'O') board[y + dy][x + dx] = tileName[0]
                }
            }

            // check intersections
            for (window in board.chunked()) {
                val val1 = window[0][1]
                val val2 = window[1][0]
                if (val1 == '.' || val2 == '.' || val1 == val2) continue
                if (val1 == window[2][1] && val2 == window[1][2]) incorrectPlacement = true
            }

            // run visualization
            visualize(tileName, flip, rightRotations, x / 3, y / 3)
            if (incorrectPlacement) { gameManager.loseGame("Invalid placement"); return }
            remainingTiles = remainingTiles - tileName[0]
        } catch (e: AbstractPlayer.TimeoutException) {
            gameManager.loseGame("Timeout")
            return
        } catch (e: Exception) {
            e.printStackTrace()
            gameManager.loseGame("Invalid player output")
            return
        }

        if (remainingTiles.isEmpty()) {
            gameManager.winGame("Congrats!")
        }
    }

    private lateinit var tetraStickSprites: Map<String, Sprite>

    private val initialPositions = listOf(
        109 to 71,
        426 to 93,
        120 to 379,
        426 to 531,
        179 to 666,
        482 to 800,
        85 to 866,
        1022 to 36,
        1183 to 971,
        1468 to 93,
        1693 to 270,
        1468 to 380,
        303 to 254,
        1771 to 106,
        1420 to 866,
        1574 to 541
    ).shuffled().toMutableList()

    private fun initVisual(placed: List<Placement>) {
        graphicEntityModule.createSprite().setImage("background.jpg")
        val allTiles = remainingTiles + placed.map { it.id[0] }
        tetraStickSprites = allTiles.associate { c ->
            val pos = initialPositions.removeAt(0)
            c.toString() to graphicEntityModule.createSprite().setImage("$c.png")
                .setAnchor(0.5)
                .setScale(0.6)
                .setRotation(Random.nextDouble(2 * Math.PI))
                .setX(pos.first)
                .setY(pos.second)
        }
        for (tile in placed) {
            visualize(tile.id, tile.flip, tile.rotate, tile.x, tile.y)
        }
    }

    private var finalZIndex = 900
    private fun visualize(tileName: String, flip: Boolean, rotate: Int, x: Int, y: Int) {
        val sprite = tetraStickSprites[tileName] ?: throw IllegalStateException("Error in Referee - contact author")

        val anchor = when(flip to rotate) {
            false to 0 -> 0.0 to 0.0
            false to 1 -> 0.0 to 1.0
            false to 2 -> 1.0 to 1.0
            false to 3 -> 1.0 to 0.0
            true to 0 -> 1.0 to 0.0
            true to 1 -> 1.0 to 1.0
            true to 2 -> 0.0 to 1.0
            true to 3 -> 0.0 to 0.0
            else -> 0.0 to 0.0
        }
        sprite.setZIndex(1000)
        graphicEntityModule.commitEntityState(0.01, sprite)
        sprite
            .setX(620 + x * 130)
            .setY(200 + y * 130)
            .setAnchorX(anchor.first)
            .setAnchorY(anchor.second)
            .setScaleX(if (flip) -1.0 else 1.0)
            .setScaleY(1.0)
            .setRotation(Math.toRadians(90.0 * rotate))
        graphicEntityModule.commitEntityState(0.98, sprite)
        sprite.setZIndex(finalZIndex++)
        graphicEntityModule.commitEntityState(0.99, sprite)
    }
}
