package compiler.interpreter;

import java.util.*;

import compiler.*;
import compiler.abstr.tree.AbsFunDef;
import compiler.frames.*;
import compiler.imcode.*;
import compiler.seman.SymbDesc;
import compiler.seman.SymbTable;
import java.awt.Label;

public class Interpreter {

	public static boolean debug = false;

	/*--- staticni del navideznega stroja ---*/
	
	/** Pomnilnik navideznega stroja. */
	public static HashMap<Integer, Object> mems = new HashMap<Integer, Object>();
	
        
        //Store in memory at address (integer)
	public static void stM(Integer address, Object value) {
		if (debug) System.out.println(" [" + address + "] <= " + value);
		mems.put(address, value);
	}

        //Load from memory address (integer)
	public static Object ldM(Integer address) {
		Object value = mems.get(address);
		if (debug) System.out.println(" [" + address + "] => " + value);
		return value;
	}
	
	/** Kazalec na vrh klicnega zapisa. */
	private static int fp = 1000;

	/** Kazalec na dno klicnega zapisa. */
	private static int sp = 1000;
	
	/*--- dinamicni del navideznega stroja ---*/
	
	/** Zacasne spremenljivke (`registri') navideznega stroja. */
	public HashMap<FrmTemp, Object> temps = new HashMap<FrmTemp, Object>();
		
        //Store v register (začasne/ temp spremenljivke)
	public void stT(FrmTemp temp, Object value) {
		if (debug) System.out.println(" " + temp.name() + " <= " + value);
		temps.put(temp, value);
	}

        //Load iz registra (začasne/ temp spremenljivke)
	public Object ldT(FrmTemp temp) {
		Object value = temps.get(temp);
		if (debug) System.out.println(" " + temp.name() + " => " + value);
		return value;
	}
	
	/*--- Izvajanje navideznega stroja. ---*/
        /** Za izvajanje vsake funkcije ustvarimo interpreter, kateremu podamo okvir funkcije in njeno kodo.
         * 
         * @param frame - Okvir funkcije
         * @param code - Koda funkcije
         */
	public Interpreter(FrmFrame frame, ImcSEQ code) {
		if (debug) {
			System.out.println("[START OF " + frame.label.name() + "]");
		}
	
		//stM(sp + frame.oldFPoffset, fp); //TODO: MAYBE
        stM(sp + frame.oldFPoffset(), fp); //TODO: MAYBE
 		fp = sp;
		sp = sp - frame.size();
		if (debug) {
			System.out.println("[FP=" + fp + "]");
			System.out.println("[SP=" + sp + "]");
		}

		stT(frame.FP, fp);

		int pc = 0;
		Object result = null;
		while (pc < code.stmts.size()) {
			if (debug) System.out.println("pc=" + pc);
			ImcCode instruction = code.stmts.get(pc);
			result = execute(instruction);
			if (result instanceof FrmLabel) {
				for (pc = 0; pc < code.stmts.size(); pc++) {
					instruction = code.stmts.get(pc);
					if ((instruction instanceof ImcLABEL) && (((ImcLABEL) instruction).label.name().equals(((FrmLabel) result).name())))
						break;
				}
			}
			else
				pc++;
		}
		
		//fp = (Integer) ldM(fp + frame.oldFPoffset);
		fp = (Integer) ldM(fp + frame.oldFPoffset());
		sp = sp + frame.size();
		if (debug) {
			System.out.println("[FP=" + fp + "]");
			System.out.println("[SP=" + sp + "]");
		}
		
		stM(sp, result);
		if (debug) {
			System.out.println("[RV=" + result + "]");
		}

		if (debug) {
			System.out.println("[END OF " + frame.label.name() + "]");
		}
	}
	
	public Object execute(ImcCode instruction) {
		
		if (instruction instanceof ImcBINOP) {
			ImcBINOP instr = (ImcBINOP) instruction;
			Object fstSubValue = execute(instr.limc);
			Object sndSubValue = execute(instr.rimc);
			switch (instr.op) {
			case ImcBINOP.OR:
				return ((((Integer) fstSubValue).intValue() != 0) || (((Integer) sndSubValue).intValue() != 0) ? 1 : 0);
			case ImcBINOP.AND:
				return ((((Integer) fstSubValue).intValue() != 0) && (((Integer) sndSubValue).intValue() != 0) ? 1 : 0);
			case ImcBINOP.EQU:
				return (((Integer) fstSubValue).intValue() == ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.NEQ:
				return (((Integer) fstSubValue).intValue() != ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.LTH:
				return (((Integer) fstSubValue).intValue() < ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.GTH:
				return (((Integer) fstSubValue).intValue() > ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.LEQ:
				return (((Integer) fstSubValue).intValue() <= ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.GEQ:
				return (((Integer) fstSubValue).intValue() >= ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.ADD:
				return (((Integer) fstSubValue).intValue() + ((Integer) sndSubValue).intValue());
			case ImcBINOP.SUB:
				return (((Integer) fstSubValue).intValue() - ((Integer) sndSubValue).intValue());
			case ImcBINOP.MUL:
				return (((Integer) fstSubValue).intValue() * ((Integer) sndSubValue).intValue());
			case ImcBINOP.DIV:
				return (((Integer) fstSubValue).intValue() / ((Integer) sndSubValue).intValue());
			//TODO: MOD LOGIC
			//case ImcBINOP.MOD:
			//	return (((Integer) fstSubValue).intValue() % ((Integer) sndSubValue).intValue());
                        /*
			case ImcBINOP.EQUs:
				return (((String) fstSubValue).compareTo((String) sndSubValue)) == 0 ? 1 : 0;
			case ImcBINOP.NEQs:
				return (((String) fstSubValue).compareTo((String) sndSubValue)) != 0 ? 1 : 0;
			case ImcBINOP.LTHs:
				return (((String) fstSubValue).compareTo((String) sndSubValue)) < 0 ? 1 : 0;
			case ImcBINOP.GTHs:
				return (((String) fstSubValue).compareTo((String) sndSubValue)) > 0 ? 1 : 0;
			case ImcBINOP.LEQs:
				return (((String) fstSubValue).compareTo((String) sndSubValue)) <= 0 ? 1 : 0;
			case ImcBINOP.GEQs:
				return (((String) fstSubValue).compareTo((String) sndSubValue)) >= 0 ? 1 : 0;
			}
                        */
                        }
                    Report.error("Internal error.");
                    return null;
		}
		
		if (instruction instanceof ImcCALL) {
			ImcCALL instr = (ImcCALL) instruction;
                        
			int offset = 0;
			stM(sp + offset, new ImcCONST(fp)); 
                        offset += 4;
                        
			for (ImcCode arg : instr.args) {
				stM(sp + offset, execute(arg));
				offset += 4;
			}
			if (instr.label.name().equals("_Lsys::putInt")) {
				System.out.println((Integer) ldM(sp + 4));
				return null;
			}
			if (instr.label.name().equals("_Lsys::getInt")) {
				Scanner scanner = new Scanner(System.in);
				stM((Integer) ldM (sp + 4),scanner.nextInt());
				return null;
			}
			if (instr.label.name().equals("_Lsys::putString")) {
				System.out.println((String) ldM(sp + 4));
				return null;
			}
			if (instr.label.name().equals("_Lsys::getString")) {
				Scanner scanner = new Scanner(System.in);
				stM((Integer) ldM (sp + 4),scanner.next());
				return null;
			}
			//new Interpreter(compiler.lincode.CodeGenerator.framesByLabel.get(instr.label), (ImcSEQ) compiler.lincode.CodeGenerator.codesByLabel.get(instr.fun));
                        AbsFunDef calledFunctionDef = (AbsFunDef) SymbTable.fnd(instr.label.name().substring(1));
                        FrmFrame calledFunctionFrame = FrmDesc.getFrame(calledFunctionDef);
                       
                        new Interpreter(calledFunctionFrame, ImcCodeGen.linearCode.get(calledFunctionDef));
			return null;
		}
		
		if (instruction instanceof ImcCJUMP) {
			ImcCJUMP instr = (ImcCJUMP) instruction;
			Object cond = execute(instr.cond);
			if (cond instanceof Integer) {
				if (((Integer) cond).intValue() != 0)
					return instr.trueLabel;
				else
					return instr.falseLabel;
			}
			else Report.error("CJUMP: illegal condition type.");
		}
		
		if (instruction instanceof ImcCONST) {
			ImcCONST instr = (ImcCONST) instruction;
                        //TODO: STRING, LOGICAL, INT??
			return new Integer(instr.value); //.intValue
		}
		/*
		if (instruction instanceof ImcCONSTr) {
			ImcCONSTr instr = (ImcCONSTr) instruction;
			return new Float(instr.realValue);
		}
		
		if (instruction instanceof ImcCONSTs) {
			ImcCONSTs instr = (ImcCONSTs) instruction;
			return new String(instr.stringValue);
		}
                */
		
		if (instruction instanceof ImcJUMP) {
			ImcJUMP instr = (ImcJUMP) instruction;
			return instr.label;
		}
		
		if (instruction instanceof ImcLABEL) {
			return null;
		}
		
		if (instruction instanceof ImcMEM) {
			ImcMEM instr = (ImcMEM) instruction;
			return ldM((Integer) execute(instr.expr));
		}
		
		if (instruction instanceof ImcMOVE) {
			ImcMOVE instr = (ImcMOVE) instruction;
            //Load
			if (instr.dst instanceof ImcTEMP) {
				FrmTemp temp = ((ImcTEMP) instr.dst).temp;
				Object srcValue = execute(instr.src);
				stT(temp, srcValue);
				return srcValue;
			}
			//Store
			if (instr.dst instanceof ImcMEM) {
				Object dstValue = execute(((ImcMEM) instr.dst).expr);
				Object srcValue = execute(instr.src);
				stM((Integer) dstValue, srcValue);
				return srcValue;
			}
		}
		
		if (instruction instanceof ImcNAME) {
			ImcNAME instr = (ImcNAME) instruction;
			String imeLabele = instr.label.name();
			if (imeLabele.equals("FP")) return fp;
			if (imeLabele.equals("SP")) return sp;
			if (ImcCodeGen.nasloviGlobalnih.containsKey(imeLabele)){
			    return ImcCodeGen.nasloviGlobalnih.get(imeLabele);
            }
		}
		
		if (instruction instanceof ImcTEMP) {
			ImcTEMP instr = (ImcTEMP) instruction;
			return ldT(instr.temp);
		}
                
		return null;
	}
        
        private static final String[] predefinedFunctions = new String[]{"putInt", "getInt", "putString", "getString"};
        public static boolean isPredefined(String name){
            boolean predef = false;
            for(String pre : predefinedFunctions){
                if(name.equals(pre)) predef = true;
            }
            
            return predef;
        }
	
}
