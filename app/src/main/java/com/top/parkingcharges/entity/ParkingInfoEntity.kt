package com.top.parkingcharges.entity

/**
 * * DA:   为显示屏的地址，取值范围为 0x00 - 0xFF。
 * VR:   描述了协议版本号，目前支持版本 100 和版本 200。
 * PN:   是包的序列，在传输超过 255 个字节的数据时，需要分包传输，PN 表示了包的序列，每次交互完之后自增 1，设置为最大值 0XFFFF 时表示当前包是最后一个包。在传输小于 255 个字节的数据时，该值应该设置为 0XFFFF。
 * CMD:  该字段描述了该包的作用，显示屏通过这个值完成不同的功能服务。
 * DL:   该字段用来描述 DATA 的数据长度，当 VR 为 100 时，DL 只有 1 个字节，最大取值为 255。当 VR 为 200 时，DL 为 2 个字节组成 16 位数据,最大取值为 65535。
 * DATA: 是参数数据，每条指令携带的参数和长度是不同的，详解参见后指令集章节。
 * CRC:  数据包的校验码。参与校验的字段是从 DA 到 DATA 的最后一个字节。校验算法采用 CRC16，
 *
 *
 * ### 参数数据拆解
参数数据格式：SAVE_FLAG + TEXT_CONTEXT_NUMBER + TEXT_CONTEXT[…]+ VF+ VTL + VT[...] 具体说明参考文档；
拆解：

 * SAVE_FLAG = 00              //为保存标志，取值为 1 时表示下载到存储区，取值为 0 时表示下载到临时区，频繁修改的内容建议下载到临时区。
 * TEXT_CONTEXT_NUMBER = 04    //为文本参数数量，即几行文本。目前版本最大支持 4 行。
 * TEXT_CONTEXT:               //为文本参数，每个文本参数控制一行，用 0X0D 分开，最后一个文本参数用 0X00 结束，最多 4 个文本参数。(本报文含4个文本参数)
 *
 * 文本参数的结构为 LID + DM + DS + DT + DR + TC[4]+TL +TEXT[...]+0X0D/0X00 各参数取值含义如下。
 *
 * ### 语音部分：
0A 1D BB A6 41 41 50 36 33 37 35 2C C1 D9 CA B1 B3 B5 2C D7 A3 C4 FA D2 BB C2 B7 CB B3 B7 E7 00
1. VF = 0A   //语音标志，固定取值为 0X0A。
2. VTL = 1D  //语音文本长度。
3. VOICE = BB A6 41 41 50 36 33 37 35 2C C1 D9 CA B1 B3 B5 2C D7 A3 C4 FA D2 BB C2 B7 CB B3 B7 E7   //语音文本内容，最大 64 字节。当前值：“沪AAP6375,临时车,祝您一路顺风”
4. 结束标志 = 00 //报文结束符
 */

data class ParkingInfoEntity(
    val da: String,
    val vr: Int,
    val pn: String,
    val cmd: String,
    val dl: Int,
    val saveFlag: Int,
    val textContentNumber: Int,
    val textContentList: List<TextContentEntity>,
    val vf: String,
    val vtl: Int,
    val voiceContent: String,
    val voiceEndFlag: String,
    val crc: String
)

/**
 * 1. LID = 00  //为显示行号。0 表示第 1 行，1 表示第 2 行，以此类推。
2. DM = 15   //为显示模式 0x15 连续左移
3. DS = 01   //为显示速度，建议取值为 0；
4. DT = 19   //停留时间，单位为秒，最大为 255 秒；
5. DR = 00   //为显示次数，0 为无限循环显示。
6. TC = FF 00 00 00  //为文本颜色，32 位数据类型，存储结构为 RGBA，R 为红色分量，G 为绿色分量，B 为蓝色分量，A 为透明值目前保留为 0，每个颜色分量占用一个字节即 8 位。
7. TL = 08  //为文本长度。
8. TEXT = D2 BB C2 B7 CB B3 B7 E7  //为文本内容，最大 32 字节。GB2312编码, 当前值： “一路顺风”
9. 结束标志 = 0D
 */
data class TextContentEntity(
    val lid: String,
    val dm: String,
    val ds: String,
    val dt: Int,
    val dr: Int,
    val tc: String,
    val textLength: Int,
    val text: String,
    val endFlag: String
)