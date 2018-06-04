package compiler.imcode;

import compiler.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Binarna operacija.
 * 
 * @author sliva
 */
public class ImcBINOP extends ImcExpr {

	public static final int ADD = 0;
	public static final int SUB = 1;
	public static final int MUL = 2;
	public static final int DIV = 3;
	public static final int EQU = 4;
	public static final int NEQ = 5;
	public static final int LTH = 6;
	public static final int GTH = 7;
	public static final int LEQ = 8;
	public static final int GEQ = 9;
	public static final int AND = 10;
	public static final int OR  = 11;
        
        public static final Map<Integer, Integer> AbsToImcCode;
        
        static{
            Map<Integer, Integer> map = new HashMap<Integer,Integer>();
            map.put(0,11); map.put(1, 10); map.put(2,4); map.put(3,5);
            map.put(4,8); map.put(5,9); map.put(6,6); map.put(7,7);
            map.put(8,0); map.put(9,1); map.put(10,2); map.put(11,3);
        
            AbsToImcCode = Collections.unmodifiableMap(map);
        }

	/** Operator.  */
	public int op;

	/** Koda levega podizraza.  */
	public ImcExpr limc;

	/** Koda desnega podizraza.  */
	public ImcExpr rimc;

	/**
	 * Ustvari novo binarno operacijo.
	 * 
	 * @param op Operator.
	 * @param limc Levi podizraz.
	 * @param rimc Desni podizraz.
	 */
	public ImcBINOP(int op, ImcExpr limc, ImcExpr rimc) {
		this.op = op;
		this.limc = limc;
		this.rimc = rimc;
	}

	@Override
	public void dump(int indent) {
		String op = null;
		switch (this.op) {
		case ADD: op = "+" ; break;
		case SUB: op = "-" ; break;
		case MUL: op = "*" ; break;
		case DIV: op = "/" ; break;
		case EQU: op = "=="; break;
		case NEQ: op = "!="; break;
		case LTH: op = "<" ; break;
		case GTH: op = ">" ; break;
		case LEQ: op = "<="; break;
		case GEQ: op = ">="; break;
		case AND: op = "&" ; break;
		case OR : op = "|" ; break;
		}
		Report.dump(indent, "BINOP op=" + op);
		limc.dump(indent + 2);
		rimc.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ limc = this.limc.linear();
		ImcESEQ rimc = this.rimc.linear();
		ImcSEQ stmt = new ImcSEQ();
		stmt.stmts.addAll(((ImcSEQ)limc.stmt).stmts);
		stmt.stmts.addAll(((ImcSEQ)rimc.stmt).stmts);
		ImcESEQ lin = new ImcESEQ(stmt, new ImcBINOP(op, limc.expr, rimc.expr));
		return lin;
	}

}
