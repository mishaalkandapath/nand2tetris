package main.java;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    /*
    Accept filename.vm from input
    Create a Parses
    Create a CodeWriter
    Assemble!
     */
    public static void main(String[] args) throws IOException {
        //create the parser:
        boolean isFileName = args[0].contains("."); //is the given name a vm file or a directory of many vm files.
        List<String> files = isFileName ? List.of(args[0]) : filesInDir(Path.of(args[0]));

        try {
            int lines = 0;
            CodeWriter codeWriter = new CodeWriter(isFileName ? args[0].substring(0, args[0].length() - 2) : args[0],
                    files.contains("Sys.vm"),
                    loadOperationSymbols(),
                    loadMemSegments()); //same codewriter for all the files, writes into one file only
            for (String file: files){
                Parser parser = new Parser(file);
                while (parser.hasMoreCommands()){
                    parser.advance();
                    Command type = parser.commandType();
                    System.out.println(type);
                    switch (type){
                        case C_ARITHMETIC_BIN -> lines += codeWriter.writeArithmeticBinary(parser.arg1(), lines);
                        case C_ARITHMETIC_UN -> codeWriter.writeArithmeticUnary(parser.arg1());
                        case C_POP, C_PUSH -> codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
                        case C_LABEL -> codeWriter.writeLabel(parser.arg1());
                        case C_GOTO -> codeWriter.writeGoto(parser.arg1());
                        case C_IF -> codeWriter.writeIf(parser.arg1());
                        case C_FUNCTION -> codeWriter.writeFunction(parser.arg1(), parser.arg2());
                        case C_CALL -> {
                            codeWriter.writeCall(parser.arg1(), parser.arg2(), lines);
                            lines += 1;
                        }
                        case C_RETURN -> codeWriter.writeReturn();

                    }
                }
                codeWriter.close();//close the file
                parser.close();
            }
        } catch (FileNotFoundException e) {
            System.out.println("File probably not found");
            throw new RuntimeException(e);
        }



    }

    private static List<String> filesInDir(Path path) throws IOException{
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }

        List<String> files;
        try (Stream<Path> walk = Files.walk(path)){
            files = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> p.toString().toLowerCase())
                    .filter(f -> f.endsWith(".vm"))
                    .collect(Collectors.toList());
        }
        return files;
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
