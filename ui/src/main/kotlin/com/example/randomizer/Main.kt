package com.example.randomizer

import com.example.randomizer.data.*
import com.example.randomizer.randomizer.*
import com.example.randomizer.rom.NdsRom
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlin.io.path.writeBytes
import java.nio.file.Path

fun main(args: Array<String>) = Application.launch(App::class.java, *args)

class App : Application() {
    override fun start(stage: Stage) {
        val ui = RandomizerUi(stage)
        stage.scene = Scene(ui.root, 700.0, 640.0)
        stage.title = "Inazuma Eleven NDS Randomizer"
        stage.minWidth = 500.0
        stage.minHeight = 540.0
        stage.show()
    }
}

class RandomizerUi(private val stage: Stage) {

    private val romPathField   = TextField().apply {
        isEditable = false
        promptText = "Geen ROM gekozen…"
        HBox.setHgrow(this, Priority.ALWAYS)
    }
    private val browseButton   = Button("Bladeren…")
    private val seedField      = TextField().apply { promptText = "willekeurig"; prefWidth = 200.0 }

    private val statsToggle    = ToggleGroup()
    private val notChangedBtn  = RadioButton("Not Changed").apply { toggleGroup = statsToggle; isSelected = true }
    private val shuffleBtn     = RadioButton("Shuffle").apply { toggleGroup = statsToggle }
    private val randomBtn      = RadioButton("Random (Totally)").apply { toggleGroup = statsToggle }

    private val variationField = TextField("0").apply {
        prefWidth = 55.0
        isDisable = true
    }

    private val elementToggle  = ToggleGroup()
    private val elemNcBtn      = RadioButton("Not Changed").apply { toggleGroup = elementToggle; isSelected = true }
    private val elemRevBtn     = RadioButton("Reverse").apply { toggleGroup = elementToggle }
    private val elemRndBtn     = RadioButton("Random (Totally)").apply { toggleGroup = elementToggle }

    private val genderToggle   = ToggleGroup()
    private val genderNcBtn    = RadioButton("Not Changed").apply { toggleGroup = genderToggle; isSelected = true }
    private val genderRevBtn   = RadioButton("Reverse").apply { toggleGroup = genderToggle }
    private val genderRndBtn   = RadioButton("Random (Totally)").apply { toggleGroup = genderToggle }

    private val generateButton = Button("Genereer ROM").apply { maxWidth = Double.MAX_VALUE }
    private val logArea        = TextArea().apply {
        isEditable = false
        isWrapText = true
        font = Font.font("Monospaced", 12.0)
        VBox.setVgrow(this, Priority.ALWAYS)
    }

    val root: VBox = buildLayout()

    init {
        browseButton.setOnAction   { pickRom() }
        generateButton.setOnAction { generate() }
        statsToggle.selectedToggleProperty().addListener { _, _, selected ->
            when (selected) {
                notChangedBtn -> variationField.isDisable = true
                shuffleBtn    -> { variationField.isDisable = false; variationField.text = "0" }
                randomBtn     -> { variationField.isDisable = false; variationField.text = "35" }
            }
        }
    }

    private fun buildLayout(): VBox {
        val labelWidth = 80.0
        fun rowLabel(text: String) = Label(text).apply { minWidth = labelWidth }

        val romRow       = HBox(8.0, romPathField, browseButton).apply { alignment = Pos.CENTER_LEFT }
        val seedRow      = HBox(8.0, Label("Seed:"), seedField).apply { alignment = Pos.CENTER_LEFT }
        val statsRow     = HBox(12.0, rowLabel("Stats:"), notChangedBtn, shuffleBtn, randomBtn)
            .apply { alignment = Pos.CENTER_LEFT }
        val variationRow = HBox(8.0, Label("Variation %:"), variationField, Label("(0–99, aanbevolen ≤ 50)"))
            .apply { alignment = Pos.CENTER_LEFT; padding = Insets(0.0, 0.0, 0.0, labelWidth + 12.0) }
        val elementRow   = HBox(12.0, rowLabel("Element:"), elemNcBtn, elemRevBtn, elemRndBtn)
            .apply { alignment = Pos.CENTER_LEFT }
        val genderRow    = HBox(12.0, rowLabel("Gender:"), genderNcBtn, genderRevBtn, genderRndBtn)
            .apply { alignment = Pos.CENTER_LEFT }

        return VBox(10.0).apply {
            padding = Insets(16.0)
            children.addAll(
                Label("ROM-bestand:"),
                romRow,
                seedRow,
                statsRow,
                variationRow,
                elementRow,
                genderRow,
                generateButton,
                Separator(),
                Label("Log:"),
                logArea
            )
        }
    }

    private fun pickRom() {
        val chooser = FileChooser().apply {
            title = "Kies NDS ROM"
            extensionFilters.add(FileChooser.ExtensionFilter("NDS ROM-bestanden", "*.nds"))
        }
        chooser.showOpenDialog(stage)?.let { romPathField.text = it.absolutePath }
    }

    private fun selectedStatMode(): StatMode = when (statsToggle.selectedToggle) {
        shuffleBtn -> StatMode.SHUFFLE
        randomBtn  -> StatMode.RANDOM_TOTALLY
        else       -> StatMode.NOT_CHANGED
    }

    private fun selectedElementMode(): FieldMode = when (elementToggle.selectedToggle) {
        elemRevBtn -> FieldMode.REVERSE
        elemRndBtn -> FieldMode.RANDOM_TOTALLY
        else       -> FieldMode.NOT_CHANGED
    }

    private fun selectedGenderMode(): FieldMode = when (genderToggle.selectedToggle) {
        genderRevBtn -> FieldMode.REVERSE
        genderRndBtn -> FieldMode.RANDOM_TOTALLY
        else         -> FieldMode.NOT_CHANGED
    }

    private fun generate() {
        val romPath     = romPathField.text.ifBlank { return log("Kies eerst een ROM-bestand.") }
        val seed        = seedField.text.toLongOrNull() ?: System.currentTimeMillis()
        val statMode    = selectedStatMode()
        val elementMode = selectedElementMode()
        val genderMode  = selectedGenderMode()
        val variance    = variationField.text.toIntOrNull()?.coerceIn(0, 99)?.div(100.0) ?: 0.35

        generateButton.isDisable = true
        logArea.clear()
        log("Seed: $seed")

        Thread {
            try {
                val rom     = NdsRom(Path.of(romPath))
                val version = GameVersion.fromGameId(rom.gameId)
                    ?: error("Onbekende game ID '${rom.gameId}'")
                log("ROM: ${rom.title}  (${rom.gameId} — $version)")

                val config = RandomizerConfig(
                    seed = seed, statMode = statMode, variance = variance,
                    elementMode = elementMode, genderMode = genderMode
                )

                // — Stats —
                val statPath   = resolveGameFilePath(rom, version, "unitstat.dat")
                val statData   = rom.getFile(statPath)
                val stats      = UnitStatParser(version).parse(statData)
                val randomized = StatRandomizer(config, version).randomize(stats)
                val newStatData = UnitStatSerializer(version).serialize(statData, randomized)

                when (statMode) {
                    StatMode.NOT_CHANGED -> log("Stats: ongemoeid gelaten.")
                    else -> {
                        val storyCount        = StoryPlayers.byVersion[version]?.size ?: 0
                        val realCount         = stats.count { it.maxTotal > 0 }
                        val effectiveStoryVar = minOf(config.storyVariance, variance)
                        log("Stats ($statMode): $realCount spelers  ($storyCount story ±${(effectiveStoryVar * 100).toInt()}%, overige ±${(variance * 100).toInt()}%)")
                    }
                }

                // — Element / Gender —
                val basePath    = resolveGameFilePath(rom, version, "unitbase.dat")
                val baseData    = rom.getFile(basePath)
                val players     = UnitBaseParser(version).parse(baseData)
                val newBaseData = UnitBaseSerializer(version).serialize(baseData, ElementGenderRandomizer(config).randomize(players))

                if (elementMode != FieldMode.NOT_CHANGED) log("Element: $elementMode")
                if (genderMode  != FieldMode.NOT_CHANGED) log("Gender:  $genderMode")

                // Chain patches: unitbase on top of stat patch.
                val patchedRom = rom.patchFile(basePath, newBaseData, rom.patchFile(statPath, newStatData))

                val input  = Path.of(romPath)
                val output = input.resolveSibling(
                    input.fileName.toString().removeSuffix(".nds") + "_randomized.nds"
                )
                output.writeBytes(patchedRom)
                log("Klaar! → $output")
            } catch (e: Exception) {
                log("Fout: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                Platform.runLater { generateButton.isDisable = false }
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun log(message: String) = Platform.runLater { logArea.appendText("$message\n") }
}
