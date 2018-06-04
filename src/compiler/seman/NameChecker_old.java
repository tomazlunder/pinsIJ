package compiler.seman;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import java.util.LinkedList;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @author sliva
 */
public class NameChecker_old implements Visitor {

    private final String nameCheckStr = "NameChecker error: ";
    private boolean nameChecking;
    private boolean subChecking;
    private LinkedList<AbsTree> subScopes;
    private LinkedList<AbsTree> nameUsage;
    
    /**TREE ENTRY POINT.
     * 
     * 
     */
    //ok2
    @Override
    public void visit(AbsDefs acceptor) {
        subScopes = new LinkedList<>();
        nameUsage = new LinkedList<>();
        nameChecking = false;
        subChecking = false;
        
       	for (int def = 0; def < acceptor.numDefs(); def++) {
            acceptor.def(def).accept(this);
	}
        
        nameChecking = true;
        while(!nameUsage.isEmpty()){
            AbsTree name = nameUsage.pollFirst();
            name.accept(this);
        }
        nameChecking = false;
       
        subChecking = true;
        while(!subScopes.isEmpty()){
            AbsTree subScope = subScopes.pollFirst();
            subScope.accept(this);
        }
        subChecking = false;
        
    }
    
    /**DEFINITIONS.
     * 
     */
    @Override
    public void visit(AbsPar acceptor) {
        try{
            SymbTable.ins(acceptor.name, acceptor);
        } catch (SemIllegalInsertException e){
            Report.error("SemIllegalInstertEx");
        }
    }

    @Override
    public void visit(AbsTypeDef acceptor) {
        try{
            SymbTable.ins(acceptor.name, acceptor);
        } catch (SemIllegalInsertException e) {
            Report.error("SemIllegalInstertEx");
        };
    }

    @Override
    public void visit(AbsVarDef acceptor){
        try{
            SymbTable.ins(acceptor.name, acceptor);
        } catch (SemIllegalInsertException e) {
            Report.error("SemIllegalInstertEx");
        };
    }

    /**SCOPE SWITCHES (FunDef, Where).
     * 
     */
    @Override
    public void visit(AbsWhere acceptor) {
        if(!subChecking){
            acceptor.expr.accept(this);
            nameUsage.add(acceptor);
            return;
        }
        
        SymbTable.newScope();
        acceptor.defs.accept(this);
        nameChecking = true;
        acceptor.expr.accept(this);
        nameChecking = false;
        SymbTable.oldScope();
    }
    
    @Override
    public void visit(AbsFunDef acceptor) {   
        if(!subChecking){
            subScopes.add(acceptor);
            try{
                SymbTable.ins(acceptor.name, acceptor);
            } catch (SemIllegalInsertException e){}
            return;
        }
        
        SymbTable.newScope();
        for(int i = 0; i < acceptor.numPars(); i++){
            acceptor.par(i).accept(this);
        }
        acceptor.type.accept(this);
        nameChecking = true;
        acceptor.expr.accept(this);
        nameChecking = false;
        SymbTable.oldScope();
    }
    
    /**CHECKING NAMES (nameChecking) from nameUsage.
     * 
     */
    @Override
    public void visit(AbsFunCall acceptor) {
        if(!nameChecking){
            nameUsage.add(acceptor);
            return;
        }
        AbsDef definition = SymbTable.fnd(acceptor.name);
        if(definition == null){
            Report.error(acceptor.position, nameCheckStr + "function not defined - " + acceptor.name + ".");
        } else {
            SymbDesc.setNameDef(acceptor, definition);
        }
    }
    
    @Override
    public void visit(AbsTypeName acceptor) {
        if(!nameChecking){
            nameUsage.add(acceptor);
            return;
        }
        AbsDef definition = SymbTable.fnd(acceptor.name);
        if(definition== null){
            Report.error(acceptor.position, nameCheckStr + "type not defined - " + acceptor.name + ".");
        } else {
            SymbDesc.setNameDef(acceptor, definition);
        }
    }
    
    @Override
    public void visit(AbsVarName acceptor) {
        if(!nameChecking){
            nameUsage.add(acceptor);
            return;
        }
        AbsDef definition = SymbTable.fnd(acceptor.name);
        if(definition == null){
            Report.error(acceptor.position, nameCheckStr + "variable not defined - " + acceptor.name + ".");
        } else {
            SymbDesc.setNameDef(acceptor, definition);
        }
    }

    /**SKIPPED METHODS.
     * 
     */
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
    public void visit(AbsExprs acceptor) {
        for (int expr = 0; expr < acceptor.numExprs(); expr++) {
            acceptor.expr(expr).accept(this);
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
    public void visit(AbsIfThen acceptor) {
        acceptor.cond.accept(this);
        acceptor.thenBody.accept(this);
    }

    @Override
    public void visit(AbsIfThenElse acceptor) {
        acceptor.cond.accept(this);
        acceptor.thenBody.accept(this);
        acceptor.elseBody.accept(this);
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
