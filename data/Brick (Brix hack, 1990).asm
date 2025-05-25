LD Ve, 0x05
LD V5, 0x00
label0:
LD Vb, 0x06
label1:
LD Va, 0x00
label2:
LD I, label15
DRW Va, Vb, 0x1
ADD Va, 0x04
SNE Va, 0x40
JP label2
ADD Vb, 0x01
SNE Vb, 0x12
JP label1
LD Vc, 0x20
LD Vd, 0x1f
LD I, label17
DRW Vc, Vd, 0x1
CALL label14
LD V0, 0x00
LD V1, 0x00
LD I, label18
DRW V0, V1, 0x1
ADD V0, 0x08
LD I, label16
DRW V0, V1, 0x1
label3:
LD V0, 0x40
LD delay, V0
label4:
LD V0, delay
SNE V0, 0x00
JP label4
RND V6, 0x0f
LD V7, 0x1e
LD V8, 0x01
LD V9, 0xff
LD I, label16
DRW V6, V7, 0x1
label5:
LD I, label17
DRW Vc, Vd, 0x1
LD V0, 0x04
KP V0
ADD Vc, 0xfe
LD V0, 0x06
KP V0
ADD Vc, 0x02
LD V0, 0x3f
AND Vc, V0
DRW Vc, Vd, 0x1
LD I, label16
DRW V6, V7, 0x1
ADD V6, V8
ADD V7, V9
LD V0, 0x3f
AND V6, V0
LD V1, 0x1f
AND V7, V1
SE V7, 0x1f
JP label8
label6:
SE V6, 0x00
LD V8, 0x01
SE V6, 0x3f
LD V8, 0xff
SE V7, 0x00
LD V9, 0x01
DRW V6, V7, 0x1
SNE Vf, 0x01
JP label7
SE V7, 0x1f
JP label7
LD V0, 0x05
SUB V0, V7
SNE Vf, 0x00
JP label7
LD V0, 0x01
LD buzzer, V0
LD V0, V6
LD V1, 0xfc
AND V0, V1
LD I, label15
DRW V0, V7, 0x1
LD V0, 0xfe
XOR V9, V0
CALL label14
ADD V5, 0x01
CALL label14
SE V5, 0xc0
JP label20
label7:
JP label5
label8:
LD V9, 0xff
LD V0, V6
SUB V0, Vc
SNE Vf, 0x01
JP label9
LD V1, 0x02
SUB V0, V1
SNE Vf, 0x01
JP label11
SUB V0, V1
SNE Vf, 0x01
JP label13
SUB V0, V1
SNE Vf, 0x01
JP label12
label9:
LD V0, 0x20
LD buzzer, V0
LD I, label16
ADD Ve, 0xff
LD V0, Ve
ADD V0, V0
LD V1, 0x00
DRW V0, V1, 0x1
SNE Ve, 0x00
JP label3
label10:
JP label10
label11:
ADD V8, 0xff
SE V8, 0xfe
LD V8, 0xff
JP label13
label12:
ADD V8, 0x01
SE V8, 0x02
LD V8, 0x01
label13:
LD V0, 0x04
LD buzzer, V0
LD V9, 0xff
JP label6
label14:
LD I, label19
BCD V5
LOAD V2
HEX I, V1
LD V3, 0x37
LD V4, 0x00
DRW V3, V4, 0x5
ADD V3, 0x05
HEX I, V2
DRW V3, V4, 0x5
RET
label15:
WORD 0xf000
label16:
WORD 0x8000
label17:
WORD 0xfc00
label18:
WORD 0xaa00
label19:
WORD 0x0000
WORD 0x0000
label20:
LD Ve, 0x05
CLEAR
JP label0
