package main.java;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

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
            CodeWriter codeWriter = new CodeWriter(args[0],
                    loadOperationSymbols(),
                    loadMemSegments());
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

    private static HashMap<String, Integer> loadMemSegments(){
        HashMap<String, Integer> memSegments = new HashMap<>();
        memSegments.put("local", 1);
        memSegments.put("argument", 2);
        memSegments.put("this", 3);
        memSegments.put("that", 4);
//        memSegments.put("static", 16);
        memSegments.put("temp", 5);
        //note static is implemented differently, refer codewriter class for the same
        //pointer is also implemented differently, refer as above.
        return memSegments;
    }

    private static HashMap<String, String> loadOperationSymbols(){
        HashMap<String, String> opSymbols = new HashMap<>();
        opSymbols.put("add", "+");
        opSymbols.put("sub", "-");
        opSymbols.put("and", "&");
        opSymbols.put("or", "|");
        opSymbols.put("neg", "-");
        opSymbols.put("not", "!");
        opSymbols.put("eq", "JEQ");
        opSymbols.put("lt", "JLT");
        opSymbols.put("gt", "JGT");
        return opSymbols;
    }
}
