# ParkingCharges
parking
# 指令0XE5


BASE64:

> AMj//+UvAQEAAXgAgXYgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgILumQjk5MUg1LM2js7Uw0KHKsTQ1t9bW0zE4w+ssx+u9ybfRNdSqQk2yAAAAAAAAAD4AAAAoAAAAHQAAAOP///8BAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA////AAAAAAD+x3v4gp1CCLq6iui6r8LoupQy6IJUkgj+qqv4ALMIAFbizvjw+10YLpX9OHA4ODhLpaWoEIiqgI7L5FBFqjuga3s1cFDq6KgOVMJguV3LSKdXT9AA12ig/ndqoIKk6KC6Z3+4upqGeLoK7uiCkRzw/kRNwK6c


## 字节数组：

    00 C8 FF FF E5 2F 01 01 00 01 78 00 81 76 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 BB A6 42 39 39 31 48 35 2C CD A3 B3 B5 30 D0 A1 CA B1 34 35 B7 D6 D6 D3 31 38 C3 EB 2C C7 EB BD C9 B7 D1 35 D4 AA 42 4D B2 00 00 00 00 00 00 00 3E 00 00 00 28 00 00 00 1D 00 00 00 E3 FF FF FF 01 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF 00 00 00 00 00 FE C7 7B F8 82 9D 42 08 BA BA 8A E8 BA AF C2 E8 BA 94 32 E8 82 54 92 08 FE AA AB F8 00 B3 08 00 56 E2 CE F8 F0 FB 5D 18 2E 95 FD 38 70 38 38 38 4B A5 A5 A8 10 88 AA 80 8E CB E4 50 45 AA 3B A0 6B 7B 35 70 50 EA E8 A8 0E 54 C2 60 B9 5D CB 48 A7 57 4F D0 00 D7 68 A0 FE 77 6A A0 82 A4 E8 A0 BA 67 7F B8 BA 9A 86 78 BA 0A EE E8 82 91 1C F0 FE 44 4D C0 AE 9C 

## 报文格式说明：

* DA:   为显示屏的地址，取值范围为 0x00 - 0xFF。
* VR:   描述了协议版本号，目前支持版本 100 和版本 200。
* PN:   是包的序列，在传输超过 255 个字节的数据时，需要分包传输，PN 表示了包的序列，每次交互完之后自增 1，设置为最大值 0XFFFF 时表示当前包是最后一个包。在传输小于 255 个字节的数据时，该值应该设置为 0XFFFF。
* CMD:  该字段描述了该包的作用，显示屏通过这个值完成不同的功能服务。
* DL:   该字段用来描述 DATA 的数据长度，当 VR 为 100 时，DL 只有 1 个字节，最大取值为 255。当 VR 为 200 时，DL 为 2 个字节组成 16 位数据,最大取值为 65535。
* DATA: 是参数数据，每条指令携带的参数和长度是不同的，详解参见后指令集章节。
* CRC:  数据包的校验码。参与校验的字段是从 DA 到 DATA 的最后一个字节。校验算法采用 CRC16，
见后章节详解。  

## 报文拆解：

### 包头数据(长度为7个字节): 
    00 C8 FF FF E5 2F 01
1. DA = 00
2. VR = C8
3. PN = FF FF 
4. CMD = E5
5. DL = 2F 01  (即参数数据长度为: 303 = 0x012f) //注意本指令属于 V2 指令集，DL 长度是 2 个字节。

### 参数数据(长度为303个字节)：
    01 00 01 78 00 81 76 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 BB A6 42 39 39 31 48 35 2C CD A3 B3 B5 30 D0 A1 CA B1 34 35 B7 D6 D6 D3 31 38 C3 EB 2C C7 EB BD C9 B7 D1 35 D4 AA 42 4D B2 00 00 00 00 00 00 00 3E 00 00 00 28 00 00 00 1D 00 00 00 E3 FF FF FF 01 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF 00 00 00 00 00 FE C7 7B F8 82 9D 42 08 BA BA 8A E8 BA AF C2 E8 BA 94 32 E8 82 54 92 08 FE AA AB F8 00 B3 08 00 56 E2 CE F8 F0 FB 5D 18 2E 95 FD 38 70 38 38 38 4B A5 A5 A8 10 88 AA 80 8E CB E4 50 45 AA 3B A0 6B 7B 35 70 50 EA E8 A8 0E 54 C2 60 B9 5D CB 48 A7 57 4F D0 00 D7 68 A0 FE 77 6A A0 82 A4 E8 A0 BA 67 7F B8 BA 9A 86 78 BA 0A EE E8 82 91 1C F0 FE 44 4D C0 

### 参数数据拆解
参数数据格式：SF + EM + ETM + ST + NI + VEN + TL + TEXT[TL] + BMPDATA [….] 具体说明参考文档；
拆解：
1. SF = 01     //: 显示标志，1 为显示，0 为不显示。
2. EM = 00     //: 进入模式，保留赋值为 0(无操作)。
3. ETM = 01    //: 退出模式，保留赋值为 0(无操作)。
4. ST = 78     //:界面的显示时间，单位为秒，0 为一直显示。
5. NI = 00     //:下一个界面的索引号，目前保留取值为 0.
6. VEN = 81    //:语音播报开关，为 80 不播报语音，为 81 播报语音。
7. TL =  76    //:显示文本长度。当前值：118
8. TEXT = 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20
20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20
20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20
20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 
20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 
BB A6 42 39 39 31 48 35 2C CD A3 B3 B5 30 D0 A1 
CA B1 34 35 B7 D6 D6 D3 31 38 C3 EB 2C C7 EB BD 
C9 B7 D1 35 D4 AA     
//文本信息字符串。当前值：“                                                                                沪B991H5,停车0小时45分钟18秒,请缴费5元”, 前面的空格需要trim掉
9. BMPDATA = 42 4D B2 00 00 00 00 00 00 00 
3E 00 00 00 28 00 00 00 1D 00 00 00 E3 FF FF FF 
01 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 
00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF 00 
00 00 00 00 FE C7 7B F8 82 9D 42 08 BA BA 8A E8 
BA AF C2 E8 BA 94 32 E8 82 54 92 08 FE AA AB F8 
00 B3 08 00 56 E2 CE F8 F0 FB 5D 18 2E 95 FD 38 
70 38 38 38 4B A5 A5 A8 10 88 AA 80 8E CB E4 50 
45 AA 3B A0 6B 7B 35 70 50 EA E8 A8 0E 54 C2 60 
B9 5D CB 48 A7 57 4F D0 00 D7 68 A0 FE 77 6A A0 
82 A4 E8 A0 BA 67 7F B8 BA 9A 86 78 BA 0A EE E8 
82 91 1C F0 FE 44 4D C0     
//二维码单色位图数据,当前值为一个29*29的BMP文件


### CRC校验码:
    AE 9C


# 指令0X6E：

## BASE64:
> AGT//25tAAQAFQEZAP8AAAAI0rvCt8uzt+cNARUBGQAA/wAACbumQUFQNjM3NQ0CFQEZAP8AAAAGwdnKsbO1DQMVARkA/wAAAAjQu9C7u925ywAKHbumQUFQNjM3NSzB2cqxs7Us16PE+tK7wrfLs7fnAFzz

## 字节数组：

    00 64 FF FF 6E 6D 00 04 00 15 01 19 00 FF 00 00
    00 08 D2 BB C2 B7 CB B3 B7 E7 0D 01 15 01 19 00
    00 FF 00 00 09 BB A6 41 41 50 36 33 37 35 0D 02
    15 01 19 00 FF 00 00 00 06 C1 D9 CA B1 B3 B5 0D
    03 15 01 19 00 FF 00 00 00 08 D0 BB D0 BB BB DD
    B9 CB 00 0A 1D BB A6 41 41 50 36 33 37 35 2C C1
    D9 CA B1 B3 B5 2C D7 A3 C4 FA D2 BB C2 B7 CB B3
    B7 E7 00 5C F3

## 报文格式说明：

* DA:   为显示屏的地址，取值范围为 0x00 - 0xFF。
* VR:   描述了协议版本号，目前支持版本 100 和版本 200。
* PN:   是包的序列，在传输超过 255 个字节的数据时，需要分包传输，PN 表示了包的序列，每次交互完之后自增 1，设置为最大值 0XFFFF 时表示当前包是最后一个包。在传输小于 255 个字节的数据时，该值应该设置为 0XFFFF。
* CMD:  该字段描述了该包的作用，显示屏通过这个值完成不同的功能服务。
* DL:   该字段用来描述 DATA 的数据长度，当 VR 为 100 时，DL 只有 1 个字节，最大取值为 255。当 VR 为 200 时，DL 为 2 个字节组成 16 位数据,最大取值为 65535。
* DATA: 是参数数据，每条指令携带的参数和长度是不同的，详解参见后指令集章节。
* CRC:  数据包的校验码。参与校验的字段是从 DA 到 DATA 的最后一个字节。校验算法采用 CRC16，
见后章节详解。  

## 报文拆解：

### 包头数据(长度为6个字节): 
    00 64 FF FF 6E 6D
1. DA = 00
2. VR = 64
3. PN = FF FF 
4. CMD = 6E
5. DL = 6D  (即参数数据长度为: 109)

### 参数数据(长度为109个字节)：
    00 04 00 15 01 19 00 FF 00 00 00 08 D2 BB C2 B7
    CB B3 B7 E7 0D 01 15 01 19 00 00 FF 00 00 09 BB
    A6 41 41 50 36 33 37 35 0D 02 15 01 19 00 FF 00
    00 00 06 C1 D9 CA B1 B3 B5 0D 03 15 01 19 00 FF
    00 00 00 08 D0 BB D0 BB BB DD B9 CB 00 0A 1D BB
    A6 41 41 50 36 33 37 35 2C C1 D9 CA B1 B3 B5 2C
    D7 A3 C4 FA D2 BB C2 B7 CB B3 B7 E7 00 

### 参数数据拆解
参数数据格式：SAVE_FLAG + TEXT_CONTEXT_NUMBER + TEXT_CONTEXT[…]+ VF+ VTL + VT[...] 具体说明参考文档；
拆解：

* SAVE_FLAG = 00              //为保存标志，取值为 1 时表示下载到存储区，取值为 0 时表示下载到临时区，频繁修改的内容建议下载到临时区。
* TEXT_CONTEXT_NUMBER = 04    //为文本参数数量，即几行文本。目前版本最大支持 4 行。
* TEXT_CONTEXT:               //为文本参数，每个文本参数控制一行，用 0X0D 分开，最后一个文本参数用 0X00 结束，最多 4 个文本参数。(本报文含4个文本参数)

* TEXT_CONTEXT[0]: //第一个文本参数
> 00 15 01 19 00 FF 00 00 00 08 D2 BB C2 B7 CB B3 B7 E7 0D

文本参数的结构为 LID + DM + DS + DT + DR + TC[4]+TL +TEXT[...]+0X0D/0X00 各参数取值含义如下。
1. LID = 00  //为显示行号。0 表示第 1 行，1 表示第 2 行，以此类推。
2. DM = 15   //为显示模式 0x15 连续左移
3. DS = 01   //为显示速度，建议取值为 0；
4. DT = 19   //停留时间，单位为秒，最大为 255 秒；
5. DR = 00   //为显示次数，0 为无限循环显示。
6. TC = FF 00 00 00  //为文本颜色，32 位数据类型，存储结构为 RGBA，R 为红色分量，G 为绿色分量，B 为蓝色分量，A 为透明值目前保留为 0，每个颜色分量占用一个字节即 8 位。
7. TL = 08  //为文本长度。
8. TEXT = D2 BB C2 B7 CB B3 B7 E7  //为文本内容，最大 32 字节。GB2312编码, 当前值： “一路顺风”
9. 结束标志 = 0D


* TEXT_CONTEXT[1]: //第二个文本参数
> 01 15 01 19 00 00 FF 00 00 09 BB A6 41 41 50 36 33 37 35 0D

文本参数的结构为 LID + DM + DS + DT + DR + TC[4]+TL +TEXT[...]+0X0D/0X00 各参数取值含义如下。
1. LID = 01  //为显示行号。0 表示第 1 行，1 表示第 2 行，以此类推。
2. DM = 15   //为显示模式 0x15 连续左移
3. DS = 01   //为显示速度，建议取值为 0；
4. DT = 19   //停留时间，单位为秒，最大为 255 秒；
5. DR = 00   //为显示次数，0 为无限循环显示。
6. TC = 00 FF 00 00  //为文本颜色，32 位数据类型，存储结构为 RGBA，R 为红色分量，G 为绿色分量，B 为蓝色分量，A 为透明值目前保留为 0，每个颜色分量占用一个字节即 8 位。
7. TL = 09  //为文本长度。
8. TEXT = BB A6 41 41 50 36 33 37 35  //为文本内容，最大 32 字节。GB2312编码, 当前值： “沪AAP6375”
9. 结束标志 = 0D


* TEXT_CONTEXT[2]: //第三个文本参数
> 02 15 01 19 00 FF 00 00 00 06 C1 D9 CA B1 B3 B5 0D

文本参数的结构为 LID + DM + DS + DT + DR + TC[4]+TL +TEXT[...]+0X0D/0X00 各参数取值含义如下。
1. LID = 02  //为显示行号。0 表示第 1 行，1 表示第 2 行，以此类推。
2. DM = 15   //为显示模式 0x15 连续左移
3. DS = 01   //为显示速度，建议取值为 0；
4. DT = 19   //停留时间，单位为秒，最大为 255 秒；
5. DR = 00   //为显示次数，0 为无限循环显示。
6. TC = FF 00 00 00  //为文本颜色，32 位数据类型，存储结构为 RGBA，R 为红色分量，G 为绿色分量，B 为蓝色分量，A 为透明值目前保留为 0，每个颜色分量占用一个字节即 8 位。
7. TL = 06  //为文本长度。
8. TEXT = C1 D9 CA B1 B3 B5  //为文本内容，最大 32 字节。GB2312编码, 当前值： “临时车”
9. 结束标志 = 0D

* TEXT_CONTEXT[3]: //第四个文本参数
> 03 15 01 19 00 FF 00 00 00 08 D0 BB D0 BB BB DD B9 CB 00

文本参数的结构为 LID + DM + DS + DT + DR + TC[4]+TL +TEXT[...]+0X0D/0X00 各参数取值含义如下。
1. LID = 03  //为显示行号。0 表示第 1 行，1 表示第 2 行，以此类推。
2. DM = 15   //为显示模式 0x15 连续左移
3. DS = 01   //为显示速度，建议取值为 0；
4. DT = 19   //停留时间，单位为秒，最大为 255 秒；
5. DR = 00   //为显示次数，0 为无限循环显示。
6. TC = FF 00 00 00  //为文本颜色，32 位数据类型，存储结构为 RGBA，R 为红色分量，G 为绿色分量，B 为蓝色分量，A 为透明值目前保留为 0，每个颜色分量占用一个字节即 8 位。
7. TL = 08  //为文本长度。
8. TEXT = D0 BB D0 BB BB DD B9 CB  //为文本内容，最大 32 字节。GB2312编码, 当前值： “谢谢惠顾”
9. 结束标志 = 00  //最终结束符

### 语音部分：
    0A 1D BB A6 41 41 50 36 33 37 35 2C C1 D9 CA B1 B3 B5 2C D7 A3 C4 FA D2 BB C2 B7 CB B3 B7 E7 00
1. VF = 0A   //语音标志，固定取值为 0X0A。
2. VTL = 1D  //语音文本长度。
3. VOICE = BB A6 41 41 50 36 33 37 35 2C C1 D9 CA B1 B3 B5 2C D7 A3 C4 FA D2 BB C2 B7 CB B3 B7 E7   //语音文本内容，最大 64 字节。当前值：“沪AAP6375,临时车,祝您一路顺风” 
4. 结束标志 = 00 //报文结束符

### CRC校验码:
    5C F3

