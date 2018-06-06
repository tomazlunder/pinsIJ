package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.interpreter.Interpreter;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 * @author sliva
 */
public class TypeChecker implements Visitor {

    /*
     *Za AbsDefs hrani stevilo trenutnega preleta.
     *  1 - 1. prelet tipov (podpis)
     *  2 - 2. prelet tipov (globina... ugotavlanje konkretnega tipa pri verižnem definiranju tipov)
     *  3 - prelet spremenljivk 
     *  4 - 1. prelet funkcij (podpis)
     *  5 - 2. prelet funkcij (telo... ali se rezultat telesa sklada z podpisom?)
     */
    int globPrelet = -1;
    
    //Za primerjanje ATOM Tipov v unarnih in binarnih izrazih.
    final public static SemAtomType LOGICAL = new SemAtomType(0);
    final public static SemAtomType INTEGER = new SemAtomType(1);
    final public static SemAtomType STRING = new SemAtomType(2);
    final public static SemAtomType VOID = new SemAtomType(3);

    AbsDefs original;
    /*
     * ENTRY POINT
     */
    /*
    @Override
    public void visit(AbsDefs acceptor) {
        //Prelet        
        int preletPrej = -1;
        if(globPrelet != -1){
            preletPrej = globPrelet;
        }
        
        
        int prelet = 1;
        int delajDo = 5;
        if(preletPrej != -1)
            delajDo = Math.min(5, preletPrej);
        while(prelet <= delajDo){
            globPrelet = prelet;
            //Naredimo prelet definicij
            for(int i = 0; i < acceptor.numDefs(); i++){
                acceptor.def(i).accept(this);
            }
            
            prelet++;
        }
        
        if(preletPrej != -1){
            globPrelet = preletPrej;
        }
    }
    */
    
    
    @Override
    public void visit(AbsDefs acceptor) {
        if(globPrelet == -1){
            original = acceptor;
            globPrelet = 1;
        }
        
        if(acceptor.equals(original)){
            while(globPrelet <= 5){
                //Naredimo prelet definicij
                for(int i = 0; i < acceptor.numDefs(); i++){
                    acceptor.def(i).accept(this);
                }

                globPrelet++;
            }
        
        }

        else {
            //Naredimo prelet definicij
            for(int i = 0; i < acceptor.numDefs(); i++){
                acceptor.def(i).accept(this);
            }            
        }
    }
    
    
    /*
     * NO LOGIC FUNCTIONS
     */
    @Override
    public void visit(AbsWhere acceptor) {
        acceptor.defs.accept(this);
        
        acceptor.expr.accept(this);
        
        if(globPrelet == 4){
            SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.expr));
        }
    }
    
    @Override
    public void visit(AbsExprs acceptor) {
        for(int i = 0; i < acceptor.numExprs(); i++){
            acceptor.expr(i).accept(this);
        }
        
        if(globPrelet == 4){
            SemType typeOfLast = SymbDesc.getType(acceptor.expr(acceptor.numExprs()-1));
            SymbDesc.setType(acceptor, typeOfLast);
        }
    }
       
    /*
     *PRELETI TIPOV. 
     */
    @Override
    public void visit(AbsTypeDef acceptor) {
        acceptor.type.accept(this);
        

        if(globPrelet == 1){
            SymbDesc.setType(acceptor, new SemTypeName(acceptor.name));
        }
        
        //V globino najdemo primitivno vrednost vsakega tipa, ki še ne kaže na Atom
        if(globPrelet == 2){
            SymbDesc.setType(acceptor, getRealType(acceptor));
        }
    }
    
    //POMOŽNA FUNKCIJA - ISKANJE KONKRETNEGA PRIMITIVNEGA TIPA DEFINICIJE TIPA
    public SemType getRealType(AbsTree node){
        //Če je atom
        SemType semType = SymbDesc.getType(node);
        
        if(semType instanceof SemAtomType){
            return (SemAtomType) semType;
        }
        
        else if (semType instanceof SemArrType){
            return (SemArrType) semType;
        }
        
        if(node instanceof AbsTypeName){
            AbsTypeDef nameDef = (AbsTypeDef) SymbDesc.getNameDef(node);
            AbsTree type = nameDef.type;
            
            return getRealType(type);
        }
        
        //Če ni, gre v globino
        if(semType instanceof SemTypeName){
            AbsTree type = ((AbsTypeDef) node).type;
            if(type instanceof AbsTypeName){
                type = (AbsTypeDef) SymbDesc.getNameDef(type);
            }
            return getRealType(type);
        }
        
        //To se naj ne bi zgodilo
        Report.error("Iskanje tipa v globino: ne najdem tipa.");
        return null;
    }
    
    public void visit(AbsAtomType acceptor) {
        //Če še ni definiran tip
        if(SymbDesc.getType(acceptor) == null)
            SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
    }
    
    @Override
    public void visit(AbsTypeName acceptor) {        
        if(globPrelet == 1){
            SymbDesc.setType(acceptor, new SemTypeName(acceptor.name));
        }
        
        if(globPrelet == 2){
            SemTypeName semTypeName = (SemTypeName) SymbDesc.getType(acceptor);
            //semTypeName.setType(SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
            semTypeName.setType(getRealType(acceptor));
            SymbDesc.setType(acceptor, semTypeName.getType());
            int debug = 3;
        }
    }
    
    //ARRAY
    @Override
    public void visit(AbsArrType acceptor) {
        acceptor.type.accept(this);
        if(globPrelet == 1)
            SymbDesc.setType(acceptor, new SemArrType(acceptor.length, null));
        
        if(globPrelet == 2)
            SymbDesc.setType(acceptor, new SemArrType(acceptor.length, SymbDesc.getType(acceptor.type)));
    }
    /**TRETJI PRELET. ***************************************************************************************************************************************************
     * 
     */
    
    @Override
    public void visit(AbsVarDef acceptor) {
        acceptor.type.accept(this);
        //Ob tretjem preletu prevzame tip vozlišča, ki pove tip definicije spremenljivke
        if(globPrelet == 3){
            SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.type));
        }
    }
    
    //KONSTANTA
    @Override
    public void visit(AbsAtomConst acceptor) {
        //Če vozlišče še ni definirano
        if(SymbDesc.getType(acceptor) == null)
            SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
    }
    
    @Override
    public void visit(AbsPar acceptor) {
        //Tip obdelamo neglede na prelet
        acceptor.type.accept(this);
        
        //Ob drugem preletu funkcij določimo tip parametra
        if(globPrelet == 3){
            SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.type));
        }
    }
    
    /**ČETRTI IN PETI PRELET. **************************************************************************************************************************************************
     * 
     */
    
    /*
     * FUNKCIJE
     */
    @Override
    public void visit(AbsFunDef acceptor) {
        //Neglede na prelet obiščemo vsa vozlišča
        acceptor.type.accept(this);
        for(int i = 0; i < acceptor.numPars(); i++){
            acceptor.par(i).accept(this);
        }
        acceptor.expr.accept(this);
        
        //Obdelamo podpis funkcije
        if(globPrelet == 3){  //XXX
           Vector<SemType> parTypes = new Vector();
           for(int i = 0; i < acceptor.numPars(); i++){
               acceptor.par(i).accept(this);
               parTypes.add(SymbDesc.getType(acceptor.par(i)));
           }
           SymbDesc.setType(acceptor, new SemFunType(parTypes, SymbDesc.getType(acceptor.type)));
           
        }
        
        if(globPrelet == 5){
            //Dodano pred generiranjem vmesne kode, 14.5. Funkcije lahko sedaj sprejemajo in vračajo samo INTEGER, LOGICAL and STRING.
            for(int i = 0; i < acceptor.numPars(); i++){
                AbsPar current = acceptor.par(i);
                SemType st = SymbDesc.getType(current);
                if(st.sameStructureAs(INTEGER) || st.sameStructureAs(LOGICAL) || st.sameStructureAs(STRING)){
                } else{
                    Report.error("AbsFunDef: argumenti funkcije so lahko le INTEGER/LOGICAL/STRING");
                }
            }
            SemType rt = SymbDesc.getType(acceptor.type);
            if(rt.sameStructureAs(INTEGER) || rt.sameStructureAs(LOGICAL) || rt.sameStructureAs(STRING)){}
            else{
                Report.error("AbsFunDef: rezultat funkcije je lahko le INTEGER/LOGICAL/STRING");
            }
            
            
            if(! SymbDesc.getType(acceptor.expr).sameStructureAs(SymbDesc.getType(acceptor.type))){
                Report.error("AbsFunDef: tip rezultata podpisa funkcije se razlikuje od dejanskega. Pričakovan: "+SymbDesc.getType(acceptor.type).toString()+"| Dejanski: "+SymbDesc.getType(acceptor.expr).toString());
            }
        }
    }
   
    @Override
    public void visit(AbsFunCall acceptor) {
        //Argumente obdelamo neglede na prelet
        for(int i = 0; i < acceptor.numArgs(); i++){
            acceptor.arg(i).accept(this);
        }
        
        //Ob drugem preletu funkcij pogledamo, če se argumenti ujemajo u parametri klicane funkcije
        if(globPrelet == 4 && !Interpreter.isPredefined(acceptor.name)){
            AbsTree definition = SymbDesc.getNameDef(acceptor);
            SemType semType = SymbDesc.getType(definition);
            SemFunType semFunType = (SemFunType) semType;
            for(int i = 0; i < acceptor.numArgs(); i++){
               if(! semFunType.getParType(i).sameStructureAs(SymbDesc.getType(acceptor.arg(i)))){
                   Report.error("AbsFunCall: tip argumenta se razlikuje s tipom parametra funkcije");
               }
            }
            
            SymbDesc.setType(acceptor, semFunType.resultType);
        }

        //Če je klicana funkcija predefinirana je tipa void
        else if(globPrelet == 4 && Interpreter.isPredefined(acceptor.name)){
            switch(acceptor.name){
                case "putInt": SymbDesc.setType(acceptor,VOID); break;
                case "getInt": SymbDesc.setType(acceptor,INTEGER); break;
                case "putString": SymbDesc.setType(acceptor, VOID); break;
                case "getString": SymbDesc.setType(acceptor, STRING); break;
                default: Report.error("Predefined function TypeChecker error!");
            }
        }
    }
    
    /*
     * UNARNI IZRAZI
     */
    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
        
        if(globPrelet == 3){
            //ADD
            if(acceptor.oper == 0 || acceptor.oper == 1){
                if(SymbDesc.getType(acceptor.expr) instanceof SemAtomType){
                    if( ((SemAtomType) SymbDesc.getType(acceptor.expr)).sameStructureAs(INTEGER)){
                        SymbDesc.setType(acceptor, new SemAtomType(1));
                    }
                }
            }
            //NOT
            if(acceptor.oper == 4){
                if(SymbDesc.getType(acceptor.expr) instanceof SemAtomType){
                    if( ((SemAtomType) SymbDesc.getType(acceptor.expr)).sameStructureAs(LOGICAL)){
                        SymbDesc.setType(acceptor, new SemAtomType(0));
                    }
                }
            }
        }
    }

    /*
     * BINARNI IZRAZI
     */
    @Override
    public void visit(AbsBinExpr acceptor) {
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
        
        //Tipa podizrazov
        if(globPrelet == 4){ //XXX 
            SemType typeExpr1 = SymbDesc.getType(acceptor.expr1);
            SemType typeExpr2 = SymbDesc.getType(acceptor.expr2);

            SemAtomType satTypeExpr1 = null; 
            SemAtomType satTypeExpr2 = null;


            //ČE NE GRE ZA ARR ALI ASSIGN SI NASTAVIMO POMOŽNE SPREMENLJIVKE TIPA SemAtomType
            if(acceptor.oper < 13){
                if(typeExpr1 instanceof SemAtomType)
                satTypeExpr1 = (SemAtomType) typeExpr1;
                else
                    Report.error("AbsBinExpr[razen ARR/ASSIGN] zahteva dva izraza tipa SemAtomType(LOG/INT). Expr1: " + typeExpr1.toString());

                if(typeExpr1 instanceof SemAtomType)
                    satTypeExpr2 = (SemAtomType) typeExpr2;
                else
                    Report.error("AbsBinExpr[razen ARR/ASSIGN] zahteva dva izraza tipa SemAtomType(LOG/INT). Expr2: " + typeExpr2.toString());
            }

            //[IOR, AND]
            if(acceptor.oper <= 1){
                // Oba expressiona morata biti logical, rezultat je tudi logical
                if(satTypeExpr1.type == 0 && satTypeExpr2.type == 0)
                    SymbDesc.setType(acceptor, new SemAtomType(0));
                else
                    Report.error("AbsBinExpr[IOR/AND] zahteva dva AbsExpr-ja tipa LOGICAL. Expr1: "+satTypeExpr1.toString()+ " |Expr2: " + satTypeExpr2.toString());
            }
            
            //[EQU, NEQ]
            else if(acceptor.oper <= 3){
                // Oba expressiona morata biti logical ali integer, rezulatat je logical
                if(satTypeExpr1.type <= 1 && typeExpr1.actualType().sameStructureAs(typeExpr2))
                    SymbDesc.setType(acceptor, new SemAtomType(0));
                else
                    Report.error("AbsBinExpr[EQU/NEQ] zahteva dva AbsExpr-ja tipa LOGICAL oz. INTEGER. Expr1: "+satTypeExpr1.toString()+ " |Expr2: " + satTypeExpr2.toString());
            }
            
            //[LEQ, GEQ, LTH, GTH]
            else if(acceptor.oper <= 7){
                //Oba expressina morata biti integer, rezultat je logical
                if(satTypeExpr1.type == 1 && satTypeExpr2.type == 1)
                    SymbDesc.setType(acceptor, new SemAtomType(0));
                else
                    Report.error("AbsBinExpr[LEQ, GEQ, LTH, GTH] zahteva dva AbsExpr-ja tipa INTEGER. Expr1: "+satTypeExpr1.toString()+ " |Expr2: " + satTypeExpr2.toString());
            }

            //[ADD, SUB, MUL, DIV, MOD]
            else if(acceptor.oper <= 12){
                //Oba expressiona morata biti integer, rezultat je integer
                if(satTypeExpr1.type == 1 && satTypeExpr2.type == 1)
                    SymbDesc.setType(acceptor, new SemAtomType(1));
                else
                    Report.error("AbsBinExpr[ADD, SUB, MUL, DIV, MOD] zahteva dva AbsExpr-ja tipa INTEGER. Expr1: "+satTypeExpr1.toString()+ " |Expr2: " + satTypeExpr2.toString());

            }
            
            //ARR
            else if(acceptor.oper == 14){
                if(! (SymbDesc.getType(acceptor.expr1) instanceof SemArrType))
                    Report.error("AbsBinExpr[ARR] expr1 more biti type SemArrType. Expr1: " + typeExpr1.toString());
                
                if(! SymbDesc.getType(acceptor.expr2).sameStructureAs(INTEGER))
                    Report.error("AbsBinExpr[ARR] expr2 mora biti tipa INTEGER. Expr2: " + typeExpr2.toString());
                 
                SymbDesc.setType(acceptor, ((SemArrType) typeExpr1).type);
            }
            
            //ASSIGN
            else if(acceptor.oper == 15){
               
                if(typeExpr2.sameStructureAs(INTEGER) ||typeExpr2.sameStructureAs(LOGICAL) || typeExpr2.sameStructureAs(STRING)){
                    SymbDesc.setType(acceptor, new SemAtomType(((SemAtomType) typeExpr2).type));
                    
                } else {
                    Report.error("AbsBinExpr[ASSIGN] expr2 mora biti tipa LOGICAL/INTEGER/STRING. Expr2: " + typeExpr2.toString());
                }
            }
        }
    }

    /*
     * VARIABLE NAME
     */
    
    @Override
    public void visit(AbsVarName acceptor) {
        //Ob drugem preletu funkcij prevzame tip vozlišča, kjer je spremenljivka definirana.
        if(globPrelet == 3 || globPrelet == 4){
            AbsTree definicija = SymbDesc.getNameDef(acceptor);
            SymbDesc.setType(acceptor, SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
        }
    }
    
    /*
     * VOIDED
     */
    @Override
    public void visit(AbsIfThen acceptor) {
        //Obisce oba
        acceptor.cond.accept(this);
        acceptor.thenBody.accept(this);
        
        //Condition mora biti tipa logical
        if(globPrelet == 5){
            if(SymbDesc.getType(acceptor.cond) instanceof SemAtomType)
            {
                //Če je body tipa INT/LOGICAL/STR je tudi ta izraz enakega tipa
                if(SymbDesc.getType(acceptor.thenBody) instanceof SemAtomType){
                    SemAtomType bodyType = (SemAtomType) SymbDesc.getType(acceptor.thenBody);
                    SymbDesc.setType(acceptor, new SemAtomType(bodyType.type));
                }
                //Drugače je izraz tipa VOID
                SymbDesc.setType(acceptor, new SemAtomType(3)); //VOID
            }
            else
            {
                Report.error("AbsIfThen condition mora biti tipa LOGICAL. Condition: " + SymbDesc.getType(acceptor.cond).toString());
            }
        }
    }
    
    @Override
    public void visit(AbsIfThenElse acceptor) {
        //Obisce vse tri
        acceptor.cond.accept(this);
        acceptor.thenBody.accept(this);
        acceptor.elseBody.accept(this);
        
        //Condition mora biti tipa logical
        if(globPrelet == 4){
            if(SymbDesc.getType(acceptor.cond) instanceof SemAtomType)
            {
                if(SymbDesc.getType(acceptor.thenBody) instanceof SemAtomType && SymbDesc.getType(acceptor.elseBody) instanceof SemAtomType){
                    SemAtomType _then = (SemAtomType) SymbDesc.getType(acceptor.thenBody);
                    SemAtomType _else = (SemAtomType) SymbDesc.getType(acceptor.elseBody);
                    if(_then.type == _else.type){
                        SymbDesc.setType(acceptor, new SemAtomType(_then.type));
                    }
                    else
                        //VOID
                        SymbDesc.setType(acceptor, new SemAtomType(3));
                
                }
                else
                    //VOID
                    SymbDesc.setType(acceptor, new SemAtomType(3));
            }
            else
                Report.error("AbsIfThenElse condition mora biti tipa LOGICAL. Condition: " + SymbDesc.getType(acceptor.cond).toString());
        }
    }
    
    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);
        
        //Condition mora biti tipa logical
        if(globPrelet == 5){
            if(SymbDesc.getType(acceptor.cond) instanceof SemAtomType)
                SymbDesc.setType(acceptor, new SemAtomType(3)); //VOID
            else
                Report.error("AbsIfThen condition mora biti tipa LOGICAL. Condition: " + SymbDesc.getType(acceptor.cond).toString());
        }
    }
    
    @Override
    public void visit(AbsFor acceptor) {
        acceptor.hi.accept(this);
        acceptor.lo.accept(this);
        acceptor.step.accept(this);
        acceptor.count.accept(this);
        acceptor.body.accept(this);
        
        if(globPrelet == 5){
            SemType hiType = SymbDesc.getType(acceptor.hi);
            if(!(hiType instanceof SemAtomType) || ((SemAtomType) hiType).type != 1){
                Report.error("AbsFor [hi] mora biti tipa INTEGER. Hi: " + hiType.toString());
            }

            SemType loType = SymbDesc.getType(acceptor.lo);
            if(!(loType instanceof SemAtomType) || ((SemAtomType) loType).type != 1){
                Report.error("AbsFor [lo] mora biti tipa INTEGER. Lo: " + loType.toString());
            }

            SemType stepType = SymbDesc.getType(acceptor.step);
            if(!(stepType instanceof SemAtomType) || ((SemAtomType) stepType).type != 1){
                Report.error("AbsFor [step] mora biti tipa INTEGER. Step: " + stepType.toString());
            }

            AbsDef counterDef = SymbDesc.getNameDef(acceptor.count);
            SemType counterType = SymbDesc.getType(counterDef);
            if(!(counterType instanceof SemAtomType) || ((SemAtomType) counterType).type != 1){
                Report.error("AbsFor [counter] mora biti tipa INTEGER. Counter: " + counterType.toString());
            }

            SymbDesc.setType(acceptor, new SemAtomType(3)); //VOID
        }
    }
}

