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
        stage.scene = Scene(ui.root, 700.0, 560.0)
        stage.title = "Inazuma Eleven NDS Randomizer"
        stage.minWidth = 500.0
        stage.minHeight = 440.0
        stage.show()
    }
}

class RandomizerUi(private val stage: Stage) {

    private var romPath: String = ""

    private val romInfoLabel = Label("No ROM loaded.").apply {
        isWrapText = true
        style = "-fx-text-fill: gray;"
    }

    private val logArea = TextArea().apply {
        isEditable   = false
        isWrapText   = true
        prefRowCount = 5
        font         = Font.font("Monospaced", 11.0)
        HBox.setHgrow(this, Priority.ALWAYS)
    }

    private val seedField = TextField().apply { promptText = "random"; prefWidth = 160.0 }

    private val openRomButton  = Button("Open ROM").apply    { maxWidth = Double.MAX_VALUE }
    private val generateButton = Button("Generate ROM").apply {
        maxWidth = Double.MAX_VALUE
        isDisable = true
    }

    private val statsToggle   = ToggleGroup()
    private val notChangedBtn = RadioButton("Not Changed").apply      { toggleGroup = statsToggle;   isSelected = true }
    private val shuffleBtn    = RadioButton("Shuffle").apply          { toggleGroup = statsToggle }
    private val randomBtn     = RadioButton("Random (Totally)").apply { toggleGroup = statsToggle }

    private val variationField = TextField("0").apply {
        prefWidth = 55.0
        isDisable = true
    }

    private val elementToggle = ToggleGroup()
    private val elemNcBtn     = RadioButton("Not Changed").apply      { toggleGroup = elementToggle; isSelected = true }
    private val elemRevBtn    = RadioButton("Reverse").apply          { toggleGroup = elementToggle }
    private val elemRndBtn    = RadioButton("Random (Totally)").apply { toggleGroup = elementToggle }

    private val genderToggle  = ToggleGroup()
    private val genderNcBtn   = RadioButton("Not Changed").apply      { toggleGroup = genderToggle;  isSelected = true }
    private val genderRevBtn  = RadioButton("Reverse").apply          { toggleGroup = genderToggle }
    private val genderRndBtn  = RadioButton("Random (Totally)").apply { toggleGroup = genderToggle }

    private val positionToggle  = ToggleGroup()
    private val posNcBtn        = RadioButton("Not Changed").apply      { toggleGroup = positionToggle; isSelected = true }
    private val posRevBtn       = RadioButton("Reverse").apply          { toggleGroup = positionToggle }
    private val posRndBtn       = RadioButton("Random (Totally)").apply { toggleGroup = positionToggle }

    private val nameToggle = ToggleGroup()
    private val nameNcBtn  = RadioButton("Not Changed").apply      { toggleGroup = nameToggle; isSelected = true }
    private val nameRndBtn = RadioButton("Random (Totally)").apply { toggleGroup = nameToggle }

    val root: VBox = buildLayout()

    init {
        openRomButton.setOnAction  { pickRom() }
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

        // — Top bar: ROM info | Log | Seed | Buttons —
        val romInfoBox = VBox(6.0).apply {
            children.addAll(
                Label("ROM Information").apply { style = "-fx-font-weight: bold;" },
                romInfoLabel
            )
            minWidth = 160.0
            padding  = Insets(0.0, 12.0, 0.0, 0.0)
        }

        val logBox = VBox(4.0).apply {
            children.addAll(
                Label("Log:"),
                logArea
            )
            HBox.setHgrow(this, Priority.ALWAYS)
            padding = Insets(0.0, 12.0, 0.0, 0.0)
        }

        val seedBox = VBox(4.0).apply {
            children.addAll(Label("Seed:"), seedField)
            padding = Insets(0.0, 12.0, 0.0, 0.0)
        }

        val buttonBox = VBox(8.0, openRomButton, generateButton).apply {
            prefWidth = 140.0
            minWidth  = 140.0
        }

        val topBar = HBox(0.0, romInfoBox, logBox, seedBox, buttonBox).apply {
            alignment = Pos.TOP_LEFT
        }

        // — Player tab —
        val statsRow     = HBox(12.0, rowLabel("Stats:"), notChangedBtn, shuffleBtn, randomBtn)
            .apply { alignment = Pos.CENTER_LEFT }
        val variationRow = HBox(8.0, Label("Variation %:"), variationField, Label("(1–150, recommended ≤ 50)"))
            .apply { alignment = Pos.CENTER_LEFT; padding = Insets(0.0, 0.0, 0.0, labelWidth + 12.0) }
        val elementRow   = HBox(12.0, rowLabel("Element:"),  elemNcBtn,  elemRevBtn,  elemRndBtn)
            .apply { alignment = Pos.CENTER_LEFT }
        val genderRow    = HBox(12.0, rowLabel("Gender:"),   genderNcBtn, genderRevBtn, genderRndBtn)
            .apply { alignment = Pos.CENTER_LEFT }
        val positionRow  = HBox(12.0, rowLabel("Position:"), posNcBtn, posRevBtn, posRndBtn)
            .apply { alignment = Pos.CENTER_LEFT }
        val nameRow      = HBox(12.0, rowLabel("Names:"),    nameNcBtn, nameRndBtn)
            .apply { alignment = Pos.CENTER_LEFT }

        val playerContent = VBox(10.0).apply {
            padding = Insets(16.0)
            children.addAll(statsRow, variationRow, elementRow, genderRow, positionRow, nameRow)
        }

        val tabPane = TabPane().apply {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabs.add(Tab("Player", playerContent))
        }

        return VBox(10.0).apply {
            padding = Insets(16.0)
            children.addAll(topBar, tabPane)
        }
    }

    private fun pickRom() {
        val chooser = FileChooser().apply {
            title = "Select NDS ROM"
            extensionFilters.add(FileChooser.ExtensionFilter("NDS ROM files", "*.nds"))
        }
        val file = chooser.showOpenDialog(stage) ?: return
        romPath = file.absolutePath
        try {
            val rom     = NdsRom(Path.of(romPath))
            val version = GameVersion.fromGameId(rom.gameId)
            if (version != null) {
                romInfoLabel.text  = version.displayName
                romInfoLabel.style = ""
                generateButton.isDisable = false
            } else {
                romInfoLabel.text  = "Unknown game\n(${rom.gameId})"
                romInfoLabel.style = "-fx-text-fill: orange;"
                generateButton.isDisable = true
            }
        } catch (e: Exception) {
            romInfoLabel.text  = "Failed to read ROM:\n${e.message}"
            romInfoLabel.style = "-fx-text-fill: red;"
            generateButton.isDisable = true
        }
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

    private fun selectedPositionMode(): PositionMode = when (positionToggle.selectedToggle) {
        posRevBtn -> PositionMode.REVERSE
        posRndBtn -> PositionMode.RANDOM_TOTALLY
        else      -> PositionMode.NOT_CHANGED
    }

    private fun selectedNameMode(): NameMode = when (nameToggle.selectedToggle) {
        nameRndBtn -> NameMode.RANDOM_TOTALLY
        else       -> NameMode.NOT_CHANGED
    }

    private fun generate() {
        val seed         = seedField.text.toLongOrNull() ?: System.currentTimeMillis()
        val statMode     = selectedStatMode()
        val elementMode  = selectedElementMode()
        val genderMode   = selectedGenderMode()
        val positionMode = selectedPositionMode()
        val nameMode     = selectedNameMode()
        val variance     = variationField.text.toIntOrNull()?.coerceIn(0, 150)?.div(100.0) ?: 0.35

        generateButton.isDisable = true
        openRomButton.isDisable  = true
        logArea.clear()
        log("Seed: $seed")

        Thread {
            try {
                val rom     = NdsRom(Path.of(romPath))
                val version = GameVersion.fromGameId(rom.gameId)
                    ?: error("Unknown game ID '${rom.gameId}'")

                val config = RandomizerConfig(
                    seed = seed, statMode = statMode, variance = variance,
                    elementMode = elementMode, genderMode = genderMode,
                    positionMode = positionMode, nameMode = nameMode
                )

                // — Stats —
                val statPath    = resolveGameFilePath(rom, version, "unitstat.dat")
                val statData    = rom.getFile(statPath)
                val stats       = UnitStatParser(version).parse(statData)
                val randomized  = StatRandomizer(config, version).randomize(stats)
                val newStatData = UnitStatSerializer(version).serialize(statData, randomized)

                when (statMode) {
                    StatMode.NOT_CHANGED -> log("Stats: not changed.")
                    else -> {
                        val storyCount        = StoryPlayers.byVersion[version]?.size ?: 0
                        val realCount         = stats.count { it.maxTotal > 0 }
                        val effectiveStoryVar = minOf(config.storyVariance, variance)
                        log("Stats ($statMode): $realCount players  ($storyCount story ±${(effectiveStoryVar * 100).toInt()}%, others ±${(variance * 100).toInt()}%)")
                    }
                }

                // — Element / Gender / Position / Names —
                val basePath    = resolveGameFilePath(rom, version, "unitbase.dat")
                val baseData    = rom.getFile(basePath)
                val players = UnitBaseParser(version).parse(baseData)
                val randomizedPlayers = NameRandomizer(config)
                    .randomize(PositionRandomizer(config)
                        .randomize(ElementGenderRandomizer(config).randomize(players)))
                val newBaseData = UnitBaseSerializer(version).serialize(baseData, randomizedPlayers)

                if (elementMode  != FieldMode.NOT_CHANGED)    log("Element:  $elementMode")
                if (genderMode   != FieldMode.NOT_CHANGED)    log("Gender:   $genderMode")
                if (positionMode != PositionMode.NOT_CHANGED) log("Position: $positionMode")
                if (nameMode     != NameMode.NOT_CHANGED)     log("Names:    $nameMode")

                // Chain patches: unitbase on top of stat patch.
                val patchedRom = rom.patchFile(basePath, newBaseData, rom.patchFile(statPath, newStatData))

                val input  = Path.of(romPath)
                val output = input.resolveSibling(
                    input.fileName.toString().removeSuffix(".nds") + "_randomized.nds"
                )
                output.writeBytes(patchedRom)
                log("Done! → $output")
            } catch (e: Exception) {
                log("Error: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                Platform.runLater {
                    generateButton.isDisable = false
                    openRomButton.isDisable  = false
                }
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun log(message: String) = Platform.runLater { logArea.appendText("$message\n") }
}
