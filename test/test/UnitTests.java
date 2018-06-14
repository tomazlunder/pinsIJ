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
import org.junit.jupiter.api.*;

import java.io.*;

public class UnitTests {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final String pre = "TEST: Konec lex\nTEST: Konec syn\nTEST: Abst drevo zgrajeno\nTEST: Seman imena ok\nTEST: Seman tipi ok\nTEST: Frames constructed\nTEST: Intermediatecode chunkified\n";

    @BeforeEach
    public void prepare(){
        SymbTable.reset();
        SymbDesc.reset();
    }

    @Test
    public void simple() throws Exception{
        String fileName = "test/resources/ok/simple.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
            + "5\n" + "10\n" + "15\n" + "3\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void math() throws Exception{
        String fileName = "test/resources/ok/math.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "30\n" + "10\n" + "200\n" + "2\n" + "3\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void functions() throws Exception{
        String fileName = "test/resources/ok/functions.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "30\n" + "-10\n" + "99\n" + "76\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void functions2() throws Exception{
        String fileName = "test/resources/ok/functions2.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "1\n" + "2\n" + "3\n" + "4\n" + "5\n" + "10\n" + "-100\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void loops() throws Exception{
        String fileName = "test/resources/ok/loops.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "1\n" + "2\n" + "3\n" + "4\n" + "5\n" + "1\n" + "2\n" + "3\n" + "4\n" + "5\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void types() throws Exception{
        String fileName = "test/resources/ok/types.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "10\n" + "20\n" + "30\n" + "'40str'\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void logical() throws Exception{
        String fileName = "test/resources/ok/logical.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        Assertions.assertFalse(output.contains("0"));
    }

    @Test
    public void putInt() throws Exception{
        String fileName = "test/resources/ok/putInt.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "3\n" + "6\n" + "9\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    @Disabled
    public void getInt() throws Exception{
        String fileName = "test/resources/ok/getInt.pins";
        captureOut();

        Thread myInputThread = new Thread(){
            @Override
            public void run() {
                try {
                    sleep(3000);
                    InputStream in = new ByteArrayInputStream("1111".getBytes());
                    System.setIn(in);

                    sleep(3000);
                    in = new ByteArrayInputStream("2222".getBytes());
                    System.setIn(in);

                    sleep(3000);
                    System.setIn(System.in);
                } catch (Exception e){
                    System.out.println("Thread exception thingy.");
                }
            }
        };

        myInputThread.run();
        run(fileName);
        myInputThread.join();


        String output = getOut();
        int a = 3;
    }

    @Test
    public void putString() throws Exception{
        String fileName = "test/resources/ok/putString.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "'const!'\n" + "'global!'\n" + "'local!'\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    @Disabled
    public void getString() throws Exception{
        String fileName = "test/resources/ok/getString.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "3\n" + "6\n" + "9\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void arrays() throws Exception{
        String fileName = "test/resources/ok/arrays.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "0\n" + "-100\n" + "-101\n"+ "-102\n" + "-103\n" + "1\n" + "-200\n" + "-201\n" + "-202\n" + "-203\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void arrays2() throws Exception{
        String fileName = "test/resources/ok/arrays2.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "-100\n" + "-104\n" + "-200\n"+ "-204\n" + "100\n" + "102\n" + "200\n" + "202\n";

        Assertions.assertEquals(expected,output);
    }


    /**
     * Zažene prevajanje nad podano vhodno datoteko.
     * Prevajanje izvaja brez debugganja za lažje primerjanje rezultata
     * @param sourceFileName
     * @throws Exception
     */
    public void run(String sourceFileName) throws Exception {
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
        System.out.printf("TEST: Seman imena ok\n");

        //Seman tipi - 4
        source.accept(new TypeChecker());
        //semAn.dump(source);

        System.out.printf("TEST: Seman tipi ok\n");

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

            new Interpreter(mainFrame, ImcCodeGen.linearCode.get(mainDef));
        }

    }

    /**
     * Turns on stdOut output capture
     */
    private void captureOut() {
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
     *
     * @return
     */
    private String getOut() {
        System.setOut( new PrintStream( new FileOutputStream( FileDescriptor.out ) ) );
        return outContent.toString().replaceAll( "\r", "" );

    }

    /**
     * Turns off stdErr capture and returns the contents
     * that have been captured
     *
     * @return
     */
    private String getErr() {
        System.setErr( new PrintStream( new FileOutputStream( FileDescriptor.out ) ) );
        return errContent.toString().replaceAll( "\r", "" );
    }
}
