package com.example.randomizer.rom

internal fun ByteArray.readUInt8(offset: Int): Int = this[offset].toInt() and 0xFF

internal fun ByteArray.readInt16LE(offset: Int): Short {
    val lo = this[offset].toInt() and 0xFF
    val hi = this[offset + 1].toInt() and 0xFF
    return ((hi shl 8) or lo).toShort()
}

internal fun ByteArray.readUInt16LE(offset: Int): Int {
    val lo = this[offset].toInt() and 0xFF
    val hi = this[offset + 1].toInt() and 0xFF
    return (hi shl 8) or lo
}

internal fun ByteArray.readInt32LE(offset: Int): Int {
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    val b2 = this[offset + 2].toInt() and 0xFF
    val b3 = this[offset + 3].toInt() and 0xFF
    return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
}

internal fun ByteArray.readString(offset: Int, maxLen: Int): String {
    var len = 0
    while (len < maxLen && this[offset + len] != 0.toByte()) len++
    return String(this, offset, len, Charsets.ISO_8859_1)
}

internal fun ByteArray.writeUInt8(offset: Int, value: Int) {
    this[offset] = (value and 0xFF).toByte()
}

internal fun ByteArray.writeInt16LE(offset: Int, value: Int) {
    this[offset]     = (value and 0xFF).toByte()
    this[offset + 1] = ((value ushr 8) and 0xFF).toByte()
}

internal fun ByteArray.writeString(offset: Int, maxLen: Int, value: String) {
    val bytes = value.toByteArray(Charsets.ISO_8859_1)
    val len = minOf(bytes.size, maxLen)
    bytes.copyInto(this, offset, 0, len)
    for (i in len until maxLen) this[offset + i] = 0
}
