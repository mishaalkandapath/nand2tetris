package main.java;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    /*
    Accept filename.vm from input
    Create a Parses
    Create a CodeWriter
    Assemble!
     */
    public static void main(String[] args) throws IOException {
        //create the parser:
        try {
            int lines = 0;
            Parser parser = new Parser(args[0]);
            CodeWriter codeWriter = new CodeWriter(args[0].substring(0, args[0].length()));
            while (parser.hasMoreCommands()){
                parser.advance();
                Command type = parser.commandType();
                switch (type){
                    case C_ARITHMETIC_BIN -> lines += codeWriter.writeArithmeticBinary(parser.arg1(), lines);
                    case C_ARITHMETIC_UN -> codeWriter.writeArithmeticUnary(parser.arg1());
                    case C_POP, C_PUSH -> codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
                }
            }
            codeWriter.close();//close the file
            parser.close();
        } catch (FileNotFoundException e) {
            System.out.println("File probably not found");
            throw new RuntimeException(e);
        }



    }
}
