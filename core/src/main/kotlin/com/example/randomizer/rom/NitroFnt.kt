package com.example.randomizer.rom

class NitroFnt(private val fnt: ByteArray) {
    private val pathToId: Map<String, Int>

    init {
        val numDirs      = fnt.readUInt16LE(6)
        val entryOffsets = IntArray(numDirs) { fnt.readInt32LE(it * 8) }
        val firstFileIds = IntArray(numDirs) { fnt.readUInt16LE(it * 8 + 4) }

        val map = mutableMapOf<String, Int>()
        traverse(0, "", entryOffsets, firstFileIds, map)
        pathToId = map
    }

    private fun traverse(
        dirIndex: Int,
        prefix: String,
        entryOffsets: IntArray,
        firstFileIds: IntArray,
        map: MutableMap<String, Int>
    ) {
        var pos    = entryOffsets[dirIndex]
        var fileId = firstFileIds[dirIndex]

        while (true) {
            val lengthType = fnt[pos].toInt() and 0xFF
            if (lengthType == 0) break

            val nameLen = lengthType and 0x7F
            val isDir   = (lengthType and 0x80) != 0
            val name    = String(fnt, pos + 1, nameLen, Charsets.US_ASCII)
            pos += 1 + nameLen

            if (isDir) {
                // Directory IDs in the entry list start at 0xF000; subtract to get table index
                val subDirIndex = fnt.readUInt16LE(pos) - 0xF000
                pos += 2
                traverse(subDirIndex, "$prefix$name/", entryOffsets, firstFileIds, map)
            } else {
                map["$prefix$name"] = fileId++
            }
        }
    }

    fun fileId(path: String): Int =
        pathToId[path] ?: error("File not found in ROM filesystem: $path")

    fun listFiles(): List<String> = pathToId.keys.sorted()
}
