package org.ks

import java.io.File

private fun getBytesFromFile(filePath: String): ByteArray{
    val file = File(filePath)

    return if(file.exists()){
        file.readBytes()
    } else{
        println("Could not open the file.")
        ByteArray(0)
    }
}
private fun convertBytesToWords(bytes: ByteArray): Array<UShort>{
    val paddedBytes = if(bytes.size and 1 == 1) {
        bytes + 0x00
    } else {
        bytes
    }

    val words = Array(paddedBytes.size / 2){ 0.toUShort() }
    for(i in words.indices){
        val msb = paddedBytes[i * 2].toUInt() and 0xFFu
        val lsb = paddedBytes[i * 2 + 1].toUInt() and 0xFFu
        words[i] = (msb shl 8 or lsb).toUShort()
    }
    return words
}

fun disassemble(inputFile: String, outputFile: String){
    val file = File(inputFile)
    check(file.exists())

    val words = convertBytesToWords(getBytesFromFile(inputFile))
    if(words.isEmpty()) return;

    val rawLabels = arrayListOf<Int>()
    val lines = arrayListOf<String>()

    val isLabelData = mutableMapOf<Int, Boolean>()

    // PASS 1
    // Detect labels and get initial addresses for 2nd pass
    for(i in words.indices) {
        val word = words[i]
        val n1 = words[i].toUInt() and 0xF000u shr 12

        val address = ((word and 0xFFFu) - 0x200u).toInt() and 0xFFE

        when (Instruction.fromValue(n1.toInt())) {
            Instruction.JP, Instruction.CALL, Instruction.JP0 -> {
                if(address < words.size * 2) {
                    rawLabels.add(address)
                    isLabelData[address] = false
                }
            }
            Instruction.SET_I -> {
                if(address < words.size * 2) {
                    rawLabels.add(address)
                    isLabelData[address] = true
                }
            }
            else -> {}
        }
    }

    rawLabels.sort()
    val preLabels = rawLabels.distinct()
    val labelsCalled = arrayListOf<Int>()
    for(label in preLabels){
        labelsCalled.add(0)
    }

    // PASS 2
    // Detect data segments
    // In case CHIP-8, the data will always be after jump, call and return instructions,
    // unless they follow a skip instruction.

    var isData = false
    var previousInstruction: UInt = 0u

    for(i in words.indices) {
        val word = words[i]
        val n1 = words[i].toUInt() and 0xF000u shr 12
        val n2 = words[i].toUInt() and 0x0F00u shr 8
        val n3 = words[i].toUInt() and 0x00F0u shr 4
        val n4 = words[i].toUInt() and 0x000Fu

        val address = ((word and 0xFFFu) - 0x200u).toInt() and 0xFFE

        for(j in preLabels.indices){
            if(preLabels[j] == i * 2){
                isData = isLabelData[preLabels[j]] != false
                break
            }
        }

        if(isData) {
            continue
        }

        fun zr(){
            // if return
            if(n2 == 0u && n3 == 0xEu && n4 == 0xEu){
                // if return doesn't follow a skip instruction, then instructions after it are data
                if(previousInstruction != 0x3u && previousInstruction != 0x4u && previousInstruction != 0x5u && previousInstruction != 0x9u && previousInstruction != 0xEu) {
                    isData = true
                }
            }
        }
        fun general(){
            fun getLabel(address: Int){
                for(j in preLabels.indices){
                    if(preLabels[j] == address){
                        labelsCalled[j]++
                    }
                }
            }

            when(Instruction.fromValue(n1.toInt())){
                Instruction.JP -> {
                    if(address and 1 == 1) {
                        getLabel(address - 1)
                    } else{
                        getLabel(address)
                    }
                }
                Instruction.CALL -> {
                    if(address and 1 == 1) {
                        getLabel(address - 1)
                    } else{
                        getLabel(address)
                    }
                }
                Instruction.JP0 -> {
                    if(address and 1 == 1) {
                        getLabel(address-1)
                    } else{
                        getLabel(address)
                    }
                }
                Instruction.SET_I -> {
                    if(address and 1 == 1) {
                        getLabel(address-1)
                    } else{
                        getLabel(address)
                    }
                }
                else -> {}
            }
        }

        when(Instruction.fromValue(n1.toInt())){
            Instruction.ZR -> zr()
            else -> general()
        }

        previousInstruction = n1
    }

    val labels = preLabels.filterIndexed{index, _ ->
        labelsCalled[index] != 0 || isLabelData[preLabels[index]] == true
    }

    // PASS 3
    // Emit instructions and data
    isData = false
    previousInstruction = 0u

    for(i in words.indices) {
        val word = words[i]
        val n1 = words[i].toUInt() and 0xF000u shr 12
        val n2 = words[i].toUInt() and 0x0F00u shr 8
        val n3 = words[i].toUInt() and 0x00F0u shr 4
        val n4 = words[i].toUInt() and 0x000Fu

        val address = ((word and 0xFFFu) - 0x200u).toInt() and 0xFFF

        for(j in labels.indices){
            if(labels[j] == i * 2){
                isData = isLabelData[preLabels[j]] != false
                break
            }
        }

        if(isData) {
            //val line = String.format("0x%03x: %s\n", i * 2 + 512, String.format("WORD 0x%04x", word.toInt()))
            val line = String.format("%s\n", String.format("WORD 0x%04x", word.toInt()))
            lines.add(line)
            continue
        }

        fun zr(): String{
            if(n2 == 0u && n3 == 0xEu && n4 == 0x0u){
                return "CLEAR"
            }
            else if(n2 == 0u && n3 == 0xEu && n4 == 0xEu){
                if(previousInstruction != 0x3u && previousInstruction != 0x4u && previousInstruction != 0x5u && previousInstruction != 0x9u && previousInstruction != 0xEu) {
                    isData = true
                }
                return "RET"
            }
            return String.format("WORD 0x%04x", word.toInt())
        }
        fun arithmetic(): String{
            return when(ArithmeticType.fromValue(n4.toInt())){
                ArithmeticType.SET -> String.format("LD V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.OR -> String.format("OR V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.AND -> String.format("AND V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.XOR -> String.format("XOR V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.ADD -> String.format("ADD V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.SUB -> String.format("SUB V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.RHS -> String.format("RHS V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.ISUB -> String.format("ISUB V%01x, V%01x", n2.toInt(), n3.toInt())
                ArithmeticType.LHS -> String.format("LHS V%01x, V%01x", n2.toInt(), n3.toInt())
                else -> String.format("WORD 0x%04x", word.toInt())
            }
        }
        fun key(): String{
            if(n3 == 0x9u && n4 == 0xEu){
                return String.format("KNP V%01x", n2.toInt())
            }
            else if(n3 == 0xAu && n4 == 0x1u){
                return String.format("KP V%01x", n2.toInt())
            }

            return String.format("WORD 0x%04x", word.toInt())
        }
        fun misc(): String{
            return when(MiscType.fromValue((word.toUInt() and 0x00FFu).toInt())){
                MiscType.SET_VX_TO_DELAY -> String.format("LD V%01x, delay", n2.toInt())
                MiscType.WAIT_KEY -> String.format("LD V%01x, key", n2.toInt())
                MiscType.SET_DELAY_TO_VX -> String.format("LD delay, V%01x", n2.toInt())
                MiscType.SET_BUZZER_TO_VX -> String.format("LD buzzer, V%01x", n2.toInt())
                MiscType.ADD_VX_TO_I -> String.format("ADD I, V%01x", n2.toInt())
                MiscType.SET_I_TO_HEX -> String.format("HEX I, V%01x", n2.toInt())
                MiscType.BCD_VX -> String.format("BCD V%01x", n2.toInt())
                MiscType.SAVE_VX -> String.format("SAVE V%01x", n2.toInt())
                MiscType.LOAD_VX -> String.format("LOAD V%01x", n2.toInt())
                else -> String.format("WORD 0x%04x", word.toInt())
            }
        }
        fun general(): String{
            fun getLabel(address: Int): String{
                for(j in labels.indices){
                    if(labels[j] == address){
                        return String.format("label%d", j)
                    }
                }
                return String.format("0x%03x", address + 0x200)
            }

            return when(Instruction.fromValue(n1.toInt())){
                Instruction.JP -> {
                    if(address and 1 == 1) {
                        String.format("JP %s+1", getLabel(address - 1))
                    } else{
                        String.format("JP %s", getLabel(address))
                    }
                }
                Instruction.CALL -> {
                    if(address and 1 == 1) {
                        String.format("CALL %s+1", getLabel(address - 1))
                    } else{
                        String.format("CALL %s", getLabel(address))
                    }
                }
                Instruction.SKIP_VX_NQ_NN -> String.format("SNE V%01x, 0x%02x", n2.toInt(), (word.toUInt() and 0x00FFu).toInt())
                Instruction.SKIP_VX_EQ_NN -> String.format("SE V%01x, 0x%02x", n2.toInt(), (word.toUInt() and 0x00FFu).toInt())
                Instruction.SKIP_VX_NQ_VY -> String.format("SNE V%01x, V%01x", n2.toInt(), n3.toInt())
                Instruction.SET_VX_TO_NN -> String.format("LD V%01x, 0x%02x", n2.toInt(), (word.toUInt() and 0x00FFu).toInt())
                Instruction.ADD_NN_TO_VX -> String.format("ADD V%01x, 0x%02x", n2.toInt(), (word.toUInt() and 0x00FFu).toInt())
                Instruction.SKIP_VX_EQ_VY -> String.format("SE V%01x, V%01x", n2.toInt(), n3.toInt())
                Instruction.SET_I -> {
                    if(address and 1 == 1) {
                        String.format("LD I, %s+1", getLabel(address-1))
                    } else{
                        String.format("LD I, %s", getLabel(address))
                    }
                }
                Instruction.JP0 -> {
                    if(address and 1 == 1) {
                        String.format("JP0 %s+1", getLabel(address-1))
                    } else{
                        String.format("JP0 %s", getLabel(address))
                    }
                }
                Instruction.RANDOM -> String.format("RND V%01x, 0x%02x", n2.toInt(), (word.toUInt() and 0x00FFu).toInt())
                Instruction.DRAW -> String.format("DRW V%01x, V%01x, 0x%01x", n2.toInt(), n3.toInt(), n4.toInt())
                else -> String.format("WORD 0x%04x", word.toInt())
            }
        }

        val ret = when(Instruction.fromValue(n1.toInt())){
            Instruction.ZR -> zr()
            Instruction.ARITHMETIC -> arithmetic()
            Instruction.CHECK_KEY -> key()
            Instruction.MISC -> misc()
            else -> general()
        }

        previousInstruction = n1

        val line = String.format("%s\n", ret)
        lines.add(line)
    }

    // Insert labels
    for(i in labels.indices){
        lines.add(labels[i] / 2 + i, String.format("label%d:\n", i))
    }

    val linesToOutput = lines
        .map{it.trim()}
        .filter{it.isNotEmpty()}

    val output = File(outputFile)
    if(!output.exists()){
        output.createNewFile()
    }
    output.writeText("")
    for(line in linesToOutput){
        output.appendText(line + "\n")
    }
}