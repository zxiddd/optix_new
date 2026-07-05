package com.example.services

object EscPosConstants {
    val ESC: Byte = 0x1B
    val GS: Byte = 0x1D
    val LF: Byte = 0x0A

    val ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    val ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    val ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)

    val FONT_SIZE_NORMAL = byteArrayOf(GS, 0x21, 0x00)
    val FONT_SIZE_DOUBLE_HEIGHT = byteArrayOf(GS, 0x21, 0x01)
    val FONT_SIZE_DOUBLE_WIDTH = byteArrayOf(GS, 0x21, 0x10)
    val FONT_SIZE_BIG = byteArrayOf(GS, 0x21, 0x11) // Double width + Double height

    val BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)

    val INIT = byteArrayOf(ESC, 0x40)
    val PAPER_CUT = byteArrayOf(GS, 0x56, 0x01)
    
    val FEED_LINE = byteArrayOf(LF)
}
