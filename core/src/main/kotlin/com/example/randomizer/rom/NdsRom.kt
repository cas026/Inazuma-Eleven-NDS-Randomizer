package com.example.randomizer.rom

import java.nio.file.Path
import kotlin.io.path.readBytes

class NdsRom(path: Path) {
    private val bytes: ByteArray  = path.readBytes()
    private val header: NdsHeader = NdsHeader.parse(bytes)
    private val fat: NitroFat
    private val fnt: NitroFnt

    val gameId: String get() = header.gameId
    val title: String  get() = header.title

    init {
        fat = NitroFat(bytes, header.fatOffset, header.fatSize)
        val fntBytes = bytes.copyOfRange(header.fntOffset, header.fntOffset + header.fntSize)
        fnt = NitroFnt(fntBytes)
    }

    fun listFiles(): List<String> = fnt.listFiles()

    fun getFile(path: String): ByteArray {
        val range = fat.range(fnt.fileId(path))
        return bytes.copyOfRange(range.first, range.last + 1)
    }
}
