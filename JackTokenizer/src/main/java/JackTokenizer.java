import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class JackTokenizer {


    private final Scanner jackScannedFile;
    private String currToken;

    public JackTokenizer(String filename) throws FileNotFoundException {
        File jackFile = new File(filename + ".jack");
        this.jackScannedFile = new Scanner(jackFile);
    }

    public boolean hasMoreTokens(){
        return this.jackScannedFile.hasNext(); // has a token remaining
    }

    public void advance(){
        this.currToken = this.jackScannedFile.next();
    }


}
