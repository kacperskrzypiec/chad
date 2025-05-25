package org.ks

import java.io.File

fun argsGiven(args: Array<String>) {
    var inputFile: String? = null
    var outputFile: String? = null
    var assemble = false
    var disassemble = false

    fun printError(msg: String) {
        println("Error: $msg")
        println()
        printUsage()
    }

    var i = 0
    while(i < args.size) {
        when(args[i]) {
            "-i", "--input" -> {
                if(inputFile != null) {
                    printError("Input file specified more than once.")
                    return
                }

                if(i + 1 < args.size) {
                    inputFile = args[i + 1]
                    i++
                }
                else {
                    printError("No input file specified.")
                    return
                }
            }
            "-o", "--output" -> {
                if(outputFile != null) {
                    printError("Output file specified more than once.")
                    return
                }

                if(i + 1 < args.size) {
                    outputFile = args[i + 1]
                    i++
                }
                else {
                    printError("No output file specified.")
                    return
                }
            }
            "-a", "--assemble" -> assemble = true
            "-d", "--disassemble" -> disassemble = true
            else -> {
                printError("Unknown argument: ${args[i]}")
                return
            }
        }
        i++
    }

    if(assemble && disassemble) {
        printError("Cannot specify both --assemble and --disassemble.")
        return
    }

    if(!assemble && !disassemble) {
        printError("Must specify either --assemble or --disassemble.")
        return
    }

    if(inputFile == null) {
        printError("No input file specified.")
        return
    }

    if(outputFile == null) {
        outputFile = inputFile.substringBeforeLast(".")
    }

    if(!outputFile.substringAfterLast("/").contains(".")) {
        outputFile +=   if(assemble) ".ch8"
                        else if (disassemble) ".asm"
                        else ""
    }

    val file = File(inputFile)
    if(!file.exists()) {
        printError("The specified input file does not exist.")
        return
    }

    if(assemble) {
        println("Assembling $inputFile into $outputFile")
        assemble(inputFile, outputFile)
    }
    else if(disassemble) {
        println("Disassembling $inputFile into $outputFile")
        disassemble(inputFile, outputFile)
    }
}
fun printUsage() {
    println("Usage: chad [options]")
    println()
    println("Options:")
    println("\t-i, --input <file>       Specify the input file (source assembly or CH8 binary)")
    println("\t-o, --output <file>      Specify the output file (assembled CH8 binary or disassembled source)")
    println("\t-a, --assemble           Assemble the input assembly file into a CH8 binary")
    println("\t-d, --disassemble        Disassemble the input ch8 binary into assembly")
    println()
}

fun main(args: Array<String>) {
    if(args.isEmpty()) {
        printUsage()
    } else {
        argsGiven(args)
    }
}