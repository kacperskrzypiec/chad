package org.ks

enum class TokenType(val value: Int) {
    UNKNOWN(-1),
    CLEAR(0),
    RET(1),
    OR(2),
    AND(3),
    XOR(4),
    RHS(5),
    LHS(6),
    ISUB(7),
    SUB(8),
    LD(9),
    //LD_VX_DELAY
    //LD_VX_KEY
    //LD_DELAY_VX
    //LD_BUZZER_VX
    //LD_VX_NN
    //LD_I_VX
    ADD(10),
    //ADD_VX_VY
    //ADD_I_VX
    //ADD_VX_NN
    KP(11),
    KNP(12),
    HEX(13),
    BCD(14),
    SAVE(15),
    LOAD(16),
    JP(17),
    JP0(18),
    CALL(19),
    SNE(20),
    SE(21),
    //SE_VX_NN
    //SE_VX_VY
    RND(22),
    DRW(23),
    WORD(24),

    V(25),
    I(26),
    PLUS_ONE(27),
    IDENTIFIER(28),
    NUMBER(29),
    HEX_NUMBER(30),
    COMMA(31),
    COLON(32),
    NEW_LINE(33),
    END(34);

    companion object{
        private val map = TokenType.values().associateBy(TokenType::name)
        fun fromName(name: String) = map[name]
    }
}
data class Token(val type: TokenType, val line: Int, val name: String="", val number: Int=0)