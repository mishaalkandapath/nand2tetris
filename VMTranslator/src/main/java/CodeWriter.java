package main.java;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

        if (sysInitPresent){ //normally should assume this exists, role of compiler to comply
            writeInit();
        }
    }

    public void setFilename(String filename){
        this.filename = filename;
    }

    public void writeNewLine() throws IOException{ //used outside te seperate commands, j a debug feature not really required
        this.fileWriter.newLine();
    }

    /**
     * sets up the memory segments for the program, and calles the sys.init function
     * @throws IOException if file error
     */
    public void writeInit() throws IOException{
        //there is a sysinit, so run the code to call it,
        //this should be called always, but the course testing is weird sigh
        //set the stack value
        this.fileWriter.write("@256");
        this.fileWriter.newLine();
        this.fileWriter.write("D=A");
        this.fileWriter.newLine();
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
        //set the LCL to be the same as the stack for now
        this.fileWriter.write("@LCL");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
        //similarly for arg:
        //set the LCL to be the same as the stack for now
        this.fileWriter.write("@ARG");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
        //everything else is of global memory scope, or is this or that

        //call the sys.init function if it exists
        writeCall("Sys.init", 0, 0);
    }

    /**
     * write the arithmetic command for not and neg
     * @param command the given arithmetic command
     * @throws IOException if I/O error
     */
    public void writeArithmeticUnary(String command) throws IOException {
        accessLastInStack();
        this.fileWriter.write(String.format("M=%sM", this.operatorSymbols.get(command)));
        this.fileWriter.newLine();
    }

    /**
     * write binary arithmetic commands
     * @param command
     * @param lineCount
     * @return
     * @throws IOException
     */
    public int writeArithmeticBinary(String command, int lineCount) throws IOException{
        accessLastInStack();
        this.fileWriter.write("D=M"); //store the last element in the stack to the data register
        this.fileWriter.newLine();
        this.fileWriter.write("A=A-1"); //go to the second last element in the stack
        this.fileWriter.newLine();
        switch (command){
            case "lt", "gt", "eq" ->{
                //compute the difference first
                writeComparativeArithmetic(command, lineCount);
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
        this.fileWriter.write("(" + this.filename + "$" + label + ")");
        this.fileWriter.newLine();
    }

    public void writeGoto(String label) throws  IOException{
        this.fileWriter.write("@" + this.filename + "$" + label);
        this.fileWriter.newLine();
        this.fileWriter.write("0;JMP");
        this.fileWriter.newLine();
    }

    public void writeIf(String label) throws IOException{
        accessLastInStack();
        this.fileWriter.write("D=M"); //store the last value in the data register
        this.fileWriter.newLine();
        decrementStack();
        this.fileWriter.write("@"+ this.filename + "$" + label);
        this.fileWriter.newLine();

        //this section is due to an abhorrent behaviour of the computer -
        this.fileWriter.write("!D;JEQ"); //if D is true, jump
        this.fileWriter.newLine();
        this.fileWriter.write("-D;JLT");
        this.fileWriter.newLine();
    }

    public void writeFunction(String functionName, int numVars) throws IOException {
        this.fileWriter.write("(" + functionName + ")");
        this.fileWriter.newLine();
        if (numVars != 0){ //if there are non, nothign to set up really
            //set up the local variables as 0
            this.fileWriter.write("@LCL");
            this.fileWriter.newLine();

            this.fileWriter.write("A=M");
            this.fileWriter.newLine();
            this.fileWriter.write("M=0");
            this.fileWriter.newLine();

            for (int i=0; i < numVars-1; i++){
                this.fileWriter.write("A=A+1");
                this.fileWriter.newLine();
                this.fileWriter.write("M=0");
                this.fileWriter.newLine();
            }

            this.fileWriter.write("D=A+1");
            this.fileWriter.newLine();

            //setup stack
            this.fileWriter.write("@SP");
            this.fileWriter.newLine();
            this.fileWriter.write("M=D");
            this.fileWriter.newLine();

        }
    }

    public void writeCall(String functionName, int numArgs, int count) throws IOException{

        //store the value to run's address in stack
        String finalLabel = this.filename.equals("") ? "ret" : this.filename + "$" + "ret" + "." + count;

        this.fileWriter.write("@" + finalLabel);
        this.fileWriter.newLine();

        //store the value in D
        this.fileWriter.write("D=A");
        this.fileWriter.newLine();
        //set the stack pointer to the return address
        this.accessLastInStack();
        this.fileWriter.write("A=A+1"); //go the empty spot
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
        this.fileWriter.write("@"+ (5 + numArgs));
        this.fileWriter.newLine();
        this.fileWriter.write("D=A");
        this.fileWriter.newLine();
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M-D");
        this.fileWriter.newLine();

        this.fileWriter.write("@ARG");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();

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
        this.fileWriter.write("@"+functionName);
        this.fileWriter.newLine();
        this.fileWriter.write("0;JMP"); //jump to label
        this.fileWriter.newLine();
        this.fileWriter.write("(" + finalLabel + ")"); //this is the place to return to
        this.fileWriter.newLine();

    }

    public void writeReturn() throws IOException {
        //can assume the top most value in the stack is the value to return
        //access local addr:
        this.fileWriter.write("@LCL");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M"); //store the address in D
        this.fileWriter.newLine();
        this.fileWriter.write("@R13"); //using temp variable to store the lcl address currently
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
        //get the address of the return address
        this.fileWriter.write("@R14");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
        this.fileWriter.write("@5");
        this.fileWriter.newLine();
        this.fileWriter.write("D=A");
        this.fileWriter.newLine();
        this.fileWriter.write("@R14");
        this.fileWriter.newLine();
        this.fileWriter.write("M=M-D"); // this is the address of where the return address is stored
        this.fileWriter.newLine();

        // get the return address:
        this.fileWriter.write("A=M");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M"); //this is the value of the return addresss
        this.fileWriter.newLine();
        this.fileWriter.write("@R14");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D"); //store into r1, another tmep variable
        this.fileWriter.newLine();

        //pop the stack into the arg
        writePushPop(Command.C_POP, "argument", 0); //moved the return value into the space of the first argument
        //re-establish the stack pointer:
        this.fileWriter.write("@ARG");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M");
        this.fileWriter.newLine();
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=D+1"); //new stack position
        this.fileWriter.newLine();

        //setting that, this, arg, and local
        List<String> memAreas = Arrays.asList("THAT", "THIS", "ARG", "LCL");
        for (String area: memAreas){
            restoreAddresses(area);
        }

        //goto retaddr:
        this.fileWriter.write("@R14"); //acess the return address
        this.fileWriter.newLine();
        this.fileWriter.write("A=M"); // get the address
        this.fileWriter.newLine();
        this.fileWriter.write("0;JMP"); //jump to return address
        this.fileWriter.newLine();
    }

    /**
     * close the file stream wen over
     * @throws IOException for file based errors
     */
    public void close() throws IOException {
        this.fileWriter.close();
    }

    //writer helper methods:

    /**
     * get the address of where to return
     * @param memLabel name of label of command to resume upon return
     * @throws IOException for file based errors
     */
    private void saveAddress(String memLabel) throws IOException {
        this.fileWriter.write("@"+memLabel);
        this.fileWriter.newLine();
        //store the address value in D
        this.fileWriter.write("D=M");
        this.fileWriter.newLine();
        saveIntoStack();
    }

    /**
     * to restore segments on returning from a function
     * @param segment can be of LCL, ARG, THIS, or THAT
     * @throws IOException for file based errors
     */
    private void restoreAddresses(String segment) throws IOException{

        this.fileWriter.write("@R13");
        this.fileWriter.newLine();
        this.fileWriter.write("D=M-1"); //this is the address where the address to segment is stored
        this.fileWriter.newLine();
        this.fileWriter.write("M=M-1"); //decrement M by 1 for next time
        this.fileWriter.newLine();

        this.fileWriter.write("A=D"); //go to the address for segment
        this.fileWriter.newLine();
        this.fileWriter.write("D=M"); //stores the address for segment
        this.fileWriter.newLine();

        this.fileWriter.write("@"+segment);
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
    }

    /**
     * select the address of last element in stack
     * @throws IOException for file based errors
     */
    private void accessLastInStack() throws IOException{
        this.fileWriter.write("@SP"); //get the current pointer to the stack
        this.fileWriter.newLine();
        this.fileWriter.write("A=M-1"); //move to last element in stack
        this.fileWriter.newLine();
    }

    /**
     * select addr of last element in stack
     * required for binary operations
     * @throws IOException for file based errors
     */
    private void accessSecondLastInStack() throws IOException{
        this.fileWriter.write("@SP"); //eat two of the current stack values
        this.fileWriter.newLine();
        this.fileWriter.write("A=M-1");
        this.fileWriter.newLine();
        this.fileWriter.write("A=A-1");//decrease by one more for M-2
        this.fileWriter.newLine();
    }

    /**
     * decrement the stack pointer
     * @throws IOException for file based errors
     */
    private void decrementStack() throws IOException {
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=M-1");
        this.fileWriter.newLine();
    }

    /**
     * increment the stack pointer
     * @throws IOException for file based errors
     */
    private void incrementStack() throws IOException{
        this.fileWriter.write("@SP");
        this.fileWriter.newLine();
        this.fileWriter.write("M=M+1");
        this.fileWriter.newLine();
    }

    /**
     * write the value in D (specified outside) into the stack
     * @throws IOException for file related errors
     */
    private void saveIntoStack() throws IOException{
        this.accessLastInStack();
        this.fileWriter.write("A=A+1"); //go the empty spot
        this.fileWriter.newLine();
        this.fileWriter.write("M=D");
        this.fileWriter.newLine();
    }

    /**
     * access respective mem segment
     * @param segment the segment in question: static, constant, etc
     * @param index of the segment in question
     * @param isPush indicates whether the current argument being processed is a push or pop
     * @throws IOException if file error
     */
    private void accessMem(String segment, int index, boolean isPush) throws IOException{
        switch (segment) {
            case "this", "that"-> {
                this.fileWriter.write("@"+ (segment.equals("this") ? "THIS" : "THAT"));
                this.fileWriter.newLine();
                accessLocalMem(isPush, index);
            }
            case "temp" -> {
                this.fileWriter.write(String.format("@%d", index + this.memorySegmentIndices.get(segment)));
                this.fileWriter.newLine();
                accessGlobalMem(isPush);
            }
            case "static" -> {
                this.fileWriter.write(String.format("@%s.%d", filename, index));
                this.fileWriter.newLine();
                accessGlobalMem(isPush);

            }
            case "pointer" -> {
                this.fileWriter.write(String.format("@%s", index == 0 ? "THIS" : "THAT"));
                this.fileWriter.newLine();
                accessGlobalMem(isPush);

            }
            case "constant" -> {
                this.fileWriter.write("@"+index);
                this.fileWriter.newLine();
                this.fileWriter.write("D=A");
                this.fileWriter.newLine();
            }
            case "local" -> {
                this.fileWriter.write("@LCL");
                this.fileWriter.newLine();
                accessLocalMem(isPush, index);
            }
            case "argument" -> {
                this.fileWriter.write("@ARG");
                this.fileWriter.newLine();
                accessLocalMem(isPush, index);
            }
        }
    }

    /**
     * access static, pointer, temp
     * @param isPush indicates whether the commmand is a push or a pop
     * @throws IOException if file error
     */
    private void accessGlobalMem(boolean isPush) throws IOException {
        this.fileWriter.write(String.format("%s=%s", isPush ? "D":"M", isPush ? "M":"D"));
        this.fileWriter.newLine();
    }

    /**
     * access local, argument, stack, this, and that.
     * @param isPush to indicate if the current command is push or pop
     * @param index of memory segment being queried
     * @throws IOException for file error
     */
    private void accessLocalMem(boolean isPush, int index) throws IOException{
        this.fileWriter.write("A=M");
        this.fileWriter.newLine();

        //offset by index if needed:
        for (int i = 0; i<index; i++){
            this.fileWriter.write("A=A+1");
            this.fileWriter.newLine();
        }

        this.fileWriter.write(String.format("%s=M", isPush ? "D":"M"));
        this.fileWriter.newLine();

    }

    /**
     * helper function to write comparative functions, due to the presence of branching
     * @param command the command being parsed and generated
     * @param lineCount the number of branch statements generated in this code.
     * @throws IOException if file error
     */
    private void writeComparativeArithmetic(String command, int lineCount) throws IOException {
        this.fileWriter.write("D=M-D");
        this.fileWriter.newLine();

        this.fileWriter.write(String.format("@%s", "boolean" + lineCount));
        this.fileWriter.newLine();
        this.fileWriter.write(String.format("D;%s", this.operatorSymbols.get(command)));
        this.fileWriter.newLine();
        //write the false condition
        accessSecondLastInStack();

        this.fileWriter.write("M=0");//vomit a false
        this.fileWriter.newLine();
        this.fileWriter.write("@skipTrue"+lineCount);
        this.fileWriter.newLine();
        this.fileWriter.write("D;JMP");
        this.fileWriter.newLine();
        //write boolean label
        this.fileWriter.write("(boolean"+lineCount+")");
        this.fileWriter.newLine();
        accessSecondLastInStack();
        this.fileWriter.write("M=-1");//vomit a true; the eat-vomit cycle - Mishaal Kandapath
        this.fileWriter.newLine();
        //write the skip part
        this.fileWriter.write("(skipTrue"+lineCount+")");
        this.fileWriter.newLine();
        decrementStack();
    }


}
