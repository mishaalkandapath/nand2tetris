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
    private String currLine; //the line in the file that is being processed right now

    /**
     * Constructor to accept a file and open it
     * @param filename name of the file to open
     * @throws FileNotFoundException when the given filename does not exist
     */
    public Parser(String filename) throws FileNotFoundException {
        File vmFile = new File(filename);
        this.vmScannedFile = new Scanner(vmFile); //using a scanner for efficiency
    }

    /**
     * check if there are more lines to process in the file
     * @return whether there are more lines
     */
    public boolean hasMoreCommands(){
        return vmScannedFile.hasNextLine();
    }

    /**
     * move on to the next line
     * call only if there are more lines
     */
    public void advance(){

        currLine = vmScannedFile.nextLine();
        while ((currLine.contains("//") || currLine.length() == 0) && hasMoreCommands()){
            currLine = vmScannedFile.nextLine();
        }
    }

    /**
     * process the type of command that has been typed out
     * @return the suitable from the Command enum
     */
    public Command commandType(){
        return switch (currLine.split(" ")[0]) {
            case "push" -> Command.C_PUSH;
            case "pop" -> Command.C_POP;
            case "neg", "not" -> Command.C_ARITHMETIC_UN;
            default -> Command.C_ARITHMETIC_BIN;
        };
    }

    /**
     * @return the first argument in command. Do not call if line of type C_RETURN
     */
    public String arg1(){
        return currLine.split(" " ).length == 1 ? currLine.split(" ")[0] : currLine.split(" ")[1];
    }

    /**
     * Call only for C_POP, C_PUSH, C_FUNCTION, C_CALL
     * @return appropriate memory segment
     */
    public int arg2(){
        return Integer.parseInt(currLine.split(" ")[2]);
    }

    public void close(){
        this.vmScannedFile.close();
    }
}
