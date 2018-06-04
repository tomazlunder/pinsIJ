package compiler.lexan;

import compiler.*;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Leksikalni analizator.
 *
 * @author sliva
 */
public class LexAn {

    /**
     * Ali se izpisujejo vmesni rezultati.
     */
    private boolean dump;
    private BufferedReader br;
    private int line, column;

    /**
     * Ustvari nov leksikalni analizator.
     *
     * @param sourceFileName Ime izvorne datoteke.
     * @param dump Ali se izpisujejo vmesni rezultati.
     * @throws java.lang.Exception
     */
    
    
    public LexAn(String sourceFileName, boolean dump) throws Exception {
        this.br = new BufferedReader(new FileReader(sourceFileName));
        this.dump = dump;
        this.line = 1;
        this.column = 0;
    }

    /**
     * Vrne naslednji simbol iz izvorne datoteke. Preden vrne simbol, ga izpise
     * na datoteko z vmesnimi rezultati.
     *
     * @return Naslednji simbol iz izvorne datoteke.
     */
    public Symbol lexAn() throws Exception {
        int character;
        String word = "";
        boolean done = false;
        Symbol to_return = null;

        while (br.ready() && !done) {
            //System.out.printf("Reading...\n"); //DBG
            character = this.br.read();
            column++;
            
            //Ascii check
            if(character < 0 || character > 127){
                Report.error(line,column, "Invalid character '" + Character.toString((char) character) + "'");
                return null;
            }
            
            //Space
            if (character == 32) {
                continue;
            }
            //Tab
            if (character == 9) {
                this.column += 3;
                continue;
            }
            //If there is a symbol for new line
            if (character == 13) {
                br.mark(2);
                character = br.read();
                //CRLF line ending
                if(character == 10){
                    this.line++;
                    this.column = 0;
                    continue;
                }
                else{
                    //CR line ending??
                    br.reset();
                    this.line++;
                    this.column = 0;
                    continue;
                }
            }
            
            //LF line ending
            if (character == 10) {
                this.line++;
                this.column = 0;
                continue;
            }

            //If the character is # the whole line is a comment (thrown away)
            if (character == 35) {
                //System.out.printf("Comment\n"); //DBG
                while (br.ready()) {
                    character = br.read();
                    //Reads and passes everything untill it reaches a symbol for new line
                    if (character == 10 || character == 13) {
                        this.line++;
                        this.column = 0;
                        break;
                    }
                }
                //System.out.printf("comment end.\n"); //DBG
                continue;
            }
            

            //Ascii 48-57 are digits. If the first character is a digit,
            //we are dealing with a positive integer.
            else if ((48 <= character && character < 58)) {
                //System.out.printf("Number\n");
                word += Integer.toString(character - 48);
                br.mark(2);
                while (br.ready()) {
                    character = br.read();
                    if (48 <= character && character < 58) {
                        column++;
                        br.mark(2);
                        word += Integer.toString(character - 48);
                    } else {
                        br.reset();
                        to_return = new Symbol(Token.INT_CONST, word, line, column - (word.length() - 1), line, column); // INT_CONST
                        done = true;
                        break;
                        //System.out.printf("Int_const read = %s\n",ret.toString());
                    }
                }
            }
            
            //String
            else if(character == 39){
                //System.out.printf("String\n"); //DBG
                boolean closed = false;
                while(br.ready()){
                    character = br.read();
                    column++;
                    //Close string symbol
                    if(character == 39){
                        br.mark(2);
                        character = br.read();
                        if(character == 39){ //Podvojen... dodaj v string in nadaljuj
                            word += Character.toString((char) character);
                        } else {
                            br.reset();
                            closed = true;
                            break;
                        }
                    //New line symbol - error
                    } else if (character == 10 || character == 13){ 
                        Report.error(line, column, "String not closed error (new line).");
                        return null;
                    //Ascii simbol, all ok
                    } else if (character > 31 && character < 127) {
                        word += Character.toString((char) character);
                    //Symbol is not ascii - error
                    } else {
                        Report.error(line, column, "Invalid character '" + Character.toString((char) character) +"' in string const.");
                        return null;
                    }
                }
                //If can't read anymore, and string is not closed - error
                if(!closed){
                    Report.error(line,column, "String not closed error (eof).");
                    return null;
                }
                
                to_return = new Symbol(Token.STR_CONST,"'"+word+"'", line ,column-word.length(), line, column);
                done = true;
            }
            
            //Ascii 
            //65-90 A-Z, 97-122 a-z, 95 _
            //If the word begins with A-Z/a-z/_ it is either a name, key word, atomic data type or logical value;
            else if ((65 <= character && character < 91) || (97 <= character && character < 123) || character == 95) {
                //System.out.printf("Sklop črk/številk/_ \n"); //DBG
                word += Character.toString((char) character);
                br.mark(2);
                while (true) {
                    character = br.read();
                    if ((65 <= character && character < 91) || (97 <= character && character < 123) || character == 95 || (48 <= character && character < 58)) {
                        column++;
                        br.mark(2);
                        word += Character.toString((char) character);
                    } else {
                        br.reset();
                        break;
                    }
                }
                //System.out.printf("Word read: '%s'\n", word); //DBG
                
                //This checks if the word that was read is a key-word or reserved
                switch (word) {
                    case "arr":
                        to_return = new Symbol(Token.KW_ARR, word, line, column - 2, line, column); break;
                    case "else":
                        to_return = new Symbol(Token.KW_ELSE, word, line, column - 3, line, column); break;
                    case "for":
                        to_return = new Symbol(Token.KW_FOR, word, line, column - 2, line, column); break;
                    case "fun":
                        to_return = new Symbol(Token.KW_FUN, word, line, column - 2, line, column); break;
                    case "if":
                        to_return = new Symbol(Token.KW_IF, word, line, column - 1, line, column); break;
                    case "then":
                        to_return = new Symbol(Token.KW_THEN, word, line, column - 3, line, column); break;
                    case "typ":
                        to_return = new Symbol(Token.KW_TYP, word, line, column - 2, line, column); break;
                    case "var":
                        to_return = new Symbol(Token.KW_VAR, word, line, column - 2, line, column); break;
                    case "where":
                        to_return = new Symbol(Token.KW_WHERE, word, line, column - 4, line, column); break;
                    case "while":
                        to_return = new Symbol(Token.KW_WHILE, word, line, column - 4, line, column); break;

                    case "logical":
                        to_return = new Symbol(Token.LOGICAL, word, line, column - 6, line, column); break;
                    case "integer":
                        to_return = new Symbol(Token.INTEGER, word, line, column - 6, line, column); break;
                    case "string":
                        to_return = new Symbol(Token.STRING, word, line, column - 5, line, column); break;

                    case "true":
                        to_return = new Symbol(Token.LOG_CONST, word, line, column - 3, line, column); break; //LOG_CONST
                    case "false":
                        to_return = new Symbol(Token.LOG_CONST, word, line, column - 4, line, column); break; //LOG_CONST
                    default:
                        to_return = new Symbol(Token.IDENTIFIER, word, line, column - (word.length() - 1), line, column);
                }
                done = true;
            }
            //Only thing left to check, if the function runs to here if the read character is a syntax simbol
            else {
                //System.out.printf("+-\n"); //DBG
                switch (character) {
                    // + lone symbol
                    case (43):
                        to_return = new Symbol(Token.ADD, "+", line, column, line, column); break;
                    // - lone symbol or negative integer
                    case (45):
                        to_return = new Symbol(Token.SUB, "-", line, column, line, column); break;
                    // * lone symbol
                    case (42):
                        to_return = new Symbol(Token.MUL, "*", line, column, line, column); break;
                    // / lone symbol
                    case (47):
                        to_return = new Symbol(Token.DIV, "/", line, column, line, column); break;
                    // % lone symbol 
                    case (37):
                        to_return = new Symbol(Token.MOD, "%", line, column, line, column); break;

                    // & lone symbol
                    case (38):
                        to_return = new Symbol(Token.AND, "&", line, column, line, column); break;
                    // | lone symbol
                    case (124):
                        to_return = new Symbol(Token.IOR, "|", line, column, line, column); break;
                    // ! lone symbol OR followed by =
                    case (33): {
                        br.mark(2);
                        if (br.read() == 61) {
                            column++;
                            to_return = new Symbol(Token.NEQ, "!=", line, column - 1, line, column);
                        } else {
                            br.reset();
                            to_return = new Symbol(Token.NOT, "!", line, column, line, column);
                        }
                        break;
                    }
                    // < lone symbol OR followed by =
                    case (60): {
                        br.mark(2);
                        if (br.read() == 61) {
                            column++;
                            to_return = new Symbol(Token.LEQ, "<=", line, column - 1, line, column);
                        } else {
                            br.reset();
                            to_return = new Symbol(Token.LTH, "<", line, column, line, column);
                        }
                        break;
                    }
                    // = lone symbol OR followed by =
                    case (61): {
                        br.mark(2);
                        if (br.read() == 61) {
                            column++;
                            to_return = new Symbol(Token.EQU, "==", line, column - 1, line, column);
                        } else {
                            br.reset();
                            to_return = new Symbol(Token.ASSIGN, "=", line, column, line, column);
                        }
                        break;
                    }
                    // > lone symbol OR followed by =
                    case (62): {
                        br.mark(2);
                        if (br.read() == 61) {
                            column++;
                            to_return = new Symbol(Token.GEQ, ">=", line, column - 1, line, column);
                        } else {
                            br.reset();
                            to_return = new Symbol(Token.GTH, ">", line, column, line, column);
                        }
                        break;
                    }

                    // ( lone symbol
                    case (40):
                        to_return = new Symbol(Token.LPARENT, "(", line, column, line, column); break;
                    // ) lone symbol
                    case (41):
                        to_return = new Symbol(Token.RPARENT, ")", line, column, line, column); break;
                    // [ lone symbol
                    case (91):
                        to_return = new Symbol(Token.LBRACKET, "[", line, column, line, column); break;
                    // ] lone symbol
                    case (93):
                        to_return = new Symbol(Token.RBRACKET, "]", line, column, line, column); break;
                    // { lone symbol
                    case (123):
                        to_return = new Symbol(Token.LBRACE, "{", line, column, line, column); break;
                    // } lone symbol
                    case (125):
                        to_return = new Symbol(Token.RBRACE, "}", line, column, line, column); break;
                    // : lone symbol
                    case (58):
                        to_return = new Symbol(Token.COLON, ":", line, column, line, column); break;
                    // ; lone symbol
                    case (59):
                        to_return = new Symbol(Token.SEMIC, ";", line, column, line, column); break;
                    // . lone symbol
                    case (46):
                        to_return = new Symbol(Token.DOT, ".", line, column, line, column); break;
                    // , lone symbol
                    case (44):
                        to_return = new Symbol(Token.COMMA, ",", line, column, line, column); break;
                    default:
                        Report.error(line,column, "Invalid character '" + Character.toString((char) character) + "'");
                        return null;
                }
                done = true;
            }
        }
        if(done){
            dump(to_return);
            return to_return;
        }
        
        //If there is nothing left to be read, returns the end of file Symbol
        to_return = new Symbol(Token.EOF, "", line, column, line, column);
        dump(to_return);
        return to_return;
    }
    


    /**
     * Izpise simbol v datoteko z vmesnimi rezultati.
     *
     * @param symb Simbol, ki naj bo izpisan.
     */
    private void dump(Symbol symb) {
        if (!dump) {
            return;
        }
        if (Report.dumpFile() == null) {
            return;
        }
        if (symb.token == Token.EOF) {
            Report.dumpFile().println(symb.toString());
        } else {
            Report.dumpFile().println("[" + symb.position.toString() + "] " + symb.toString());
        }
    }
}
