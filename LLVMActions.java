
import java.util.HashMap;
import java.util.Stack;

enum VarType{ INT, REAL, STRING, UNKNOWN }

class TypeInfo{
    boolean stringDeclared = false;
    boolean numberDeclared = false;
    VarType type;
    public TypeInfo (VarType t) {
        type = t;
    }
}

class Value{
    public String name;
    public VarType type;
    public Value( String name, VarType type ){
        this.name = name;
        this.type = type;
    }
}

public class LLVMActions extends KoalaBaseListener {

    HashMap<String, TypeInfo> variables = new HashMap<String, TypeInfo>();
    Stack<Value> stack = new Stack<Value>();


    @Override
    public void exitAssign(KoalaParser.AssignContext ctx) {
        String ID = ctx.ID().getText();
        Value v = stack.pop();
        if( v.type == VarType.STRING ) {
            if (!variables.containsKey(ID))
                variables.put(ID, new TypeInfo(v.type));
            if (!variables.get(ID).stringDeclared) {
                LLVMGenerator.declare_string(ID);
                variables.get(ID).stringDeclared = true;
            }
            LLVMGenerator.assign_string(ID, v.name);
            variables.get(ID).type = v.type;
        }
        if( v.type == VarType.INT ){
            if (!variables.containsKey(ID))
                variables.put(ID, new TypeInfo(v.type));
            if (!variables.get(ID).numberDeclared) {
                LLVMGenerator.declare_i32(ID);
                variables.get(ID).numberDeclared = true;
            }
            LLVMGenerator.assign_i32(ID, v.name);
            variables.get(ID).type = v.type;
        }
        if( v.type == VarType.REAL ){
            if (!variables.containsKey(ID))
                variables.put(ID, new TypeInfo(v.type));
            if (!variables.get(ID).numberDeclared) {
                LLVMGenerator.declare_double(ID);
                variables.get(ID).numberDeclared = true;
            }
            LLVMGenerator.assign_double(ID, v.name);
            variables.get(ID).type = v.type;
        }
    }

    @Override
    public void exitProg(KoalaParser.ProgContext ctx) {
        System.out.println( LLVMGenerator.generate() );
    }

    @Override
    public void exitString(KoalaParser.StringContext ctx) {
        String val = ctx.STRING().getText();
        val = val.substring(1, val.length()-1);
        LLVMGenerator.createStringConst(val);
        stack.push( new Value("@.str."+(LLVMGenerator.str_reg-1), VarType.STRING) );
    }

    @Override
    public void exitInt(KoalaParser.IntContext ctx) {
        stack.push( new Value(ctx.INT().getText(), VarType.INT) );
    }

    @Override
    public void exitReal(KoalaParser.RealContext ctx) {
        stack.push( new Value(ctx.REAL().getText(), VarType.REAL) );
    }

    @Override
    public void exitId(KoalaParser.IdContext ctx) {
        String ID = ctx.ID().getText();
        if (variables.containsKey(ID)) {
            VarType type = variables.get(ID).type;
            if (type == VarType.STRING) {
                stack.push(new Value("%" + (ID), VarType.STRING));
            }
            if( type == VarType.INT ){
                LLVMGenerator.load_i32( ID );
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }
            if( type == VarType.REAL ){
                LLVMGenerator.load_double( ID );
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
            }
        } else {
            error(ctx.getStart().getLine(), "unknown variable "+ID);
        }
    }

    @Override
    public void exitStrid(KoalaParser.StridContext ctx){
        String ID = ctx.ID().getText();
        if (variables.containsKey(ID)) {
            VarType type = variables.get(ID).type;
            if (type == VarType.STRING) {
                stack.push(new Value("%" + (ID), VarType.STRING));
            }
            if( type == VarType.INT ){
                LLVMGenerator.load_i32( ID );
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }
            if( type == VarType.REAL ){
                LLVMGenerator.load_double( ID );
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
            }
        } else {
            error(ctx.getStart().getLine(), "unknown variable "+ID);
        }
    }

    @Override
    public void exitAdd(KoalaParser.AddContext ctx) {
        Value v2 = stack.pop();
        Value v1 = stack.pop();
        if( v1.type == v2.type ) {
            /*if( v1.type == VarType.STRING ){
                LLVMGenerator.add_string(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }*/
            if( v1.type == VarType.INT ){
                LLVMGenerator.add_i32(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }
            if( v1.type == VarType.REAL ){
                LLVMGenerator.add_double(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
            }
        } else {
            error(ctx.getStart().getLine(), "add type mismatch");
        }
    }

    @Override
    public void exitSub(KoalaParser.SubContext ctx) {
        Value v2 = stack.pop();
        Value v1 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT ){
                LLVMGenerator.sub_i32(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }
            if( v1.type == VarType.REAL ){
                LLVMGenerator.sub_double(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
            }
        } else {
            error(ctx.getStart().getLine(), "add type mismatch");
        }
    }

    @Override
    public void exitMult(KoalaParser.MultContext ctx) {
        Value v2 = stack.pop();
        Value v1 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT ){
                LLVMGenerator.mult_i32(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }
            if( v1.type == VarType.REAL ){
                LLVMGenerator.mult_double(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
            }
        } else {
            error(ctx.getStart().getLine(), "mult type mismatch");
        }
    }

    @Override
    public void exitDiv(KoalaParser.DivContext ctx) {
        Value v2 = stack.pop();
        Value v1 = stack.pop();
        if(!v2.name.equals("0")) {
            if( v1.type == v2.type ) {
                if( v1.type == VarType.INT ){
                    LLVMGenerator.div_i32(v1.name, v2.name);
                    stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
                }
                if( v1.type == VarType.REAL ){
                    LLVMGenerator.div_double(v1.name, v2.name);
                    stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
                }
            } else {
                error(ctx.getStart().getLine(), "division type mismatch");
            }

        } else {
            error(ctx.getStart().getLine(), "division by zero");
        }
    }

    @Override
    public void exitTostring(KoalaParser.TostringContext ctx) {
        Value v = stack.pop();
        if (v.type == VarType.INT || v.type == VarType.REAL) {
            LLVMGenerator.tostring( v.name, v.type );
            stack.push( new Value("%_tmp."+(LLVMGenerator.tmp_reg-1), VarType.STRING) );
        } else {
            stack.push(v);
        }
    }

    @Override
    public void exitToint(KoalaParser.TointContext ctx) {
        Value v = stack.pop();
        if (v.type == VarType.REAL)
            LLVMGenerator.fptosi( v.name );
        else if (v.type == VarType.STRING)
            LLVMGenerator.atoi( v.name );
        stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
    }

    @Override
    public void exitToint2(KoalaParser.Toint2Context ctx) {
        Value v = stack.pop();
        if (v.type == VarType.REAL)
            LLVMGenerator.fptosi( v.name );
        else if (v.type == VarType.STRING)
            LLVMGenerator.atoi( v.name );
        stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
    }

    @Override
    public void exitToreal(KoalaParser.TorealContext ctx) {
        Value v = stack.pop();
        if (v.type == VarType.INT)
            LLVMGenerator.sitofp( v.name );
        else if (v.type == VarType.STRING)
            LLVMGenerator.atof( v.name );
        stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
    }

    @Override
    public void exitToreal2(KoalaParser.Toreal2Context ctx) {
        Value v = stack.pop();
        if (v.type == VarType.INT)
            LLVMGenerator.sitofp( v.name );
        else if (v.type == VarType.STRING)
            LLVMGenerator.atof( v.name );
        stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
    }

    @Override
    public void exitPrint(KoalaParser.PrintContext ctx) {
        Value v = stack.pop();
        VarType type = v.type;
        if( type != null ) {
            if( type == VarType.STRING )
                LLVMGenerator.printf( v.name );
            else {
                error(ctx.getStart().getLine(), "expected type: STRING, got "+type+" instead");
            }
        } else { // TODO: not needed
            error(ctx.getStart().getLine(), "unknown variable "+v.name);
        }
    }

    @Override
    public void exitRead(KoalaParser.ReadContext ctx) {
        String ID = ctx.ID().getText();
        if (!variables.containsKey(ID))
            variables.put(ID, new TypeInfo(VarType.STRING));
        if (!variables.get(ID).stringDeclared) {
            LLVMGenerator.declare_string(ID);
            variables.get(ID).stringDeclared = true;
        }
        LLVMGenerator.scanf(ID);
        variables.get(ID).type = VarType.STRING;
    }

    int getTypeId(VarType t) {
        if (t == VarType.STRING)
            return 0;
        else
            return 1;
    }

    void error(int line, String msg){
        System.err.println("Error, line "+line+", "+msg);
        System.exit(1);
    }

    //IF, IF ELSE
    @Override public void enterBlockif(KoalaParser.BlockifContext ctx){
        LLVMGenerator.ifstart();
    }

    @Override
    public void exitBlockif(KoalaParser.BlockifContext ctx) {
        LLVMGenerator.ifend();
    }

    @Override
    public void exitIf(KoalaParser.IfContext ctx) {
    }

    @Override public void enterBlockifelse(KoalaParser.BlockifelseContext ctx){
        LLVMGenerator.ifstart();
    }

    @Override
    public void enterBlockelse(KoalaParser.BlockelseContext ctx) {
        LLVMGenerator.elsestart();
    }

    @Override
    public void exitBlockelse(KoalaParser.BlockelseContext ctx) {
        LLVMGenerator.elseend();
    }
    //WHILE
    @Override
    public void enterWhileloop(KoalaParser.WhileloopContext ctx) {
        LLVMGenerator.enterwhile();
    }
    @Override
    public void enterWhilecond(KoalaParser.WhilecondContext ctx) {
        LLVMGenerator.exitwhile();
    }
    @Override
    public void enterBlockwhile(KoalaParser.BlockwhileContext ctx) {
        LLVMGenerator.enterblockwhile();
    }
    @Override
    public void exitBlockwhile(KoalaParser.BlockwhileContext ctx) {
        LLVMGenerator.exitblockwhile();
    }


    @Override
    public void exitEqual(KoalaParser.EqualContext ctx) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.eq(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.oeq(v2.name, v1.name);
            //TODO: Copare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(ctx.getStart().getLine(), "koala doesn't know how compare Strings :C");
        } else {
            error(ctx.getStart().getLine(), "type mismatch");
        }
    }

    @Override
    public void exitMore(KoalaParser.MoreContext ctx) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.sgt(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.ogt(v2.name, v1.name);
            //TODO: Copare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(ctx.getStart().getLine(), "koala doesn't know how compare Strings :C");
        } else {
            error(ctx.getStart().getLine(), "type mismatch");
        }
    }

    @Override
    public void exitLess(KoalaParser.LessContext ctx) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.slt(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.olt(v2.name, v1.name);
            //TODO: Copare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(ctx.getStart().getLine(), "koala doesn't know how compare Strings :C");
        } else {
            error(ctx.getStart().getLine(), " type mismatch");
        }
    }

    @Override
    public void exitNotequal(KoalaParser.NotequalContext ctx) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.ne(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.une(v2.name, v1.name);
            //TODO: Copare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(ctx.getStart().getLine(), "koala doesn't know how compare Strings :C");
        } else {
            error(ctx.getStart().getLine(), "type mismatch");
        }
    }

    @Override
    public void exitLessequal(KoalaParser.LessequalContext ctx) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.sle(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.ole(v2.name, v1.name);
            //TODO: Copare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(ctx.getStart().getLine(), "koala doesn't know how compare Strings :C");
        } else {
            error(ctx.getStart().getLine(), "type mismatch");
        }
    }

    @Override
    public void exitMoreequal(KoalaParser.MoreequalContext ctx) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.sge(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.oge(v2.name, v1.name);
            //TODO: Copare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(ctx.getStart().getLine(), "koala doesn't know how compare Strings :C");
        } else {
            error(ctx.getStart().getLine(), "type mismatch");
        }
    }

}
