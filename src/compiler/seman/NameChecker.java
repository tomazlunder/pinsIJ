package compiler.seman;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.interpreter.Interpreter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @author sliva
 */
public class NameChecker implements Visitor {

    private static final String[] predefinedFunctions = new String[]{"putInt", "getInt", "putString", "getString"};

    
    int globPrelet = -1;
    
    @Override
    public void visit(AbsDefs acceptor) {
        int preletPrej = -1;
        if(globPrelet != - 1){
            preletPrej = globPrelet;
        }
        
        int prelet = 1;
        while(prelet <= 2){
            globPrelet = prelet;
            for(int i = 0; i < acceptor.numDefs(); i++){
                acceptor.def(i).accept(this);
            }
            prelet++;
        }
        
        if(preletPrej != -1){
            globPrelet = preletPrej;
        }
    }
    
    /**DEFINITIONS.
     * When we come to a definition we insert into the SymbTable
     */
    
    //Function parameter
    @Override
    public void visit(AbsPar acceptor) {
        try{
            SymbTable.ins(acceptor.name, acceptor);
        } catch (SemIllegalInsertException e){
            Report.error("SemIllegalInstertEx");
        }
        
        acceptor.type.accept(this);
    }

    //Type definition
    @Override
    public void visit(AbsTypeDef acceptor) {
        if(globPrelet == 1){
            try{
                SymbTable.ins(acceptor.name, acceptor);
            } catch (SemIllegalInsertException e) {
                Report.error("SemIllegalInstertEx");
            };
        }
        if(globPrelet == 2){
            acceptor.type.accept(this);
        }
    }

    //Variable definition
    @Override
    public void visit(AbsVarDef acceptor){
        if(globPrelet == 1){
            try{
                SymbTable.ins(acceptor.name, acceptor);
            } catch (SemIllegalInsertException e) {
                Report.error("SemIllegalInstertEx");
            };
        }
        if(globPrelet == 2){
            acceptor.type.accept(this);
        }
    }
    
    /**DEFINITION AND SCOPE SWITCH.
    *
    */
    
    //Function definition
    @Override
    public void visit(AbsFunDef acceptor) {
        //Function definition - vstavimo v SymbTable
        if(globPrelet == 1){
            try {
                SymbTable.ins(acceptor.name, acceptor);
            } catch (SemIllegalInsertException ex) {
                Report.error("SemIllegalInstertEx");
            }
        }
        
        if(globPrelet == 2){
        SymbTable.newScope(); //Nov scope
        
        //Obdelamo parametre
        for(int i = 0; i < acceptor.numPars(); i++){
            acceptor.par(i).accept(this);
        }
        
        //Obdelamo tip
        acceptor.type.accept(this);
        
        //Obdelamo expression
        acceptor.expr.accept(this);
        
        SymbTable.oldScope(); //Star scope
        }
    }
    
    //Definitions
    @Override
    public void visit(AbsWhere acceptor) {
        SymbTable.newScope(); //Nov scope
        
        //Obdelamo definicije
        acceptor.defs.accept(this);
        
        //Obdelamo expression
        acceptor.expr.accept(this);
        
        SymbTable.oldScope(); //Star scope
    }
    
    /**CHECKING NAMES (nameChecking) from nameUsage.
     * 
     */
    @Override
    public void visit(AbsFunCall acceptor) {
        if(!Interpreter.isPredefined(acceptor.name)){
            AbsDef definicija = SymbTable.fnd(acceptor.name); //Preverimo, če je funkcija definirana
            SymbDesc.setNameDef(acceptor, definicija);
        }
        
        for(int i = 0; i < acceptor.numArgs(); i++){
            acceptor.arg(i).accept(this);
        }
    }
    
    @Override
    public void visit(AbsTypeName acceptor) {
        AbsDef definicija = SymbTable.fnd(acceptor.name); //Preverimo, če je tip definiran
        SymbDesc.setNameDef(acceptor, definicija);

    }
    
    @Override
    public void visit(AbsVarName acceptor) {
        AbsDef definicija = SymbTable.fnd(acceptor.name); //Preverimo, če je spremenljivka definiran
        SymbDesc.setNameDef(acceptor, definicija);

    }

    /**OSTALO.
     * 
     */
    
    @Override
    public void visit(AbsArrType acceptor) {
        acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsAtomConst acceptor) {
        //nothing
    }

    @Override
    public void visit(AbsAtomType acceptor) {
        //nothing
    }

    @Override
    public void visit(AbsBinExpr acceptor) {
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
    }

    @Override
    public void visit(AbsExprs acceptor) {
        for(int i = 0; i < acceptor.numExprs(); i++){
            acceptor.expr(i).accept(this);
        }
    }

    @Override
    public void visit(AbsFor acceptor) {
        acceptor.count.accept(this);
        acceptor.lo.accept(this);
        acceptor.hi.accept(this);
        acceptor.step.accept(this);
        acceptor.body.accept(this);
    }

    @Override
    public void visit(AbsIfThen accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
    }

    @Override
    public void visit(AbsIfThenElse accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
        accpetor.elseBody.accept(this);
    }

    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
    }

    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);
    }


}