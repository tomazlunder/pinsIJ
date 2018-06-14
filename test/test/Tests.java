package test;

import compiler.Report;
import compiler.abstr.tree.AbsDef;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsTree;
import compiler.frames.Frames;
import compiler.frames.FrmDesc;
import compiler.frames.FrmEvaluator;
import compiler.frames.FrmFrame;
import compiler.imcode.ImCode;
import compiler.imcode.ImcCONST;
import compiler.imcode.ImcCodeGen;
import compiler.interpreter.Interpreter;
import compiler.lexan.LexAn;
import compiler.lexan.Token;
import compiler.seman.*;
import compiler.synan.SynAn;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.LinkedList;

public abstract class Tests {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    protected LinkedList<Integer> intInputs;
    protected LinkedList<String> stringInputs;

    protected final String pre = "TEST: Konec lex\nTEST: Konec syn\nTEST: Abst drevo zgrajeno\nTEST: Seman imena success\nTEST: Seman tipi success\nTEST: Frames constructed\nTEST: Intermediatecode chunkified\n";

    @BeforeEach
    public void prepare(){
        SymbTable.reset();
        SymbDesc.reset();

        intInputs = new LinkedList<>();
        stringInputs = new LinkedList<>();
    }

    /**
     * Runs the compiler on the given file, debugging is disabled.
     */
    protected void run(String sourceFileName) throws Exception {
        boolean interpret = true;
        boolean interpretDbg = false;

        //Lexan - 0
        LexAn lexAn = new LexAn(sourceFileName, false);
        while (lexAn.lexAn().token != Token.EOF) {
        }
        System.out.printf("TEST: Konec lex\n");

        // Sintaksna analiza - 1
        lexAn = new LexAn(sourceFileName, false);
        SynAn synAn = new SynAn(lexAn, false);
        synAn.parse();
        System.out.printf("TEST: Konec syn\n");

        //Abst drevo (grajenje) - 2
        lexAn = new LexAn(sourceFileName, false);
        synAn = new SynAn(lexAn, false);
        AbsTree source = synAn.parse();

        System.out.printf("TEST: Abst drevo zgrajeno\n");


        //Seman imena - 3
        lexAn = new LexAn(sourceFileName, false);
        synAn = new SynAn(lexAn, false);
        source = synAn.parse();
        SemAn semAn = new SemAn(false);
        source.accept(new NameChecker());
        //semAn.dump(source);
        //Report.closeDumpFile();
        System.out.printf("TEST: Seman imena success\n");

        //Seman tipi - 4
        source.accept(new TypeChecker());
        //semAn.dump(source);

        System.out.printf("TEST: Seman tipi success\n");

        // Klicni zapisi - 5
        Frames frames = new Frames(false);
        source.accept(new FrmEvaluator());
        frames.dump(source);

        System.out.printf("TEST: Frames constructed\n");


        ImCode imcode = new ImCode(false);
        ImcCodeGen imcodegen = new ImcCodeGen();
        source.accept(imcodegen);
        imcode.dump(imcodegen.chunks);

        System.out.printf("TEST: Intermediatecode chunkified\n");

        //**INTERPRETER TESTING**//
        if (interpret) {
            Interpreter.debug = false;

            //Najde definicijo funkcije main ali pa konča izvajanje, če definicije ne najde
            AbsDef mainDef = SymbTable.fnd("main");

            //Če definicija ni funkcija (npr. globalana spremenljiva)
            if (!(mainDef instanceof AbsFunDef)) Report.error("Main mora biti funkcija.");
            AbsFunDef mainFunDef = (AbsFunDef) mainDef;

            //Najde okvir funkcije main
            FrmFrame mainFrame = FrmDesc.getFrame((AbsFunDef) mainDef);

            //Argument main funkcije nastavimo na 0 (1000 - začetna vrednost FP, FP+4 naj bi bil prvi argument main funkcije)
            Interpreter.stM(1000 + 4, new ImcCONST(0));

            //If we have predefined inputs it runs the Interpreter with those settings
            if(!intInputs.isEmpty() || !stringInputs.isEmpty()){
                new Interpreter(mainFrame, ImcCodeGen.linearCode.get(mainDef), intInputs, stringInputs);
            } else {
                new Interpreter(mainFrame, ImcCodeGen.linearCode.get(mainDef));
            }
        }

    }

    /**
     * Turns on stdOut output capture
     */
    protected void captureOut() {
        System.setOut( new PrintStream( outContent ) );
    }

    /**
     * Turns on stdErr output capture
     */
    private void captureErr() {
        System.setErr( new PrintStream( errContent ) );
    }

    /**
     * Turns off stdOut capture and returns the contents
     * that have been captured
     */
    protected String getOut() {
        System.setOut( new PrintStream( new FileOutputStream( FileDescriptor.out ) ) );
        return outContent.toString().replaceAll( "\r", "" );

    }

    /**
     * Turns off stdErr capture and returns the contents
     * that have been captured
     */
    private String getErr() {
        System.setErr( new PrintStream( new FileOutputStream( FileDescriptor.out ) ) );
        return errContent.toString().replaceAll( "\r", "" );
    }
}
