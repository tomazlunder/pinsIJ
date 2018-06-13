package compiler.imcode;

import compiler.*;

/**
 * Konstanta.
 * 
 * @author sliva
 */
public class ImcCONST extends ImcExpr {

	/** Vrednost.  */
	public Integer value;
	public String stringValue;

	/**
	 * Ustvari novo konstanto.
	 * 
	 * @param value Vrednost konstante.
	 */
	public ImcCONST(Integer value) {
		this.value = value;
		this.stringValue = null;
	}

	public ImcCONST(String stringValue){
		this.stringValue = stringValue;
		this.value = null;
	}

	@Override
	public void dump(int indent) {
		if(value != null) Report.dump(indent, "CONST value=" + value.toString());
		else if(stringValue != null) Report.dump(indent, "CONST value= " + stringValue);
	}

	@Override
	public ImcESEQ linear() {
		return new ImcESEQ(new ImcSEQ(), this);
	}

}
