package com.top.parkingcharges.entity

/**
 * 1. SF = 01     //: 显示标志，1 为显示，0 为不显示。
2. EM = 00     //: 进入模式，保留赋值为 0(无操作)。
3. ETM = 01    //: 退出模式，保留赋值为 0(无操作)。
4. ST = 78     //:界面的显示时间，单位为秒，0 为一直显示。
5. NI = 00     //:下一个界面的索引号，目前保留取值为 0.
6. VEN = 81    //:语音播报开关，为 80 不播报语音，为 81 播报语音。
7. TL =  76    //:显示文本长度。当前值：118
8. TEXT
 */

data class PayInfoEntity(
    val da: String,
    val vr: Int,
    val pn: String,
    val cmd: String,
    val dl: Int,
    val payContentEntity: PayContentEntity,
    val qrCode: String,
    val crc: String
)

data class PayContentEntity(
    val sf: String,
    val em: String,
    val etm: String,
    val st: Int,
    val ni: String,
    val ven: String,
    val tl: Int,
    val text: String
)