package compiler.synan;

import compiler.Report;
import compiler.lexan.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.Position;
import java.util.Vector;

/**
 * Sintaksni analizator.
 *
 * @author sliva
 */
public class SynAn {

    /**
     * Leksikalni analizator.
     */
    private LexAn lexAn;

    /**
     * Ali se izpisujejo vmesni rezultati.
     */
    private boolean dump;

    private Symbol buffer;

    /**
     * Ustvari nov sintaksni analizator.
     *
     * @param lexAn Leksikalni analizator.
     * @param dump Ali se izpisujejo vmesni rezultati.
     */
    public SynAn(LexAn lexAn, boolean dump) {
        this.lexAn = lexAn;
        this.dump = dump;
    }

    /**
     * Opravi sintaksno analizo.
     */
    public AbsTree parse() throws Exception {
        buffer = lexAn.lexAn();
        AbsTree tree = parseSource();
        return tree;
    }

    private AbsTree parseSource() throws Exception {
        switch (buffer.token) {
            case Token.KW_TYP:
            case Token.KW_FUN:
            case Token.KW_VAR:
                dump("source -> definitions");
                AbsDefs defs = parseDefinitions();
                return defs;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseSource)! Symbol:" + buffer.toString());
        }
        AbsDefs defs_n = null;
        return defs_n; //Unreachable
    }

    private AbsDefs parseDefinitions() throws Exception {
        Vector<AbsDef> defs = new Vector<>();
        Position pos = buffer.position;
        
        switch (buffer.token) {
            case Token.KW_TYP:
            case Token.KW_FUN:
            case Token.KW_VAR:
                dump("definitions -> definition definitions'");
                defs.add(parseDefinition());
                defs = parseDefinitions1(defs);
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseDefinitions)! Symbol:" + buffer.toString());
        }
        return new AbsDefs(new Position(pos,defs.elementAt(defs.size()-1).position),defs);
    }

    private Vector<AbsDef> parseDefinitions1(Vector<AbsDef> defs) throws Exception {
        switch (buffer.token) {
            case Token.SEMIC:
                dump("definitions' -> ; definition definitions'");
                buffer = lexAn.lexAn();
                defs.add(parseDefinition());
                defs = parseDefinitions1(defs);
                break;

            case Token.RBRACE:
            case Token.EOF:
                dump("definitions' -> ε");
                break;

            default:
                Report.error(buffer.position, "Napaka v SA (v parseDefinitions1)! Symbol:" + buffer.toString());
        }
        return defs;
    }

    private AbsDef parseDefinition() throws Exception {
        AbsDef to_ret = null;
        switch (buffer.token) {
            case Token.KW_TYP:
                dump("definition -> type_definition");
                to_ret = parseTypeDefinition();
                break;
            case Token.KW_FUN:
                dump("definition -> function_definition");
                to_ret = parseFunctionDefinition();
                break;
            case Token.KW_VAR:
                dump("definition -> variable_definition");
                to_ret = parseVariableDefinition();
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseDefinition)! Symbol:" + buffer.toString());
        }
        return to_ret;
    }

    private AbsTypeDef parseTypeDefinition() throws Exception {
        Position pos = buffer.position;
        String name = null;
        AbsType type = null;
        switch (buffer.token) {
            case Token.KW_TYP:
                dump("type_definition -> typ identifier : type");
                bufferSkip(1);
                name = buffer.lexeme;
                bufferSkip(2);
                type = parseType();
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseTypeDefinition)! Symbol:" + buffer.toString());
        }
        return new AbsTypeDef(new Position(pos,type.position), name, type);
    }

    private AbsType parseType() throws Exception {
        Position pos = buffer.position;
        int type = -1;
        switch (buffer.token) {
            case Token.IDENTIFIER:
                dump("type -> identifier");
                String name = buffer.lexeme;
                buffer = lexAn.lexAn();
                return new AbsTypeName(pos, name);
            case Token.LOGICAL:
                dump("type -> logical");
                type = 0;
                buffer = lexAn.lexAn();
                break;
            case Token.INTEGER:
                dump("type -> integer");
                type = 1;
                buffer = lexAn.lexAn();
                break;
            case Token.STRING:
                dump("type -> string");
                type = 2;
                buffer = lexAn.lexAn();
                break;
            case Token.KW_ARR:
                dump("type -> arr [ int_constant ] type");
                buffer = lexAn.lexAn();
                buffer = lexAn.lexAn();
                int value = Integer.parseInt(buffer.lexeme);
                buffer = lexAn.lexAn(); 
                buffer = lexAn.lexAn();
                return new AbsArrType(pos, value, parseType());

            default:
                Report.error(buffer.position, "Napaka v SA (v parseType)! Symbol:" + buffer.toString());
        }
        return new AbsAtomType(pos, type);
    }

    private AbsFunDef parseFunctionDefinition() throws Exception {
        Position pos1 = null;
        Position pos2 = null;
        String name = null;
        Vector<AbsPar> pars = null;
        AbsType type = null;
        AbsExpr expr = null;

        switch (buffer.token) {
            case Token.KW_FUN:
                pos1 = buffer.position;
                dump("function_definition -> fun identifier ( parameters ) : type = expression");
                bufferSkip(1);
                name = buffer.lexeme;
                bufferSkip(2);
                pars = parseParameters();
                bufferSkip(2);
                type = parseType();
                bufferSkip(1);
                expr = parseExpression();
                pos2 = buffer.position;
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseFunctionDefinition)! Symbol:" + buffer.toString());
        }

        return new AbsFunDef(new Position(pos1,pos2), name, pars, type, expr);
    }

    private Vector<AbsPar> parseParameters() throws Exception {
        Vector<AbsPar> pars = new Vector<AbsPar>();
        switch (buffer.token) {
            case Token.IDENTIFIER:
                dump("parameters -> parameter parameters'");
                pars.add(parseParameter());
                pars = parseParameters1(pars);
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseParameters)! Symbol:" + buffer.toString());
        }
        return pars;
    }

    private Vector<AbsPar> parseParameters1(Vector<AbsPar> pars) throws Exception {

        switch (buffer.token) {
            case Token.RPARENT:
                dump("parameters' -> ε");
                return pars;
            case Token.COMMA:
                dump("parameters' -> , parameter parameters'");
                bufferSkip(1);
                pars.add(parseParameter());
                pars = parseParameters1(pars);
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseParameters1)! Symbol:" + buffer.toString());
        }
        return pars;
    }

    private AbsPar parseParameter() throws Exception {
        Position pos = buffer.position;
        String name = null;
        AbsType type = null;
        switch (buffer.token) {
            case Token.IDENTIFIER:
                dump("parameter -> identifier : type");
                name = buffer.lexeme;
                bufferSkip(2);
                type = parseType();
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseParameter)! Symbol:" + buffer.toString());
        }
        return new AbsPar(new Position(pos,type.position), name, type);
    }

    private AbsExpr parseExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("expression -> logical_ior_expression expression'");
                AbsExpr expr = parseLogicalIORExpression();
                return parseExpression1(expr);

            default:
                Report.error(buffer.position, "Napaka v SA (v parseExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n; //Unreachable
    }

    private AbsExpr parseExpression1(AbsExpr expr) throws Exception{
        Position pos1 = buffer.position;
        switch (buffer.token) {
            case Token.LBRACE:
                dump("expression' -> { WHERE definitions }");
                bufferSkip(2);
                AbsDefs defs = parseDefinitions();
                bufferSkip(1);
                Position pos2 = buffer.position;
                return new AbsWhere(new Position(pos1,pos2), expr, defs);

            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.RBRACE:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("expression' -> ε");
                break;

            default:
                Report.error(buffer.position, "Napaka v SA (v parseExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parseLogicalIORExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");
                AbsExpr expr = parseLogicalANDExpression();
                return parseLogicalIORExpression1(expr);

            default:
                Report.error(buffer.position, "Napaka v SA (v parseLogicalIORExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n; //Unreachable
    }

    private AbsExpr parseLogicalIORExpression1(AbsExpr expr) throws Exception {
        Position pos = buffer.position;
        switch (buffer.token) {
            /*
            case Token.IOR:
                dump("logical_ior_expression' -> | logical_and_expression logical_ior_expression'");
                bufferSkip(1);
                AbsExpr expr1 = parseLogicalANDExpression();
                AbsExpr expr2 = parseLogicalIORExpression1();
                return new AbsBinExpr(pos, 0, expr1, expr2);*/
            
            case Token.IOR:
                dump("logical_ior_expression' -> | logical_and_expression logical_ior_expression'");
                bufferSkip(1);
                AbsExpr expr2 = parseLogicalANDExpression();
                AbsExpr expr1 = new AbsBinExpr(new Position(pos,expr2.position), 0, expr, expr2);
                return parseLogicalIORExpression1(expr1);

            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("logical_ior_expression' -> ε");
                break;

            default:
                Report.error(buffer.position, "Napaka v SA (v parseLogicalIORExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parseLogicalANDExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("logical_and_expression -> compare_expression logical_and_expression'");
                AbsExpr expr = parseCompareExpression();
                return parseLogicalANDExpression1(expr);

            default:
                Report.error(buffer.position, "Napaka v SA (v parseLogicalANDExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n; //Unreachable
    }

    private AbsExpr parseLogicalANDExpression1(AbsExpr expr) throws Exception {
        Position pos = buffer.position;
        switch (buffer.token) {
            case Token.AND:
                dump("logical_and_expression' -> & compare_expression logical_and_expression'");
                bufferSkip(1);
                AbsExpr expr2 = parseCompareExpression();
                AbsExpr expr1 = new AbsBinExpr(new Position(pos,expr2.position), 1, expr, expr2);
                return parseLogicalANDExpression1(expr1);

            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.IOR:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("logical_and_expression' -> ε");
                break;

            default:
                Report.error(buffer.position, "Napaka v SA (v parseLogicalANDExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parseCompareExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("compare_expression -> additive_expression compare_expression'");
                AbsExpr expr = parseAdditiveExpression();
                return parseCompareExpression1(expr);
                

            default:
                Report.error(buffer.position, "Napaka v SA (v parseCompareExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n;
    }

    private AbsExpr parseCompareExpression1(AbsExpr expr) throws Exception {
        Position pos = buffer.position;
        AbsExpr expr1;
        switch (buffer.token) {
            case Token.EQU:
                dump("compare_expression' -> == additive_expression");
                bufferSkip(1);
                expr1 = parseAdditiveExpression();
                return new AbsBinExpr(new Position(expr.position,expr1.position),2,expr,expr1);
            case Token.NEQ:
                dump("compare_expression' -> != additive_expression");
                bufferSkip(1);
                expr1 = parseAdditiveExpression();
                return new AbsBinExpr(new Position(expr.position,expr1.position),3,expr,expr1);
            case Token.LEQ:
                dump("compare_expression' -> <= additive_expression");
                bufferSkip(1);
                expr1 = parseAdditiveExpression();
                return new AbsBinExpr(new Position(expr.position,expr1.position),4,expr,expr1);
            case Token.GEQ:
                dump("compare_expression' -> >= additive_expression");
                bufferSkip(1);
                expr1 = parseAdditiveExpression();
                return new AbsBinExpr(new Position(expr.position,expr1.position),5,expr,expr1);
            case Token.LTH:
                dump("compare_expression' -> < additive_expression");
                bufferSkip(1);
                expr1 = parseAdditiveExpression();
                return new AbsBinExpr(new Position(expr.position,expr1.position),6,expr,expr1);
            case Token.GTH:
                dump("compare_expression' -> > additive_expression");
                bufferSkip(1);
                expr1 = parseAdditiveExpression();
                return new AbsBinExpr(new Position(expr.position,expr1.position),7,expr,expr1);

            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.IOR:
            case Token.AND:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("compare_expression' -> ε");
                break;

            default:
                Report.error(buffer.position, "Napaka v SA (v parseCompareExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parseAdditiveExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("additive_expression -> multiplicatiove_expression additive_expression'");
                AbsExpr expr = parseMultiplicativeExpression();
                return parseAdditiveExpression1(expr);
            default:
                Report.error(buffer.position, "Napaka v SA (v parseAdditiveExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n; //Unreachable
    }

    private AbsExpr parseAdditiveExpression1(AbsExpr expr) throws Exception {
        Position pos = buffer.position;
        AbsExpr expr1;
        AbsExpr expr2;
        switch (buffer.token) {
            case Token.ADD:
                dump("additive_expression' -> + multiplicative_expression additive_expression'");
                bufferSkip(1);
                expr1 = parseMultiplicativeExpression();
                expr2 = new AbsBinExpr(new Position(expr.position,expr1.position),8,expr,expr1);
                return parseAdditiveExpression1(expr2);
            case Token.SUB:
                dump("additive_expression' -> - multiplicative_expression additive_expression'");
                bufferSkip(1);
                expr1 = parseMultiplicativeExpression();
                expr2 = new AbsBinExpr(new Position(expr.position,expr1.position),9,expr,expr1);
                return parseAdditiveExpression1(expr2);
                
            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.IOR:
            case Token.AND:
            case Token.EQU:
            case Token.NEQ:
            case Token.LEQ:
            case Token.GEQ:
            case Token.LTH:
            case Token.GTH:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("additive_expression' -> ε");
                break;
                
            default:
                Report.error(buffer.position, "Napaka v SA (v parseAdditiveExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parseMultiplicativeExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
                AbsExpr expr = parsePrefixExpression(buffer.position);
                return parseMultiplicativeExpression1(expr);
                
                
            default:
                Report.error(buffer.position, "Napaka v SA (v parseMultiplicativeExpression)! Symbol:" + buffer.toString());
        }
        Report.error("Error v pME");
        AbsExpr expr_n = null;
        return expr_n; //Unreachable
    }

    private AbsExpr parseMultiplicativeExpression1(AbsExpr expr) throws Exception {
        
        Position pos = buffer.position;
        String token = buffer.toString();
        AbsExpr expr1;
        AbsExpr expr2;
        switch (buffer.token) {
            case Token.MUL:
                dump("multiplicative_expression' -> * prefix_expression multiplicative_expression'");
                bufferSkip(1);
                expr1 = parsePrefixExpression(pos);
                expr2 = new AbsBinExpr(new Position(expr.position,expr1.position),10,expr,expr1);
                return parseMultiplicativeExpression1(expr2);
                
            case Token.DIV:
                dump("multiplicative_expression' -> / prefix_expression multiplicative_expression'");
                bufferSkip(1);
                expr1 = parsePrefixExpression(pos);
                expr2 = new AbsBinExpr(new Position(expr.position,expr1.position),11,expr,expr1);
                return parseMultiplicativeExpression1(expr2);

            case Token.MOD:
                dump("multiplicative_expression' -> % prefix_expression multiplicative_expression'");
                bufferSkip(1);
                expr1 = parsePrefixExpression(pos);
                expr2 = new AbsBinExpr(new Position(expr.position,expr1.position),12,expr,expr1);
                return parseMultiplicativeExpression1(expr2);

                
            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.IOR:
            case Token.AND:
            case Token.EQU:
            case Token.NEQ:
            case Token.LEQ:
            case Token.GEQ:
            case Token.LTH:
            case Token.GTH:
            case Token.ADD:
            case Token.SUB:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("multiplicative_expression' -> ε");
                break;
                
            default:
                Report.error(buffer.position, "Napaka v SA (v parseMultiplicativeExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parsePrefixExpression(Position pos_start) throws Exception {
        Position pos = buffer.position;
        AbsExpr expr1;
        switch (buffer.token) {
            case Token.ADD:
                dump("prefix_expression -> + prefix_expression");
                bufferSkip(1);
                expr1 = parsePrefixExpression(pos_start);
                return new AbsUnExpr(new Position(pos_start,expr1.position),0,expr1);
            case Token.SUB:
                dump("prefix_expression -> - prefix_expression");
                bufferSkip(1);
                expr1 = parsePrefixExpression(pos_start);
                return new AbsUnExpr(new Position(pos_start,expr1.position),1,expr1);
            case Token.NOT:
                dump("prefix_expression -> ! prefix_expression");
                bufferSkip(1);
                expr1 = parsePrefixExpression(pos_start);
                return new AbsUnExpr(new Position(pos_start,expr1.position),4,expr1);
                
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("prefix_expression -> postfix_expression");
                return parsePostfixExpression();
                
            default:
                Report.error(buffer.position, "Napaka v SA (v parsePrefixExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n; //unreachable
    }

    private AbsExpr parsePostfixExpression() throws Exception {
        switch (buffer.token) {
            case Token.IDENTIFIER:
                
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("postfix_expression -> atom_expression postfix_expression'");
                AbsExpr expr1 = parseAtomExpression();
                return parsePostfixExpression1(expr1);
            default:
                Report.error(buffer.position, "Napaka v SA (v parsePostfixExpression)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n;
    }

    private AbsExpr parsePostfixExpression1(AbsExpr expr) throws Exception {
        Position pos = buffer.position;
        AbsExpr expr1;
        AbsExpr expr2;
        switch (buffer.token) {
            case Token.LBRACKET:
                dump("postifx_expression' -> [ expression ] postfix_expression'");
                bufferSkip(1);
                expr1 = parseExpression();
                expr2 = new AbsBinExpr(new Position(expr.position,expr1.position),14,expr,expr1);
                bufferSkip(1);
                return parsePostfixExpression1(expr2);
                
            case Token.SEMIC:
            case Token.COLON:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.IOR:
            case Token.AND:
            case Token.EQU:
            case Token.NEQ:
            case Token.LEQ:
            case Token.GEQ:
            case Token.LTH:
            case Token.GTH:
            case Token.ADD:
            case Token.SUB:
            case Token.MUL:
            case Token.DIV:
            case Token.MOD:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("postfix_expression' -> ε");
                break;
                
            default:
                Report.error(buffer.position, "Napaka v SA (v parsePostfixExpression1)! Symbol:" + buffer.toString());
        }
        return expr;
    }

    private AbsExpr parseAtomExpression() throws Exception {
        AbsExpr to_ret;
        Position pos = buffer.position;
        int type = -1;
        String value = null;
        switch (buffer.token) {
            case Token.IDENTIFIER: //OK
                dump("atom_expression -> identifier atom_expression'''");
                AbsVarName expr = new AbsVarName(pos, buffer.lexeme);
                bufferSkip(1);
                return parseAtomExpression3(expr);
                
            case Token.LPARENT: //!!!
                dump("atom_expression -> ( expressions )");
                bufferSkip(1);
                AbsExpr expr1 = parseExpressions();
                bufferSkip(1);
                return expr1;
                
            case Token.LBRACE: //OK
                dump("atom_expression -> { atom_expression''");
                bufferSkip(1);
                return parseAtomExpression2();
                
            case Token.INT_CONST: //OK
                dump("atom_expression -> int_constant");
                type = 1;
                value = buffer.lexeme;
                bufferSkip(1);
                break;
            case Token.LOG_CONST: //OK
                dump("atom_expression -> log_constant");
                type = 0;
                value = buffer.lexeme;
                bufferSkip(1);
                break;
            case Token.STR_CONST: //OK
                dump("atom_expression -> str_constant'");
                type = 2;
                value = buffer.lexeme;
                bufferSkip(1);
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseAtomExpression)! Symbol:" + buffer.toString());
        }
        return new AbsAtomConst(pos, type, value);
    }

    private AbsExpr parseAtomExpression3(AbsVarName expr) throws Exception {
        Position pos = buffer.position;
        switch (buffer.token) {
            case Token.LPARENT:
                dump("atom_expression''' -> ( expressions )");
                bufferSkip(1);
                Vector<AbsExpr> exprs = parseExpressions_asVector();
                pos = buffer.position;
                bufferSkip(1);
                return new AbsFunCall(new Position(expr.position,pos),expr.name,exprs);
            case Token.SEMIC:
            case Token.COLON:
            case Token.LBRACKET:
            case Token.RBRACKET:
            case Token.RPARENT:
            case Token.ASSIGN:
            case Token.COMMA:
            case Token.LBRACE:
            case Token.RBRACE:
            case Token.IOR:
            case Token.AND:
            case Token.EQU:
            case Token.NEQ:
            case Token.LEQ:
            case Token.GEQ:
            case Token.LTH:
            case Token.GTH:
            case Token.ADD:
            case Token.SUB:
            case Token.MUL:
            case Token.DIV:
            case Token.MOD:
            case Token.KW_THEN:
            case Token.KW_ELSE:
            case Token.EOF:
                dump("atom_expression''' -> ε");
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseAtomExpression3)! Symbol:" + buffer.toString());
        }
        return expr; //Unreachable
    }

    private AbsExpr parseAtomExpression2() throws Exception {
        Position pos = buffer.position;
        AbsExpr expr1;
        AbsExpr expr2;
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.STR_CONST:
            case Token.LOG_CONST:
                dump("atom_expression'' -> expression = expression }");
                expr1 = parseExpression();
                bufferSkip(1);
                expr2 = parseExpression();
                bufferSkip(1);
                return new AbsBinExpr(new Position(pos,expr2.position), 15, expr1, expr2);

            case Token.KW_IF:
                dump("atom_expression'' -> if expression then expression atom_expression'");
                bufferSkip(1);
                AbsExpr cond = parseExpression();
                bufferSkip(1);
                AbsExpr thenBody = parseExpression();
                return parseAtomExpression1(cond, thenBody, pos);

            case Token.KW_WHILE:
                dump("atom_expression'' -> while expression : expression }");
                bufferSkip(1);
                AbsExpr condi = parseExpression();
                bufferSkip(1);
                AbsExpr body = parseExpression();
                bufferSkip(1);
                return new AbsWhile(new Position(pos,body.position), condi, body);
            case Token.KW_FOR:
                dump("atom_expression'' -> for identifier = expression , expression , expression : expression }");
                bufferSkip(1);
                AbsVarName count = new AbsVarName(pos, buffer.lexeme);
                bufferSkip(2);
                AbsExpr lo = parseExpression();
                bufferSkip(1);
                AbsExpr hi = parseExpression();
                bufferSkip(1);
                AbsExpr step = parseExpression();
                bufferSkip(1);
                AbsExpr body2 = parseExpression();
                bufferSkip(1);
                return new AbsFor(new Position(pos,body2.position), count, lo, hi, step, body2);
            default:
                Report.error(buffer.position, "Napaka v SA (v parseAtomExpression2)! Symbol:" + buffer.toString());
        }
        AbsExpr expr_n = null;
        return expr_n;
    }

    private AbsExpr parseAtomExpression1(AbsExpr cond, AbsExpr body, Position pos_start) throws Exception {
        Position pos = buffer.position;
        switch (buffer.token) {
            case Token.RBRACE:
                dump("atom_expression' -> }");
                bufferSkip(1);
                return new AbsIfThen(new Position(pos_start,pos), cond, body);
            case Token.KW_ELSE:
                dump("atom_expression' -> else expression }");
                bufferSkip(1);
                AbsExpr else_body = parseExpression();
                bufferSkip(1);
                return new AbsIfThenElse(new Position(pos_start,pos), cond, body, else_body);
            default:
                Report.error(buffer.position, "Napaka v SA (v parseAtomExpression1)! Symbol:" + buffer.toString());
        }
        return new AbsIfThen(new Position(pos_start,pos), cond, body); //Unreachable, but IDE keeps bothering
    }

    private AbsExprs parseExpressions() throws Exception {
        Position pos = buffer.position;
        Vector<AbsExpr> exprs = new Vector<>();
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("expressions -> expression expressions'");
                exprs.add(parseExpression());
                exprs = parseExpressions1(exprs);
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseExpressions)! Symbol:" + buffer.toString());
        }
        return new AbsExprs(new Position(pos,exprs.elementAt(exprs.size()-1).position), exprs);
    }
    
    private Vector<AbsExpr> parseExpressions_asVector() throws Exception {
        Position pos = buffer.position;
        Vector<AbsExpr> exprs = new Vector<>();
        switch (buffer.token) {
            case Token.IDENTIFIER:
            case Token.INT_CONST:
            case Token.LPARENT:
            case Token.LBRACE:
            case Token.ADD:
            case Token.SUB:
            case Token.NOT:
            case Token.LOG_CONST:
            case Token.STR_CONST:
                dump("expressions -> expression expressions'");
                exprs.add(parseExpression());
                exprs = parseExpressions1(exprs);
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseExpressions)! Symbol:" + buffer.toString());
        }
        return exprs;
    }

    private Vector<AbsExpr> parseExpressions1(Vector<AbsExpr> exprs) throws Exception {
        switch (buffer.token) {
            case Token.RPARENT:
                dump("expressions' -> ε");
                return exprs;
            case Token.COMMA:
                dump("expressions' -> , expression expressions'");
                bufferSkip(1);
                exprs.add(parseExpression());
                exprs = parseExpressions1(exprs);
                return exprs;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseExpressions1)! Symbol:" + buffer.toString());
        }
        return exprs;
    }

    private AbsVarDef parseVariableDefinition() throws Exception {
        Position pos = null;
        String name = null;
        AbsType type = null;
        switch (buffer.token) {
            case Token.KW_VAR:
                pos = buffer.position;
                dump("variable_definition -> var identifier : type");
                bufferSkip(1);
                name = buffer.lexeme;
                bufferSkip(2);
                type = parseType();
                break;
            default:
                Report.error(buffer.position, "Napaka v SA (v parseVariableDefinition)! Symbol:" + buffer.toString());
        }
        return new AbsVarDef(new Position(pos,type.position), name, type);
    }

    private void bufferSkip(int n) throws Exception {
        for (int i = 0; i < n; i++) {
            buffer = lexAn.lexAn();
        }
    }

    /**
     * Izpise produkcijo v datoteko z vmesnimi rezultati.
     *
     * @param production Produkcija, ki naj bo izpisana.
     */
    private void dump(String production) {
        //System.out.println(production);
        if (!dump) {
            return;
        }
        if (Report.dumpFile() == null) {
            return;
        }
        //Report.dumpFile().println(buffer.toString() + "  " + production);
        Report.dumpFile().println(production);
    }

}
