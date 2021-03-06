package compiler.imcode;

import compiler.Report;
import java.util.*;

import compiler.abstr.*;
import compiler.abstr.tree.AbsArrType;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExpr;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsFor;
import compiler.abstr.tree.AbsFunCall;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsIfThen;
import compiler.abstr.tree.AbsIfThenElse;
import compiler.abstr.tree.AbsPar;
import compiler.abstr.tree.AbsTree;
import compiler.abstr.tree.AbsTypeDef;
import compiler.abstr.tree.AbsTypeName;
import compiler.abstr.tree.AbsUnExpr;
import compiler.abstr.tree.AbsVarDef;
import compiler.abstr.tree.AbsVarName;
import compiler.abstr.tree.AbsWhere;
import compiler.abstr.tree.AbsWhile;
import compiler.frames.FrmAccess;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.frames.FrmLocAccess;
import compiler.frames.FrmParAccess;
import compiler.frames.FrmTemp;
import compiler.frames.FrmVarAccess;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemArrType;

public class ImcCodeGen_old implements Visitor {

    public LinkedList<ImcChunk> chunks;
    public HashMap<AbsTree,ImcCode> code; //Dodal za shranjevanje vmesnih, prenašanje v starša   
    
    //Da vemo na katerem stat nivoju smo in v kateri funkciji (v primeru, da uporabljamo nekaj zunaj funkcije)
    int scope;
    Stack<FrmFrame> frames;
	
    public ImcCodeGen_old() {
	chunks = new LinkedList<ImcChunk>();
        code = new HashMap<AbsTree,ImcCode>();
        
        scope = -1;
        frames = new Stack<>();
    }    
        
    @Override
    public void visit(AbsFunDef acceptor) {
        //Prelet vsega
        scope++;
        frames.add(FrmDesc.getFrame(acceptor));
        for(int i = 0; i < acceptor.numPars(); i++){
            acceptor.par(i).accept(this);
        }
        acceptor.expr.accept(this);
        frames.pop();
        scope--;
               
        FrmFrame functionFrame = FrmDesc.getFrame(acceptor);
        ImcMOVE move;
        move = new ImcMOVE((ImcExpr) code.get(acceptor.expr), new ImcTEMP(functionFrame.RV));
        ImcCodeChunk functionCodeChunk = new ImcCodeChunk(functionFrame, move);
        chunks.add(functionCodeChunk);
    }
    
    @Override
    public void visit(AbsVarDef acceptor) {
        //Preleta vsega ni, ker je samo AbsType
        
        FrmAccess access = FrmDesc.getAccess(acceptor);
        //Globalna - lokalnih in parametrov nam tu ni potrebno obdelati
        if(access instanceof FrmVarAccess){
            ImcDataChunk idc = new ImcDataChunk(((FrmVarAccess) access).label, SymbDesc.getType(acceptor).size());
            chunks.add(idc);
        } 

    }
    
    @Override
    public void visit(AbsVarName acceptor) {
        ImcCode result = null;
        //Preleta vsega ni, ker je samo AbsType
        
        FrmAccess access = FrmDesc.getAccess(SymbDesc.getNameDef(acceptor));
        //Globalna
        if(access instanceof FrmVarAccess){
            result = new ImcMEM(new ImcNAME(((FrmVarAccess)access).label));
        } 
        //Lokalna
        else if (access instanceof FrmLocAccess){
            FrmLocAccess locAccess = (FrmLocAccess) access;
            // trenutni nivo - nivo definicije = razlika v nivojih (scope)
            int razlika = scope - SymbDesc.getScope(locAccess.var);
            
            ImcExpr leftSide = new ImcTEMP(locAccess.frame.FP);
            for(int i = 0; i < razlika; i++){
                leftSide = new ImcMEM(leftSide);
            }
            
            ImcBINOP binop = new ImcBINOP(AbsBinExpr.ADD,(ImcExpr) leftSide, new ImcCONST(locAccess.offset));
            result = new ImcMEM(binop);
        }
        //Parameter
        else if (access instanceof FrmParAccess){
            FrmParAccess parAccess = (FrmParAccess) access;
            // trenutni nivo - nivo definicije = razlika v nivojih (scope)
            int trenutniNivo = parAccess.frame.level;
            int nivoDefinicije = SymbDesc.getScope(SymbDesc.getNameDef(acceptor));
            int razlika = trenutniNivo - nivoDefinicije;
            
            ImcExpr leftSide = new ImcTEMP(parAccess.frame.FP);
            for(int i = 0; i < razlika; i++){
                leftSide = new ImcMEM(leftSide);
            }
            
            ImcBINOP binop = new ImcBINOP(AbsBinExpr.ADD,(ImcExpr) leftSide, new ImcCONST(parAccess.offset));
            result = new ImcMEM(binop);        
        }
        
        code.put(acceptor, result);
    }
    
    @Override
    public void visit(AbsBinExpr acceptor) {
        ImcCode result = null;
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
        
        //BINOP
        if(acceptor.oper <= 11){
            result = new ImcBINOP(ImcBINOP.AbsToImcCode.get(acceptor.oper), (ImcExpr) code.get(acceptor.expr1), (ImcExpr) code.get(acceptor.expr2));
        }
        //MOD
        else if(acceptor.oper == 12){
            //a%b == a - ( b * (a/b))
            result = new ImcBINOP(ImcBINOP.SUB, (ImcExpr) code.get(acceptor.expr1), // a -
                            new ImcBINOP(ImcBINOP.MUL, (ImcExpr) code.get(acceptor.expr2), // (b *  
                                    new ImcBINOP(ImcBINOP.DIV, (ImcExpr) code.get(acceptor.expr1), (ImcExpr) code.get(acceptor.expr2)))); // (a/b))
        }
        //ARR - dostop do elementa array-ja
        else if (acceptor.oper == 14){
            AbsVarName arrayName = (AbsVarName) acceptor.expr1;
            int sizeOfElement = ((SemArrType) SymbDesc.getType(SymbDesc.getNameDef(arrayName))).type.size();
            
            result = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, (ImcExpr) code.get(acceptor.expr1), new ImcBINOP(ImcBINOP.MUL, (ImcExpr) code.get(acceptor.expr2), new ImcCONST(sizeOfElement))));
        }
        //Assign
        else if(acceptor.oper == 15){
            //result = new ImcMOVE((ImcExpr) code.get(acceptor.expr1), (ImcExpr) code.get(acceptor.expr2));
            result = new ImcESEQ(new ImcMOVE((ImcExpr) code.get(acceptor.expr1), (ImcExpr) code.get(acceptor.expr2)), (ImcExpr) code.get(acceptor.expr2));
        }
        
        code.put(acceptor, result);
    }
    
    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
        
        if(acceptor.oper == AbsUnExpr.ADD){
            code.put(acceptor, code.get(acceptor.expr));
        }
        else if(acceptor.oper == AbsUnExpr.SUB){
            code.put(acceptor, new ImcBINOP(ImcBINOP.SUB, new ImcCONST(0), (ImcExpr) code.get(acceptor.expr)));
        }
        else if(acceptor.oper == AbsUnExpr.NOT){
            //If 1 = true, 0 = false
            //Then 1-x negates x. 
            code.put(acceptor, new ImcBINOP(ImcBINOP.SUB, new ImcCONST(1), (ImcExpr) code.get(acceptor.expr)));
        }
    }
    
    @Override
    public void visit(AbsFunCall acceptor) {
        //Gremo cez argumente, jih dodamo v list
        LinkedList<ImcExpr> args = new LinkedList<>();
        for(int i = 0; i < acceptor.numArgs(); i++){
            acceptor.arg(i).accept(this);
            args.add((ImcExpr) code.get(acceptor.arg(i)));
        }
        
        FrmFrame klicanaFunkcija = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));
        
        //Nivo klica - nivo klicane funkcije.
        //int nivoDefinicije = SymbDesc.getScope(SymbDesc.getNameDef(acceptor));
        int razlika = scope - klicanaFunkcija.level;

        
        ImcExpr SL = new ImcCONST(1100110011);
        //Klici na nivo n-1, n-2
        if(klicanaFunkcija.level > 0){
            SL = new ImcMEM( new ImcTEMP(frames.peek().FP));
            for(int i = 0; i < razlika; i++){
                SL = new ImcMEM(SL);
            }
        }
        //Klic na nivo n+1
        else if(klicanaFunkcija.level == -1){
            SL = new ImcTEMP(FrmDesc.getFrame(acceptor).FP);
        }
        //Na zacetek argumentov dodamo StaticLink
        args.addFirst(SL);
        
        ImcCALL call = new ImcCALL(klicanaFunkcija.label);
        call.args = args;

        code.put(acceptor,call);
    }

    @Override
    public void visit(AbsAtomConst acceptor) {
        if(acceptor.type == AbsAtomConst.INT){
            code.put(acceptor, new ImcCONST(Integer.parseInt(acceptor.value)));
        }
        else if(acceptor.type == AbsAtomConst.LOG){
            Integer value = null;
            if(acceptor.value.toLowerCase().equals("true")){
                value = 1;
            } else if (acceptor.value.toLowerCase().equals("false")){
                value = 0;
            }
            code.put(acceptor, new ImcCONST(value));
        }
        else if(acceptor.type == AbsAtomConst.STR){
            code.put(acceptor, new ImcLABEL(FrmLabel.newLabel()));
        }
    }

    @Override
    public void visit(AbsDefs acceptor) {
        //Definicij ne potrebujemo več... naredimo samo prelet
        for(int i = 0; i < acceptor.numDefs(); i++){
            acceptor.def(i).accept(this);
        }
    }

    @Override
    public void visit(AbsExprs acceptor) {
        for(int i = 0; i < acceptor.numExprs(); i++){
            acceptor.expr(i).accept(this);
        }
        ImcSEQ seq = new ImcSEQ();
        ImcExpr expr = null;
        for(int i = 0; i < acceptor.numExprs(); i++){
            if(i < acceptor.numExprs()-1){
                if(code.get(acceptor.expr(i)) instanceof ImcStmt){
                    seq.stmts.add((ImcStmt) code.get(acceptor.expr(i)));
                } 
                else {
                seq.stmts.add(new ImcEXP((ImcExpr) code.get(acceptor.expr(i))));
                }
            }else{
                AbsExpr lastExpression = acceptor.expr(i);
                ImcCode lastExpressionCode = code.get(lastExpression); 
                //TODO: Tukaj je problem da je to v nekaterih primerih Stmt... moral bi pa biti Expr. 
                /*
                if(lastExpressionCode instanceof ImcExpr){
                    expr = (ImcExpr) lastExpressionCode;
                }
                else{
                    seq.stmts.add((ImcStmt) lastExpressionCode);
                    code.put(acceptor, seq);
                    return;
                }
                */
                expr = (ImcExpr) lastExpressionCode;
                     
            }
        }        
        code.put(acceptor, new ImcESEQ(seq, expr));
    }

    @Override
    public void visit(AbsIfThen accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
        
        ImcLABEL trueLabel = new ImcLABEL(FrmLabel.newLabel());
        ImcLABEL endLabel = new ImcLABEL(FrmLabel.newLabel());
        
        ImcCJUMP cjump = new ImcCJUMP((ImcExpr) code.get(accpetor.cond),trueLabel.label, endLabel.label);
        
        ImcSEQ seq = new ImcSEQ();
        seq.stmts.add(cjump.linear());
        seq.stmts.add(trueLabel);
        seq.stmts.add(new ImcEXP((ImcExpr) code.get(accpetor.thenBody)));
        seq.stmts.add(endLabel);
        
        code.put(accpetor, seq);
    }

    @Override
    public void visit(AbsIfThenElse accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
        accpetor.elseBody.accept(this);
        
        ImcLABEL falseLabel = new ImcLABEL(FrmLabel.newLabel());
        ImcLABEL trueLabel = new ImcLABEL(FrmLabel.newLabel());
        ImcLABEL endLabel = new ImcLABEL(FrmLabel.newLabel());
        
        ImcCJUMP cjump = new ImcCJUMP((ImcExpr) code.get(accpetor.cond), trueLabel.label, falseLabel.label);
        //TODO: Tha fuck .. je tle spodi ImcEXP prav? idk...
        
        ImcSEQ seq = new ImcSEQ();
        seq.stmts.add(cjump.linear());
        seq.stmts.add(falseLabel);
        seq.stmts.add(new ImcEXP((ImcExpr) code.get(accpetor.elseBody)).linear());
        seq.stmts.add(new ImcJUMP(endLabel.label));
        seq.stmts.add(trueLabel);
        seq.stmts.add(new ImcEXP((ImcExpr) code.get(accpetor.thenBody)).linear());
        seq.stmts.add(endLabel);
        
        code.put(accpetor, seq);
    }
    
    @Override
    public void visit(AbsWhile acceptor) {
        //Prelet vsega
        acceptor.cond.accept(this);
        acceptor.body.accept(this);
        
        ImcLABEL startLabel = new ImcLABEL(FrmLabel.newLabel());
        ImcLABEL trueLabel = new ImcLABEL(FrmLabel.newLabel());
        ImcLABEL endLabel = new ImcLABEL(FrmLabel.newLabel());
        
        ImcCJUMP cjump = new ImcCJUMP((ImcExpr) code.get(acceptor.cond),trueLabel.label, endLabel.label);
        
        ImcSEQ seq = new ImcSEQ();
        seq.stmts.add(cjump.linear());
        seq.stmts.add(trueLabel);
        seq.stmts.add(new ImcEXP((ImcExpr) code.get(acceptor.body)));
        seq.stmts.add(new ImcJUMP(startLabel.label));
        seq.stmts.add(endLabel);
        
        code.put(acceptor, seq);
    }
    
    @Override
    public void visit(AbsFor acceptor) {
        //Prelet vsega
        acceptor.hi.accept(this);
        acceptor.lo.accept(this);
        acceptor.step.accept(this);
        acceptor.body.accept(this);
        
        ImcTEMP counter = new ImcTEMP(new FrmTemp());
        ImcMOVE init = new ImcMOVE(new ImcMEM(counter),(ImcExpr) code.get(acceptor.lo));
        
        ImcLABEL startLabel = new ImcLABEL(FrmLabel.newLabel());
        ImcBINOP condition = new ImcBINOP(ImcBINOP.LEQ, counter, (ImcExpr) code.get(acceptor.hi));
        ImcLABEL trueLabel = new ImcLABEL(FrmLabel.newLabel());
        //Stavek
        ImcMOVE increment = new ImcMOVE(counter,new ImcBINOP(ImcBINOP.ADD, counter, (ImcExpr) code.get(acceptor.step)));
        ImcJUMP jump = new ImcJUMP(startLabel.label);
        ImcLABEL endLabel = new ImcLABEL(FrmLabel.newLabel());
        
        ImcCJUMP cjump = new ImcCJUMP(condition, trueLabel.label,endLabel.label);
        
        ImcSEQ seq = new ImcSEQ();
        seq.stmts.add(init);
        seq.stmts.add(startLabel);
        seq.stmts.add(cjump);
        seq.stmts.add(trueLabel);
        seq.stmts.add(new ImcEXP((ImcExpr) code.get(acceptor.body)));
        seq.stmts.add(jump);
        seq.stmts.add(endLabel);
        
        code.put(acceptor, seq);
    }

    @Override
    public void visit(AbsWhere acceptor) {
        acceptor.defs.accept(this);
        acceptor.expr.accept(this);   
        
        code.put(acceptor, code.get(acceptor.expr));
    }
    
    @Override
    public void visit(AbsPar acceptor) {
        //Mislim da nic
    }

    @Override
    public void visit(AbsTypeDef acceptor) {
        //Mislim da nic
    }

    @Override
    public void visit(AbsTypeName acceptor) {
        //Mislim da nic
    }
	
    @Override
    public void visit(AbsAtomType acceptor) {
        //Mislim da nic
    }
    
    @Override
    public void visit(AbsArrType acceptor) {
        //Mislim da nic
    }
}
