package main.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Parser {
 /*
 Responsible for parsing a single.vm file
 Reads a command and dissects it into required parts
 Context: Two types of VM Operations at the moment
 Push pop aka Memory Operations
 add, sub, or, neg, eq, or, and, lt, gt aka Arithmetic Operations
  */

    private final Scanner vmScannedFile;
    private String currLine;

    /*
    Constructor to accept a file and open it
     */
    public Parser(String filename, String extension) throws FileNotFoundException {
        File vmFile = new File(filename + extension);
        this.vmScannedFile = new Scanner(vmFile); //using a scanner for efficiency
    }

    public boolean hasMoreCommands(){
        return vmScannedFile.hasNextLine();
    }

    public void advance(){
        currLine = vmScannedFile.nextLine();
    }

    public Command commandType(){
        return switch (currLine.split(" ")[0]) {
            case "push" -> Command.C_PUSH;
            case "pop" -> Command.C_POP;
            default -> Command.C_ARITHMETIC;
        };
    }

    public String arg1(){
        return currLine.split(" ")[0];
    }
    public int arg2(){
        return switch (currLine.split(" ")[1]){
            case "constant" -> 0;
            case "local" -> 1;
            case "argument" -> 2;
            case "this" -> 3;
            case "that" -> 4;
            case "static" -> 5;
            case "pointer" -> 6;
            case "temp" -> 7;
            default -> -1; //invalid memory segment
        };
    }
}
