import java.io.File;
import java.io.FileNotFoundException;
import java.security.Key;
import java.util.Scanner;
import java.lang.RuntimeException;


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

    public TokenType tokenType(){
        switch (currToken){
            case "class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean",
                    "void", "true", "false", "null", "this", "let", "do", "if",
                    "else", "while", "return" -> {
                return TokenType.KEYWORD;
            }
            case "{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~" -> {
                return TokenType.SYMBOL;
            }
            default ->{
                if (this.isInteger(currToken)){
                    return TokenType.INT_CONSTANT;
                }else if (currToken.startsWith("\"") && currToken.endsWith("\"")){
                    return TokenType.STRING_CONSTANT;
                }else if (this.isInteger(currToken.substring(0, 1))){
                    System.out.println("Weird token "+currToken);
                }
                return TokenType.IDENTIFIER;
            }


        }
    }

    public Keywords keyword(){
        return switch (currToken){
            case "class" -> Keywords.CLASS;
            case "constructor" -> Keywords.CONSTRUCTOR;
            case "function" -> Keywords.FUNCTION;
            case "method" -> Keywords.METHOD;
            case "field" -> Keywords.FIELD;
            case "static" -> Keywords.STATIC;
            case "var" -> Keywords.VAR;
            case "int" -> Keywords.INT;
            case "char" -> Keywords.CHAR;
            case "boolean" -> Keywords.BOOLEAN;
            case "void" -> Keywords.VOID;
            case "true" -> Keywords.TRUE;
            case "false" -> Keywords.FALSE;
            case "null" -> Keywords.NULL;
            case "this" -> Keywords.THIS;
            case "let" -> Keywords.LET;
            case "do" -> Keywords.DO;
            case "if" -> Keywords.IF;
            case "else" -> Keywords.ELSE;
            case "while" -> Keywords.WHILE;
            case "return" -> Keywords.RETURN;
            default -> Keywords.FUNCTION;
        };
    }

    public char symbol(){
        return currToken.charAt(0);
    }

    public String identifier(){
        return currToken;
    }

    public String stringVal(){
        return currToken.substring(1, currToken.length() - 1);
    }

    public int intVal(){
        return Integer.parseInt(currToken);
    }

    private boolean isInteger(String input ) {
        try {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e ) {
            return false;
        }
    }





}
