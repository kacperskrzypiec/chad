package org.ks

enum class Instruction(val value: Int){
    ZR(0),
    JP(1),
    CALL(2),
    SKIP_VX_NQ_NN(3),
    SKIP_VX_EQ_NN(4),
    SKIP_VX_NQ_VY(5),
    SET_VX_TO_NN(6),
    ADD_NN_TO_VX(7),
    ARITHMETIC(8),
    SKIP_VX_EQ_VY(9),
    SET_I(10),
    JP0(11),
    RANDOM(12),
    DRAW(13),
    CHECK_KEY(14),
    MISC(15);

    companion object{
        private val map = Instruction.values().associateBy(Instruction::value)
        fun fromValue(type: Int) = map[type]
    }
}

enum class ArithmeticType(val value: Int){
    SET(0),
    OR(1),
    AND(2),
    XOR(3),
    ADD(4),
    SUB(5),
    RHS(6),
    ISUB(7),
    LHS(0xE);

    companion object{
        private val map = ArithmeticType.values().associateBy(ArithmeticType::value)
        fun fromValue(type: Int) = map[type]
    }
}

enum class MiscType(val value: Int){
    SET_VX_TO_DELAY(0x07),
    WAIT_KEY(0x0A),
    SET_DELAY_TO_VX(0x15),
    SET_BUZZER_TO_VX(0x18),
    ADD_VX_TO_I(0x1E),
    SET_I_TO_HEX(0x29),
    BCD_VX(0x33),
    SAVE_VX(0x55),
    LOAD_VX(0x65);

    companion object{
        private val map = MiscType.values().associateBy(MiscType::value)
        fun fromValue(type: Int) = map[type]
    }
}