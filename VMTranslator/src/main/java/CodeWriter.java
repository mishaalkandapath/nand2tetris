package main.java;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CodeWriter {

    /*
    Generating Hack Assembly code from parsed VM commands

    Functionalities:
        1. Constructor to create a new output .asm file with a given input filename
        2. Writes assembly code from Arithmetic Commands
        3. Writes assembly code from PushPop Commands
        4. Closes file

     */
    private final HashMap<String, String> operatorSymbols;
    private final HashMap<String, Integer> memorySegmentIndices;
    private final BufferedWriter fileWriter;
    private String filename;


    /**
     * create a buffered file writer with the given name to create filename.asm
     * @param filename the given filename for filename.asm
     * @throws IOException if such a file cannot be created or does not exist
     */
    public CodeWriter(String filename, boolean sysInitPresent, HashMap<String, String> operatorSymbols, HashMap<String, Integer> memorySegmentIndices) throws IOException {
        this.fileWriter= new BufferedWriter(new FileWriter(filename + "asm"));
        this.operatorSymbols = operatorSymbols;
        this.memorySegmentIndices = memorySegmentIndices;
        this.filename = "";

        if (sysInitPresent){
            writeInit();
        }
    }

    public void setFilename(String filename){
        this.filename = filename;
    }

    public void writeInit() throws IOException{
        //there is a sysinit, so run the code to call it,
        //this should be called always, but the course testing is weird sigh
        //set the stack value
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=256");
        this.fileWriter.newLine();
        //set the LCL to be the same as the stack for now
        this.fileWriter.write("@LCL");
        this.fileWriter.newLine();
        this.fileWriter.write("M=256");
        this.fileWriter.newLine();
        //similarly for arg:
        //set the LCL to be the same as the stack for now
        this.fileWriter.write("@ARG");
        this.fileWriter.newLine();
        this.fileWriter.write("M=256");
        this.fileWriter.newLine();
        //everything else is of global memory scope, or is this or that

        //call the sys.init function if it exists
        saveStateCallFn("Sys.init", "ret", 0, 0);
    }

    /**
     * write the arithmetic command
     * @param command the given arithmetic command
     * @throws IOException if I/O error
     */
    public void writeArithmeticUnary(String command) throws IOException {
        accessLastInStack();
        this.fileWriter.write(String.format("M=%sM", this.operatorSymbols.get(command)));
        this.fileWriter.newLine();
    }

    public int writeArithmeticBinary(String command, int lineCount) throws IOException{
        accessLastInStack();
        this.fileWriter.write("D=M"); //store the last element in the stack to the data register
        this.fileWriter.newLine();
        this.fileWriter.write("A=A-1"); //go to the second last element in the stack
        this.fileWriter.newLine();
        switch (command){
            case "lt", "gt", "eq" ->{
                this.fileWriter.write(String.format("@%s", "boolean" + lineCount));
                this.fileWriter.newLine();
                this.fileWriter.write(String.format("M-D;%s", this.operatorSymbols.get(command)));
                this.fileWriter.newLine();
                //write the false condition
                this.fileWriter.write("M=0");
                this.fileWriter.newLine();
                this.fileWriter.write("@skipTrue"+lineCount);
                this.fileWriter.newLine();
                this.fileWriter.write("M;JMP");
                this.fileWriter.newLine();
                //write boolean label
                this.fileWriter.write("(boolean"+lineCount+")");
                this.fileWriter.newLine();
                this.fileWriter.write("M=1");
                this.fileWriter.newLine();
                //write the skip part
                this.fileWriter.write("(skipTrue"+lineCount+")");
                this.fileWriter.newLine();
                decrementStack();
                return 1;
            }
            default -> {
                this.fileWriter.write(String.format("M=M%sD", this.operatorSymbols.get(command)));
                this.fileWriter.newLine();
                decrementStack();
                return 0;
            }
        }
    }

    public void writePushPop(Command command, String segment, int index) throws IOException {
        System.out.println(segment);
        if (command == Command.C_PUSH){
            accessMem(segment, index, true);
            accessLastInStack();
            this.fileWriter.write("A=A+1"); //offset to new location
            this.fileWriter.newLine();
            //need this value in D register
            this.fileWriter.write("M=D");
            this.fileWriter.newLine();
            incrementStack();
        }else{
            //pop
            accessLastInStack();
            this.fileWriter.write("D=M");
            this.fileWriter.newLine();
            accessMem(segment, index, false);
            this.fileWriter.write("M=D");
            this.fileWriter.newLine();
            decrementStack();
        }
    }

    public void writeLabel(String label) throws IOException {
        this.fileWriter.write(this.filename + "$" + label);
        this.fileWriter.newLine();
    }



    public void close() throws IOException {
        this.fileWriter.close();
    }

    //writer helper methods:

    private void saveStateCallFn(String funcName, String label, int count, int argNo) throws IOException { //count is used if more than one call to another function in a function
        //store the value to run's address in stack
        String finalLabel = this.filename.equals("") ? label : "@"+this.filename + "$" + label + "." + count;

        this.fileWriter.write("@" + finalLabel);
        this.fileWriter.newLine();

        //store the value in D
        this.fileWriter.write("D=A");
        this.fileWriter.newLine();
        //set the stack pointer to the return address
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
        //increment thestack
        incrementStack();

        //save the LCL, ARG etc. addresses here
        List<String> memAreas = Arrays.asList("LCL", "ARG", "THIS", "THAT");

        for (String area: memAreas){
            saveAddress(area);
            incrementStack();
        }
        //modify arg address as sp - n - 5
        this.fileWriter.write("D=" + Integer.toString(5 + argNo));
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M-D");
        this.fileWriter.newLine();

        this.fileWriter.write("@ARG");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");

        //set the lcl address as the current stack address:
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M");
        this.fileWriter.newLine();
        this.fileWriter.write("@LCL");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();

        //go the required label of the function
        this.fileWriter.write("@"+funcName);
        this.fileWriter.newLine();
        this.fileWriter.write("0;JMP"); //jump to label
        this.fileWriter.newLine();
        this.fileWriter.write("(" + finalLabel + ")");

    }


    private void saveAddress(String memLabel) throws IOException {
        this.fileWriter.write("@"+memLabel);
        this.fileWriter.newLine();
        //store the address value in D
        this.fileWriter.write("D=M");
        this.fileWriter.newLine();
        saveIntoStack();
    }

    private void accessLastInStack() throws IOException{
        this.fileWriter.write("@SP"); //get the current pointer to the stack
        this.fileWriter.newLine();
        this.fileWriter.write("A=M-1"); //move to last element in stack
        this.fileWriter.newLine();
    }
    private void decrementStack() throws IOException {
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=M-1");
        this.fileWriter.newLine();
    }

    private void incrementStack() throws IOException{
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=M+1");
        this.fileWriter.newLine();
    }

    private void saveIntoStack() throws IOException{
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
    }

    private void accessMem(String segment, int index, boolean isPush) throws IOException{
        switch (segment) {
            case "this", "that"-> {
                this.fileWriter.write(String.format("@%s", this.memorySegmentIndices.get(segment)));
                this.fileWriter.newLine();
            }
            case "temp" -> {
                this.fileWriter.write(String.format("@%d", index + this.memorySegmentIndices.get(segment)));
                this.fileWriter.newLine();
            }
            case "static" -> {
                this.fileWriter.write(String.format("@%s.%d", filename, index));
                this.fileWriter.newLine();
            }
            case "pointer" -> {
                this.fileWriter.write(String.format("@%d", index == 1 ? this.memorySegmentIndices.get("this") : this.memorySegmentIndices.get("that")));
                this.fileWriter.newLine();
            }
            case "constant" -> {
                this.fileWriter.write(String.format("D=%d", index));
                this.fileWriter.newLine();
            }
            default -> {
                this.fileWriter.write(String.format("@%s", this.memorySegmentIndices.get(segment)));
                this.fileWriter.newLine();
                this.fileWriter.write(String.format("A=M+%d", index));
                this.fileWriter.newLine();
            }
        }

        if (!segment.equals("constant")) { // there is no pop for constant
            this.fileWriter.write(String.format("%s=M", isPush ? "D":"M"));
            this.fileWriter.newLine();
        }
    }


}
