package org.ks

import java.io.ByteArrayOutputStream
import java.io.File

private fun lex(source: String): ArrayList<Token>{
    val tokens = arrayListOf<Token>()

    var position: Int = 0
    fun advance(): Char = source[position++]
    fun isEnd(): Boolean = position >= source.length
    fun peek(): Char = if (isEnd()) 0.toChar() else source[position]
    fun peekAhead(): Char = if(position + 1 >= source.length) 0.toChar() else source[position + 1]
    fun match(c: Char): Boolean = if(peek() != c) false else { position++; true }

    var line: Int = 1

    fun hexNumber(){
        val startPosition = position
        while(peek() in '0' .. '9' || peek() in 'a'..'f' || peek() in 'A'..'F') advance()
        val numString = source.substring(startPosition, position)
        val num = numString.toInt(16)
        tokens.add(Token(TokenType.HEX_NUMBER, line, "", num))
    }
    fun identifier(){
        val startPosition = position - 1
        while(peek() == '_' || peek().isLetterOrDigit()) advance()
        val key = source.substring(startPosition, position)

        val tokenType = TokenType.fromName(key.uppercase())
        if(tokenType != null) {
            tokens.add(Token(tokenType, line))
        } else {
            tokens.add(Token(TokenType.IDENTIFIER, line, key))
        }
    }

    while(!isEnd()){
        when(val c = advance()){
            '+' -> {
                if(match('1')){
                    tokens.add(Token(TokenType.PLUS_ONE, line))
                } else {
                    println(String.format("Line %d: after + there must be 1.", line))
                }
            }
            ';' -> {
                while(peek() != '\n' && !isEnd()) advance()
            }
            '0' -> {
                if(match('x')){
                    hexNumber()
                } else {
                    println(String.format("Line %d: number must be given in hex format.", line))
                }
            }
            ',' -> {
                tokens.add(Token(TokenType.COMMA, line))
            }
            ':' -> {
                tokens.add(Token(TokenType.COLON, line))
            }
            'V' -> {
                tokens.add(Token(TokenType.V, line, "", peek().digitToInt(16)))
                advance()
            }
            '\n' -> { tokens.add(Token(TokenType.NEW_LINE, line)); line++ }
            ' ', '\t', '\r' -> {}
            else -> {
                if(c.isLetterOrDigit() || c == '_'){
                    identifier()
                } else {
                    println(String.format("Line %d: '%c' unknown character.", line, c))
                }
            }
        }
    }
    tokens.add(Token(TokenType.END, line))

    return tokens
}

private fun parse(tokens: ArrayList<Token>): ByteArrayOutputStream{
    val outBytes = ByteArrayOutputStream()
    var position: Int = 0
    fun isEnd() = position >= tokens.size || tokens[position].type == TokenType.END
    fun previous(): Token = if(position > 0) tokens[position - 1] else Token(TokenType.UNKNOWN, -1)
    fun peek(): Token = if(isEnd()) Token(TokenType.UNKNOWN, -1) else tokens[position]
    fun peekAhead(): Token = if(position + 1 >= tokens.size) Token(TokenType.UNKNOWN, -1) else tokens[position + 1]
    fun advance(): Token = if(!isEnd()) tokens[position++] else Token(TokenType.UNKNOWN, -1)
    fun check(type: TokenType): Boolean = if(isEnd()) false else tokens[position].type == type
    fun consume(type: TokenType): Token =
        if(check(type)) advance()
        else {
            Token(TokenType.UNKNOWN, -1)
        }

    var instructionIndex: Int = 0

    val labelMap = mutableMapOf<String, Int>()

    while(!isEnd()){
        if(peek().type == TokenType.IDENTIFIER && peekAhead().type == TokenType.COLON){
            if(peek().name == "delay" || peek().name == "buzzer"){
                println("delay or buzzer can't be used as a label name.")
                break
            }
            labelMap[peek().name] = instructionIndex + 512
            tokens.removeAt(position)
            tokens.removeAt(position)
            while(!isEnd() && peek().type != TokenType.NEW_LINE) advance()
            advance()
        }
        else if(peek().type in TokenType.CLEAR..TokenType.WORD){
            instructionIndex += 2
            while(!isEnd() && peek().type != TokenType.NEW_LINE) advance()
            advance()
        }
    }

    position = 0

    fun emitBytes(instruction: UInt){
        outBytes.write((instruction.toInt() shr 8) and 0xFF)
        outBytes.write(instruction.toInt() and 0xFF)
    }

    fun checkNewLine(token: Token): Boolean{
        if(consume(TokenType.NEW_LINE).type != TokenType.UNKNOWN){
            return true
        }
        else{
            println(String.format("%d: expected a new line.", token.line))
            return false
        }
    }
    fun checkComma(token: Token): Boolean{
        if(consume(TokenType.COMMA).type != TokenType.UNKNOWN){
            return true
        }
        else{
            println(String.format("%d: expected a comma.", token.line))
            return false
        }
    }
    fun arithmetic(token: Token, type: UInt): Boolean{
        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
            val VX = (previous().number and 0xF).toUInt()
            checkComma(token)
            if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                val VY = (previous().number and 0xF).toUInt()
                emitBytes(0x8000u or (VX shl 8) or (VY shl 4) or (type and 0xFu))
            } else {
                println(String.format("%d: OR invalid.", token.line))
                return false
            }
        } else {
            println(String.format("%d: OR invalid.", token.line))
            return false
        }
        if(!checkNewLine(token)) return false
        return true
    }

    while(!isEnd()){
        val token = advance()

        if(token.type in TokenType.CLEAR..TokenType.WORD){
            when(token.type){
                TokenType.CLEAR -> {
                    emitBytes(0x00E0u)
                    if(!checkNewLine(token)) break
                }
                TokenType.RET -> {
                    emitBytes(0x00EEu)
                    if(!checkNewLine(token)) break
                }
                TokenType.JP -> {
                    if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                        emitBytes(0x1000u or (previous().number.toUInt() and 0x0FFFu))
                    }
                    else if(consume(TokenType.IDENTIFIER).type != TokenType.UNKNOWN){
                        if(labelMap.containsKey(previous().name)){
                            val address: UInt = labelMap[previous().name]!!.toUInt()
                            emitBytes(0x1000u or (address and 0x0FFFu))
                        } else {
                            println(String.format("%d: JP Unknown label '%s'", token.line, previous().name))
                            break
                        }
                    }
                    else {
                        println(String.format("%d: JP expected an address or a label.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.CALL -> {
                    if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                        emitBytes(0x2000u or (previous().number.toUInt() and 0x0FFFu))
                    }
                    else if(consume(TokenType.IDENTIFIER).type != TokenType.UNKNOWN){
                        if(labelMap.containsKey(previous().name)){
                            val address: UInt = labelMap[previous().name]!!.toUInt()
                            emitBytes(0x2000u or (address and 0x0FFFu))
                        } else {
                            println(String.format("%d: CALL nknown label '%s'", token.line, previous().name))
                            break
                        }
                    }
                    else {
                        println(String.format("%d: CALL expected an address or a label.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.SE -> {
                    if(consume(TokenType.V).type != TokenType.UNKNOWN){
                        val VX = (previous().number and 0xF).toUInt()
                        if(!checkComma(token)) break;
                        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VY = (previous().number and 0xF).toUInt()
                            emitBytes(0x9000u or (VX shl 8) or (VY shl 4))
                        } else if (consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN) {
                            val NN = (previous().number and 0xFF).toUInt()
                            emitBytes(0x4000u or (VX shl 8) or NN)
                        } else {
                            println(String.format("%d: SE Unknown operand.", token.line))
                            break
                        }
                    } else {
                        println(String.format("%d: SE Expected a register first.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.SNE -> {
                    if(consume(TokenType.V).type != TokenType.UNKNOWN){
                        val VX = (previous().number and 0xF).toUInt()
                        if(!checkComma(token)) break;
                        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VY = (previous().number and 0xF).toUInt()
                            emitBytes(0x5000u or (VX shl 8) or (VY shl 4))
                        } else if (consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN) {
                            val NN = (previous().number and 0xFF).toUInt()
                            emitBytes(0x3000u or (VX shl 8) or NN)
                        } else {
                            println(String.format("%d: SNE Unknown operand.", token.line))
                            break
                        }
                    } else {
                        println(String.format("%d: SNE Expected a register first.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.LD -> {
                    if(consume(TokenType.I).type != TokenType.UNKNOWN){
                        if(!checkComma(token)) break;

                        if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                            emitBytes(0xA000u or (previous().number.toUInt() and 0x0FFFu))
                        } else if(consume(TokenType.IDENTIFIER).type != TokenType.UNKNOWN){
                            if(labelMap.containsKey(previous().name)){
                                val address: UInt = labelMap[previous().name]!!.toUInt()
                                if(consume(TokenType.PLUS_ONE).type != TokenType.UNKNOWN){
                                    emitBytes(0xA000u or ((address + 1u) and 0x0FFFu))
                                } else {
                                    emitBytes(0xA000u or (address and 0x0FFFu))
                                }
                            } else {
                                println(String.format("%d: LD I nknown label '%s'", token.line, previous().name))
                                break
                            }
                        } else {
                            println(String.format("%d: LD I Unknown operand.", token.line))
                            break
                        }
                    }
                    else if(consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        if(!checkComma(token)) break;
                        if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                            emitBytes(0x6000u or (VX shl 8) or (previous().number.toUInt() and 0xFFu))
                        }
                        else if(consume(TokenType.IDENTIFIER).type != TokenType.UNKNOWN){
                            if(previous().name == "delay"){
                                emitBytes(0xF007u or (VX shl 8))
                            }
                            else if(previous().name == "key"){
                                emitBytes(0xF00Au or (VX shl 8))
                            }
                            else{
                                println(String.format("%d: Only key and delay allowed.", token.line))
                                break
                            }
                        }
                        else if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VY = (previous().number and 0xF).toUInt()
                            emitBytes(0x8000u or (VX shl 8) or (VY shl 4))
                        }
                        else{
                            println(String.format("%d: LD invalid", token.line))
                            break
                        }
                    }
                    else if(consume(TokenType.IDENTIFIER).type != TokenType.UNKNOWN){
                        val name = previous().name
                        if(!checkComma(token)) break;
                        if (consume(TokenType.V).type != TokenType.UNKNOWN){
                            val VX = (previous().number and 0xF).toUInt()
                            if(name == "buzzer"){
                                emitBytes(0xF018u or (VX shl 8))
                            }
                            else if(name == "delay"){
                                emitBytes(0xF015u or (VX shl 8))
                            }
                        }
                    }
                    else{
                        println(String.format("%d: LD Unknown instruction.", token.line))
                        break
                    }

                    if(!checkNewLine(token)) break
                }
                TokenType.ADD -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        checkComma(token)
                        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VY = (previous().number and 0xF).toUInt()
                            emitBytes(0x8004u or (VX shl 8) or (VY shl 4))
                        }
                        if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                            emitBytes(0x7000u or (VX shl 8) or (previous().number.toUInt() and 0xFFu))
                        }
                    }
                    else if (consume(TokenType.I).type != TokenType.UNKNOWN) {
                        checkComma(token)
                        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VX = (previous().number and 0xF).toUInt()
                            emitBytes(0xF01Eu or (VX shl 8))
                        } else {
                            println(String.format("%d: ADD I expected a register.", token.line))
                            break
                        }
                    }
                    else{
                        println(String.format("%d: ADD Unknown instruction.", token.line))
                        break
                    }

                    if(!checkNewLine(token)) break
                }
                TokenType.JP0 -> {
                    if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                        emitBytes(0x1000u or (previous().number.toUInt() and 0x0FFFu))
                    }
                    else if(consume(TokenType.IDENTIFIER).type != TokenType.UNKNOWN){
                        if(labelMap.containsKey(previous().name)){
                            val address: UInt = labelMap[previous().name]!!.toUInt()
                            emitBytes(0xB000u or (address and 0x0FFFu))
                        } else {
                            println(String.format("%d: JP0 Unknown label '%s'", token.line, previous().name))
                            break
                        }
                    }
                    else {
                        println(String.format("%d: JP0 expected an address or a label.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.OR -> {
                    if(!arithmetic(token, 1u)) break
                }
                TokenType.AND -> {
                    if(!arithmetic(token, 2u)) break
                }
                TokenType.XOR -> {
                    if(!arithmetic(token, 3u)) break
                }
                TokenType.SUB -> {
                    if(!arithmetic(token, 5u)) break
                }
                TokenType.RHS -> {
                    if(!arithmetic(token, 6u)) break
                }
                TokenType.ISUB -> {
                    if(!arithmetic(token, 7u)) break
                }
                TokenType.LHS -> {
                    if(!arithmetic(token, 0xEu)) break
                }
                TokenType.DRW -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        checkComma(token)
                        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VY = (previous().number and 0xF).toUInt()
                            checkComma(token)
                            if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN) {
                                emitBytes(0xD000u or (VX shl 8) or (VY shl 4) or (previous().number.toUInt() and 0xFu))
                            } else {
                                println(String.format("%d: DRAW invalid.", token.line))
                                break
                            }
                        } else {
                            println(String.format("%d: DRAW invalid.", token.line))
                            break
                        }
                    } else {
                        println(String.format("%d: DRAW invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.RND -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        checkComma(token)
                        if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN) {
                            emitBytes(0xC000u or (VX shl 8) or (previous().number.toUInt() and 0xFFu))
                        } else {
                            println(String.format("%d: RND invalid.", token.line))
                            break
                        }
                    } else {
                        println(String.format("%d: RND invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.KNP -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        emitBytes(0xE09Eu or (VX shl 8))
                    } else {
                        println(String.format("%d: KNP invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.KP -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        emitBytes(0xE0A1u or (VX shl 8))
                    } else {
                        println(String.format("%d: KP invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.HEX -> {
                    if(consume(TokenType.I).type != TokenType.UNKNOWN) {
                        checkComma(token)
                        if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                            val VX = (previous().number and 0xF).toUInt()
                            emitBytes(0xF029u or (VX shl 8))
                        } else {
                            println(String.format("%d: HEX invalid.", token.line))
                            break
                        }
                    } else{
                        println(String.format("%d: HEX invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.BCD -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        emitBytes(0xF033u or (VX shl 8))
                    } else {
                        println(String.format("%d: BCD invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.SAVE -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        emitBytes(0xF055u or (VX shl 8))
                    } else {
                        println(String.format("%d: SAVE invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.LOAD -> {
                    if (consume(TokenType.V).type != TokenType.UNKNOWN) {
                        val VX = (previous().number and 0xF).toUInt()
                        emitBytes(0xF065u or (VX shl 8))
                    } else {
                        println(String.format("%d: LOAD invalid.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                TokenType.WORD -> {
                    if(consume(TokenType.HEX_NUMBER).type != TokenType.UNKNOWN){
                        emitBytes(previous().number.toUInt() and 0xFFFFu)
                    } else {
                        println(String.format("%d: WORD expected a number.", token.line))
                        break
                    }
                    if(!checkNewLine(token)) break
                }
                else -> {

                }
            }
        } else {
            //println(String.format("%d: Unknown token.", peek().line))

        }
    }

    return outBytes
}

fun assemble(inputFile: String, outputFile: String){
    val file = File(inputFile)
    check(file.exists())

    val code = file.readLines()
            .map{it.trim()}
            .filter{it.isNotEmpty()}
            .joinToString("\n")

    val tokens = lex(code)
    val outBytes = parse(tokens)

    val outFile = File(outputFile)
    if(!outFile.exists()){
        outFile.createNewFile()
    }
    outBytes.writeTo(outFile.outputStream())
}