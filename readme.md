# CHIP-8 Assembler/Disassembler 🗿

A simple assembler and diassembler for CHIP-8. The disassembler uses a linear sweep technique to detect which parts of the ROM are instructions and which are data.

I wanted to modify [Tetris [Fran Dachille, 1991].ch8](https://github.com/kripod/chip8-roms/blob/master/games/Tetris%20%5BFran%20Dachille%2C%201991%5D.ch8) ROM because I felt I could tweak it a bit. At first, I tried poking at the raw bytes using a hex editor, but it quickly became cumbersome since each instruction contains a hard-coded address. At that point, it became apparent that a disassembler and assembler were needed to achieve my goal. Given how simple [CHIP-8](https://en.wikipedia.org/wiki/CHIP-8) is, I decided to create them myself.

I reverse-engineered the disassembled code and made several enhancements: the score counter is always visible, controls have been changed, sounds have been added for when a tetromino is placed and when a line is cleared.

Kotlin was chosen for this project because I wanted to get a feel for it (and to pass an Android exam. It actually helped because there were questions on bitwise operations).

You can find example ROMs, and their disassembled code in the [data](data) directory. ROMs are taken from [kripod/chip8-roms](https://github.com/kripod/chip8-roms).

I also created a CHIP-8 emulator in C++ with SDL3 [here](https://github.com/kacperskrzypiec/chip8emulator).

## Usage
```
chad [options]

Options:
    -i, --input <file>       
        Specify the input file (source assembly or CH8 binary)
        
    -o, --output <file>      
        Specify the output file (assembled CH8 binary or disassembled source)
        
    -a, --assemble           
        Assemble the input assembly file into a CH8 binary
        
    -d, --disassemble       
        Disassemble the input ch8 binary into assembly
```

## Sources
- [CHIP-8 Instruction Set](https://johnearnest.github.io/Octo/docs/chip8ref.pdf)
- [Chip-8 Design Specification](https://www.cs.columbia.edu/~sedwards/classes/2016/4840-spring/designs/Chip8.pdf)
- [Guide to making a CHIP-8 emulator](https://tobiasvl.github.io/blog/write-a-chip-8-emulator/)

## License

[MIT License](LICENSE)