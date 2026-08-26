package com.example.randomizer

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

fun main(args: Array<String>) = Application.launch(App::class.java, *args)

class App : Application() {
    override fun start(stage: Stage) {
        stage.scene = Scene(StackPane(Label("Inazuma Eleven Randomizer")), 800.0, 600.0)
        stage.title = "Inazuma Eleven NDS Randomizer"
        stage.show()
    }
}
