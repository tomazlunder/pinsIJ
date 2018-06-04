/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler;

import compiler.abstr.tree.AbsDef;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsTree;
import compiler.frames.Frames;
import compiler.frames.FrmDesc;
import compiler.frames.FrmEvaluator;
import compiler.frames.FrmFrame;
import compiler.imcode.ImCode;
import compiler.imcode.ImcCONST;
import compiler.imcode.ImcChunk;
import compiler.imcode.ImcCodeGen;
import compiler.interpreter.Interpreter;
import compiler.lexan.*;
import compiler.seman.NameChecker;
import compiler.seman.SemAn;
import compiler.seman.SymbTable;
import compiler.seman.TypeChecker;
import compiler.synan.SynAn;

/**
 *
 * @author Tomaz Lunder
 */

public class MainTest {
    
    //private static String fazaPrev = "lexan";
    private static String fazaPrev = "synan";
    
    //private static String pravilnost = "pravilni";
    private static String pravilnost = "pravilni";
    
    private static String testName = "p3";
    
    //Faze: 
    
    public static void main(String[] args) throws Exception {
        String sourceFileName = "test/"+fazaPrev+"/"+pravilnost+"/"+testName+".pins";
        //sourceFileName = "test/sliva/tipi/test.pins";
        //sourceFileName = "test/v2/koda/test1.pins";
        sourceFileName = "test/interpreter/simple.pins";
        //sourceFileName = "test/intellij/seven/test.pins";
        boolean interpret = true;
        boolean interpretDbg = false;

            Report.openDumpFile(sourceFileName);
            
            //Lexan - 0
            LexAn lexAn = new LexAn(sourceFileName, false);
            while (lexAn.lexAn().token != Token.EOF){}
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
            SemAn semAn = new SemAn(true);
            source.accept(new NameChecker());
            //semAn.dump(source);
            //Report.closeDumpFile();
            System.out.printf("TEST: Seman imena ok\n");
            
            //Seman tipi - 4
            source.accept(new TypeChecker());
            //semAn.dump(source);
            
            System.out.printf("TEST: Seman tipi ok\n");
            
            // Klicni zapisi - 5
            Frames frames = new Frames(true);
            source.accept(new FrmEvaluator());
            frames.dump(source);
            
            System.out.printf("TEST: Frames constructed\n");
            
            
            ImCode imcode = new ImCode(true);
            ImcCodeGen imcodegen = new ImcCodeGen();
            source.accept(imcodegen);
            imcode.dump(imcodegen.chunks);
            
            System.out.printf("TEST: Intermediatecode chunkified\n");

            //**INTERPRETER TESTING**//
            if(interpret) {
                //Najde definicijo funkcije main ali pa konča izvajanje, če definicije ne najde

                AbsDef mainDef = SymbTable.fnd("main");

                //Če definicija ni funkcija (npr. globalana spremenljiva)
                if (!(mainDef instanceof AbsFunDef)) Report.error("Main mora biti funkcija.");
                AbsFunDef mainFunDef = (AbsFunDef) mainDef;

                //Najde okvir funkcije main
                FrmFrame mainFrame = FrmDesc.getFrame((AbsFunDef) mainDef);

                Interpreter.debug = interpretDbg;

                //Argument main funkcije nastavimo na 0 (1000 - začetna vrednost FP, FP+4 naj bi bil prvi argument main funkcije)
                Interpreter.stM(1000 + 4, new ImcCONST(0));

                new Interpreter(mainFrame, ImcCodeGen.linearCode.get(mainDef));
            }

            Report.closeDumpFile();  
    }    
}
