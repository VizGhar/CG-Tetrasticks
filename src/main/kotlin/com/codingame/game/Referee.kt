package com.codingame.game

import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.AbstractReferee
import com.codingame.gameengine.core.SoloGameManager
import com.codingame.gameengine.module.entities.GraphicEntityModule
import com.codingame.gameengine.module.entities.Sprite
import com.google.inject.Inject
import kotlin.random.Random

val variants = mapOf(
    "F" to listOf("OOO", "O..", "OOO", "O..", "O.."),
    "H" to listOf("O..", "O..", "OOO", "O.O", "O.O"),
    "I" to listOf("O", "O", "O", "O", "O", "O", "O", "O", "O"),
    "J" to listOf("..O", "..O", "O.O", "O.O", "OOO"),
    "L" to listOf("O..", "O..", "O..", "O..", "O..", "O..", "OOO"),
    "N" to listOf("O..", "O..", "OOO", "..O", "..O", "..O", "..O"),
    "O" to listOf("OOO", "O.O", "OOO"),
    "P" to listOf("OOO", "..O", "OOO", "O..", "O.."),
    "R" to listOf("OOO..", "..O..", "..OOO", "..O..", "..O.."),
    "T" to listOf("OOOOO", "..O..", "..O..", "..O..", "..O.."),
    "U" to listOf("O...O", "O...O", "OOOOO"),
    "V" to listOf("....O", "....O", "....O", "....O", "OOOOO"),
    "W" to listOf("..OOO", "..O..", "OOO..", "O....", "O...."),
    "X" to listOf("..O..", "..O..", "OOOOO", "..O..", "..O.."),
    "Y" to listOf("O..", "O..", "OOO", "O..", "O..", "O..", "O.."),
    "Z" to listOf("OOO..", "..O..", "..O..", "..O..", "..OOO")
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

class Referee : AbstractReferee() {

    @Inject
    private lateinit var gameManager: SoloGameManager<Player>

    @Inject
    private lateinit var graphicEntityModule: GraphicEntityModule

    private var remainingTiles: List<Char> = listOf()
    private var board: Array<Array<Char>> = Array(11) { y -> Array(11) { x -> if (x % 2 == 1 && y % 2 == 1) ' ' else '.' } }

    override fun init() {
        gameManager.firstTurnMaxTime = 2000
        gameManager.turnMaxTime = 50
        remainingTiles = gameManager.testCaseInput[0].split("").mapNotNull { it.getOrNull(0) }
        initVisual()
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
            if (output.size != 5) { gameManager.loseGame("You should provide exactly 5 values in your output - check game statement"); return }
            if (output[0] !in variants.keys) { gameManager.loseGame("No tetrastick with name ${output[0]} exists"); return }
            if (output[0][0] !in remainingTiles) { gameManager.loseGame("The tetrastick with name ${output[0]} was already used"); return }
            if (output[1].toIntOrNull() == null || output[1].toInt() !in 0..1) { gameManager.loseGame("Flip (2nd part of output) can only have values of 0 or 1"); return }
            if (output[2].toIntOrNull() == null || output[2].toInt() !in 0..3) { gameManager.loseGame("Rotations (3rd part of output) can only have values of 0-3"); return }
            if (output[3].toIntOrNull() == null || output[3].toInt() !in 0..5) { gameManager.loseGame("Row (4th part of output) can only have values of 0-5"); return }
            if (output[4].toIntOrNull() == null || output[4].toInt() !in 0..5) { gameManager.loseGame("Column (5th part of output) can only have values of 0-5"); return }

            val tileName = output[0]
            val flip = output[1].toInt() == 1
            val rightRotations = output[2].toInt()
            val y = 2 * output[3].toInt()
            val x = 2 * output[4].toInt()

            // check can place
            val tetrastickDefaultShape = variants[tileName] ?: throw IllegalStateException("Won't happen")
            val tetrastickTransformedShape = transformTetrastick(tetrastickDefaultShape, flip, rightRotations)

            var incorrectPlacement = false
            outer@ for (ty in tetrastickTransformedShape.indices) {
                for (tx in tetrastickTransformedShape[0].indices) {
                    if ((tx + ty) % 2 == 0) {
                        val current = board[y + ty][x + tx]
                        board[y + ty][x + tx] = when {
                            tetrastickTransformedShape[ty][tx] != 'O' -> current
                            tx % 2 == 1 && ty % 2 == 1 -> ' '
                            current != '.' -> '/'
                            else -> tileName[0].lowercaseChar()
                        }
                        continue
                    }
                    if (tetrastickTransformedShape[ty][tx] == '.') continue
                    if (board[y + ty][x + tx] != '.') { incorrectPlacement = true; break@outer }
                    board[y + ty][x + tx] = tileName[0]
                }
            }

            // run visualization
            visualize(tileName, flip, rightRotations, x / 2, y / 2)
            if (incorrectPlacement) { gameManager.loseGame("Place is already taken"); return }
            remainingTiles = remainingTiles - tileName[0]
        } catch (e: AbstractPlayer.TimeoutException) {
            gameManager.loseGame("Timeout")
            return
        } catch (e: Exception) {
            gameManager.loseGame("Invalid player output. Check game statement")
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

    private fun initVisual() {
        graphicEntityModule.createSprite().setImage("background.jpg")
        tetraStickSprites = remainingTiles.associate { c ->
            val pos = initialPositions.removeAt(0)
            c.toString() to graphicEntityModule.createSprite().setImage("$c.png")
                .setAnchor(0.5)
                .setScale(0.6)
                .setRotation(Random.nextDouble(2 * Math.PI))
                .setX(pos.first)
                .setY(pos.second)
        }
    }

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
        sprite.setZIndex(900)
        graphicEntityModule.commitEntityState(0.99, sprite)
    }
}
