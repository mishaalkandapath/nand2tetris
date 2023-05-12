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

    private Scanner vmScannedFile;

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
        //
    }
}
