package main.java;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

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

    /**
     * create a buffered file writer with the given name to create filename.asm
     * @param filename the given filename for filename.asm
     * @throws IOException if such a file cannot be created or does not exist
     */
    public CodeWriter(String filename, HashMap<String, String> operatorSymbols, HashMap<String, Integer> memorySegmentIndices) throws IOException {
        this.fileWriter= new BufferedWriter(new FileWriter(filename + ".asm"));
        this.operatorSymbols = operatorSymbols;
        this.memorySegmentIndices = memorySegmentIndices;
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
                if (command.equals("lt")){
                    this.fileWriter.write("M-D;JLT");
                }else if (command.equals("eq")){
                    this.fileWriter.write("M-D;JEQ");
                }else{
                    this.fileWriter.write("M-D;JGT");
                }
                //write the false condition
                this.fileWriter.write("M=0");
                this.fileWriter.newLine();
                this.fileWriter.write("@skipTrue"+lineCount);
                this.fileWriter.newLine();
                this.fileWriter.write("M; JMP");
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
        if (command == Command.C_PUSH){
            accessMem(segment, index, true);
            accessLastInStack();
            this.fileWriter.write("A=A+1"); //offset to new location
            this.fileWriter.newLine();
            //need this value in D register
            this.fileWriter.write("M=D");
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

    public void close() throws IOException {
        this.fileWriter.close();
    }

    //writer helper methods:

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

    private void accessMem(String segment, int index, boolean isPush) throws IOException{
        this.fileWriter.write(String.format("@%s", this.memorySegmentIndices.get(segment)));
        this.fileWriter.newLine();
        switch (segment) {
            case "this", "that" -> {
                this.fileWriter.write(String.format("%s=M", isPush ? "D" : "M"));
                this.fileWriter.newLine();
            }
            case "static" -> {
                this.fileWriter.write(String.format("@%s", index + this.memorySegmentIndices.get("static")));
                this.fileWriter.write(String.format("%s=M", isPush ? "D" : "M"));
                this.fileWriter.newLine();
            }
            default -> {
                this.fileWriter.write(String.format("A=M+%s", index));
                this.fileWriter.newLine();
                this.fileWriter.write(String.format("%s=M", isPush ? "D":"M"));
            }
        }
    }


}
