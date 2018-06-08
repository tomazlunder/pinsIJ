package compiler.frames;

import compiler.abstr.*;
import compiler.abstr.tree.AbsArrType;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsFor;
import compiler.abstr.tree.AbsFunCall;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsIfThen;
import compiler.abstr.tree.AbsIfThenElse;
import compiler.abstr.tree.AbsPar;
import compiler.abstr.tree.AbsTypeDef;
import compiler.abstr.tree.AbsTypeName;
import compiler.abstr.tree.AbsUnExpr;
import compiler.abstr.tree.AbsVarDef;
import compiler.abstr.tree.AbsVarName;
import compiler.abstr.tree.AbsWhere;
import compiler.abstr.tree.AbsWhile;
import compiler.interpreter.Interpreter;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemType;
import java.util.Stack;

public class FrmEvaluator implements Visitor {

    //Stack Frejmov
    private Stack<FrmFrame> stack = new Stack<>();
    
    //Števec za offset parametrov
    int offset;
        
    @Override
    public void visit(AbsFunDef acceptor) {        

        
        //Ustvarimo okvir
        FrmFrame frame = new FrmFrame(acceptor, stack.size()+1);

        //Če je funkcija definirana znotraj funkcije mora imeti anonimno labelo
        //frame.label = FrmLabel.newLabel(acceptor.name);


        frame.numPars = acceptor.numPars();
        
        //Okvir porinemo na sklad
        stack.add(frame);
        
        int sizePars = 0;
        offset = 4; //Začetni offset je 4B (static link);
        for(int i = 0; i < acceptor.numPars(); i++){
            acceptor.par(i).accept(this);
            SemType semType = SymbDesc.getType(acceptor.par(i));
            sizePars += semType.size();
        }
       
        acceptor.type.accept(this);
        acceptor.expr.accept(this);
       
        //Okvir popamo iz sklada
        stack.pop();
        
        //SizeArgs - trenutno hrani vrednost velikosti argumentov največjega klica
        //Če ima kakšen klic, prostoru za argumente dodamo še 4B za statičen link.
        if(frame.sizeArgs > 0){
            frame.sizeArgs+=4;
        }

        //Okvir dodamo v slovar okvirjev
        FrmDesc.setFrame(acceptor, frame);
    }
    
    @Override
    public void visit(AbsPar acceptor) {
        FrmParAccess frmParAccess = new FrmParAccess(acceptor, stack.peek());
        //stack.peek().sizePars += SymbDesc.getType(acceptor).size();
        FrmDesc.setAccess(acceptor, frmParAccess);
    }
    
    @Override
    public void visit(AbsWhere acceptor) {
        acceptor.defs.accept(this);
        acceptor.expr.accept(this);
    }
    
    @Override
    public void visit(AbsVarDef acceptor) {
        FrmAccess frmAccess;
        
        //Globalna spremenljivka
        if(stack.size() == 0){
            frmAccess = new FrmVarAccess(acceptor);
        }
        //Lokalna spremenljivka
        else{
            frmAccess = new FrmLocAccess(acceptor, stack.peek());
            stack.peek().locVars.add((FrmLocAccess) frmAccess);
        }
        
        //Dodamo v slovar dostopov
        FrmDesc.setAccess(acceptor, frmAccess);
    }
    
    @Override
    public void visit(AbsArrType acceptor) {
        acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsAtomConst acceptor) {
    }

    @Override
    public void visit(AbsAtomType acceptor) {
    }

    @Override
    public void visit(AbsBinExpr acceptor) {
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
    }

    @Override
    public void visit(AbsDefs acceptor) {
        for(int i = 0; i < acceptor.numDefs(); i++){
            acceptor.def(i).accept(this);
        }
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
    public void visit(AbsFunCall acceptor) {
        int totalCallArgSize = 0;
        if(Interpreter.isPredefined(acceptor.name)){
            totalCallArgSize = 1;
        }
        else {
            totalCallArgSize = 0;
            for(int i = 0; i < acceptor.numArgs(); i++){
                totalCallArgSize+=SymbDesc.getType(acceptor).size();
                acceptor.arg(i).accept(this);
            }
        }
        if(stack.peek().sizeArgs < totalCallArgSize){
            stack.peek().sizeArgs = totalCallArgSize;
        }
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
    public void visit(AbsTypeDef acceptor) {
        acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsTypeName acceptor) {
    }

    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
    }

    @Override
    public void visit(AbsVarName acceptor) {
    }

    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);
    }
	
}
