
import java.util.*;
import java.lang.reflect.Method;

enum VarType{ INT, REAL, STRING, FUNCTION, UNKNOWN }

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

class FunctionStatement {
    FunctionStatement (Method m) {
        method = m;
    }

    FunctionStatement (Method m, String a) {
        method = m;
        args.add(a);
    }

    FunctionStatement (Method m, String a1, String a2) {
        method = m;
        args.add(a1);
        args.add(a2);
    }

    Method method;
    ArrayList<String> args = new ArrayList<String>();
}

class FunctionInfo {
    public ArrayList<String> fargNames = new ArrayList<String>();

    public ArrayList<FunctionStatement> statements = new ArrayList<FunctionStatement>();
    public ArrayList<ArrayList<VarType>> calledWithTypes = new ArrayList<ArrayList<VarType>>();
}

public class LLVMActions extends KoalaBaseListener {

    boolean returns = false;

    // function definition context
    Boolean functionDefContext = false;
    String functionName;
    FunctionInfo functionInfo;

    // call context
    Boolean global = true;
    FunctionInfo calledFunction;
    Stack<ArrayList<VarType>> callTypeStack = new Stack<ArrayList<VarType>>();
    ArrayList<VarType> callTypes = new ArrayList<VarType>();
    Stack<ArrayList<Value>> argumentValueStack = new Stack<ArrayList<Value>>();

    HashMap<String, TypeInfo> globalVariables = new HashMap<String, TypeInfo>();
    Stack<HashMap<String, TypeInfo>> localVariableStack = new Stack<HashMap<String, TypeInfo>>();
    HashMap<String, TypeInfo> localVariables = new HashMap<String, TypeInfo>();
    HashMap<String, FunctionInfo> functions = new HashMap<String, FunctionInfo>();
    Stack<Value> stack = new Stack<Value>();

	@Override
    public void exitFunname(DemoParser.FunnameContext ctx) {
        if (!functionDefContext) {
            String ID = ctx.ID().getText();
            if (!functions.containsKey(ID)) {
                functionName = ID;
                functionInfo = new FunctionInfo();

                functionDefContext = true;
            } else {
                error(ctx.getStart().getLine(), "function "+ID+" already defined");
            }
        } else {
            error(ctx.getStart().getLine(), "can't define function in local scope");
        }
    }

	@Override
    public void exitDefparam(DemoParser.DefparamContext ctx) {
        String ID = ctx.ID().getText();
        if (!functionInfo.fargNames.contains(ID))
            functionInfo.fargNames.add(ID);
        else
            error(ctx.getStart().getLine(), "argument names must be unique");
    }

	@Override
    public void enterBlockfun(DemoParser.BlockfunContext ctx) {
        try {
            functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterBlockfun", functionName.getClass()), functionName));
        } catch (Exception ignored) {}
    }

    // First 'do' method added to the call buffer
    public void doEnterBlockfun(String ID) {
        global = false;
        LLVMGenerator.functionstart(ID, callTypes);
    }

	@Override
    public void exitBlockfun(DemoParser.BlockfunContext ctx) {
        try {
            functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitBlockfun")));
        } catch (Exception ignored) {}
        functions.put(functionName, functionInfo);
        functionDefContext = false;
    }

    // Last 'do' method added to the call buffer
    public void doExitBlockfun() {
        LLVMGenerator.functionend();
        localVariableStack.pop();
        global = true;
    }

	@Override
    public void enterCall(DemoParser.CallContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterCall")));
            } catch (Exception ignored) {}
        } else {
            doEnterCall();
        }
    }

    public void doEnterCall() {
        argumentValueStack.push(new ArrayList<Value>());
        callTypeStack.push(new ArrayList<VarType>());
    }

	@Override
    public void exitCall(DemoParser.CallContext ctx) {
        String ID = ctx.ID().getText();
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitCall",
                                            ID.getClass(), line.getClass()), ID, line));
            } catch (Exception ignored) {
            }
        } else {
            doExitCall(ID, line);
        }
    }

    public void doExitCall(String ID, String line) {
        if (functions.containsKey(ID)) {
            calledFunction = functions.get(ID);
            callTypes = callTypeStack.pop();
            ArrayList<Value> argumentValues = argumentValueStack.pop();
            if (callTypes.size() == functionInfo.fargNames.size()) {
                if (!functionInfo.calledWithTypes.contains(callTypes)) {
                    localVariables = new HashMap<String, TypeInfo>();
                    localVariableStack.push(localVariables);
                    for (int i = 0; i < argumentValues.size(); i++)
                        localVariables.put(calledFunction.fargNames.get(i), new TypeInfo(callTypes.get(i)));
                    functionInfo.calledWithTypes.add(callTypes);
                    for (FunctionStatement stm : functionInfo.statements) {
                        try {
                            if (stm.args.size() == 0)
                                stm.method.invoke(this);
                            else if (stm.args.size() == 1)
                                stm.method.invoke(this, stm.args.get(0));
                            else
                                stm.method.invoke(this, stm.args.get(0), stm.args.get(1));
                        } catch (Exception ignored) {}
                    }
                }
                LLVMGenerator.call(ID, argumentValues);
                stack.push(...);
            } else {
                error(line, "number of arguments for function "+ID+" doesn't match the definition");
            }
        } else {
            error(line, "undefined function "+ID);
        }
        
    }

	@Override
    public void exitFparam(DemoParser.FparamContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitFparam")));
            } catch (Exception ignored) {}
        } else {
            doExitFparam();
        }
    }

    public void doExitFparam() {
        Value v = stack.pop();
        argumentValueStack.peek().add(v);
        callTypeStack.peek().add(v.type);
    }

    public String set_variable(String ID, TypeInfo t){
        String id;
        if(global){
            if(!globalVariables.containsKey(ID) ) {
                globalVariables.put(ID, t);
                LLVMGenerator.declare(ID, true);
            }
            id = "@"+ID;
        } else {
            if(!localVariables.containsKey(ID) ) {
                localVariables.put(ID, t);
                LLVMGenerator.declare(ID, false);
            }
            id = "%"+ID;
        }
        return id;
    }

	@Override
    public void exitRet(DemoParser.RetContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitRet")));
            } catch (Exception ignored) {}
        } else {
            doExitRet();
        }
    }

    public void doExitRet() {
        //TODO: this probably has to get v from context, because nothing before it puts the value on the stack
        Value v = stack.pop();
        //LLVMGenerator.load( "%"+function );
        LLVMGenerator.ret(v);
    }

    //TODO: handle local and global scope
	@Override
    public void exitAssign(KoalaParser.AssignContext ctx) {
        String ID = ctx.ID().getText();
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitRet", ID.getClass()), ID));
            } catch (Exception ignored) {}
        } else {
            doExitAssign(ID);
        }
    }

    // TODO: errors and check if there already exists function with this name. Or maybe you don't have to?
    public void doExitAssign(String ID) {
        HashMap<String, TypeInfo> variables = global ? globalVariables : localVariables;
        Value v = stack.pop();
        if (!existsInScope(ID))
            variables.put(ID, new TypeInfo(v.type));
        TypeInfo variable = getFromScope(ID);

        if( v.type == VarType.STRING ) {
            if (!variable.stringDeclared) {
                LLVMGenerator.declare_string(ID);
                variable.stringDeclared = true;
            }
            LLVMGenerator.assign_string(ID, v.name);
        }
        else if( v.type == VarType.INT ){
            if (!variable.numberDeclared) {
                LLVMGenerator.declare_i32(ID);
                variable.numberDeclared = true;
            }
            LLVMGenerator.assign_i32(ID, v.name);
        }
        else if ( v.type == VarType.REAL ){
            if (!variable.numberDeclared) {
                LLVMGenerator.declare_double(ID);
                variable.numberDeclared = true;
            }
            LLVMGenerator.assign_double(ID, v.name);
        }

        variable.type = v.type;
    }

    public boolean existsInScope(String ID) {
        if (global)
            return globalVariables.containsKey(ID);
        else
            return localVariables.containsKey(ID) || globalVariables.containsKey(ID);
    }

    public TypeInfo getFromScope(String ID) {
        if (global) {
            return globalVariables.get(ID);
        } else {
            if (localVariables.containsKey(ID))
                return localVariables.get(ID);
            else
                return globalVariables.get(ID);
        }
    }

	@Override
    public void exitProg(KoalaParser.ProgContext ctx) {
        System.out.println( LLVMGenerator.generate() );
    }

    //TODO: handle local and global scope
	@Override
    public void exitString(KoalaParser.StringContext ctx) {
        String val = ctx.STRING().getText();
        val = val.substring(1, val.length()-1);
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitString", val.getClass()), val));
            } catch (Exception ignored) {
            }
        } else {
            doExitString(val);
        }
    }

    public void doExitString(Object val) {
        LLVMGenerator.createStringConst((String) val);
        stack.push( new Value("@.str."+(LLVMGenerator.str_reg-1), VarType.STRING) );
    }

    //TODO: handle local and global scope
	@Override
    public void exitInt(KoalaParser.IntContext ctx) {
        String val = ctx.INT().getText();
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitInt", val.getClass()), val));
            } catch (Exception ignored) {
            }
        } else {
            doExitInt(val);
        }
    }

    public void doExitInt(Object val) {
        stack.push( new Value((String) val, VarType.INT) );
    }

    //TODO: handle local and global scope
	@Override
    public void exitReal(KoalaParser.RealContext ctx) {
        String val = ctx.REAL().getText();
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitInt", val.getClass()), val));
            } catch (Exception ignored) {
            }
        } else {
            doExitReal(val);
        }
    }

    public void doExitReal(Object val) {
        stack.push( new Value((String) val, VarType.REAL) );
    }

	@Override
    public void exitId(KoalaParser.IdContext ctx) {
        String ID = ctx.ID().getText();
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitId",
                                            ID.getClass(), line.getClass()), ID, line));
            } catch (Exception ignored) {
            }
        } else {
            doExitId(ID, line);
        }
    }

    @Override
    public void exitStrId(KoalaParser.IdContext ctx) {
        String ID = ctx.ID().getText();
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitId",
                        ID.getClass(), line.getClass()), ID, line));
            } catch (Exception ignored) {
            }
        } else {
            doExitId(ID, line);
        }
    }

    public void doExitId(String ID, String line) {
        if (existsInScope(ID)) {
            TypeInfo variable = getFromScope(ID);
            VarType type = variable.type;
            if (type == VarType.STRING) {
                stack.push(new Value("%" + (ID), VarType.STRING));
            }
            if(type == VarType.INT){
                LLVMGenerator.load_i32(ID);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
            }
            if(type == VarType.REAL){
                LLVMGenerator.load_double(ID);
                stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
            }
        } else {
            error(line, "unknown variable "+ID);
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitAdd(KoalaParser.AddContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitAdd",
                                            line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitAdd(line);
        }
    }

    public void doExitAdd(String line) {
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
            error(line, "type mismatch for operation '+'");
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitSub(KoalaParser.SubContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitSub",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitSub(line);
        }
    }

    public void doExitSub(String line) {
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
            error(line, "type mismatch for operation '-'");
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitMult(KoalaParser.MultContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitMult",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitMult(line);
        }
    }

    public void doExitMult(String line) {
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
            error(line, "type mismatch for operation '*'");
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitDiv(KoalaParser.DivContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitDiv",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitDiv(line);
        }
    }

    public void doExitDiv(String line) {
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
                error(line, "type mismatch for operation '/'");
            }

        } else {
            error(line, "division by zero");
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitTostring(KoalaParser.TostringContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitTostring")));
            } catch (Exception ignored) {
            }
        } else {
            doExitTostring();
        }
    }

    public void doExitTostring() {
        Value v = stack.pop();
        if (v.type == VarType.INT || v.type == VarType.REAL) {
            LLVMGenerator.tostring( v.name, v.type );
            stack.push( new Value("%_tmp."+(LLVMGenerator.tmp_reg-1), VarType.STRING) );
        } else {
            stack.push(v);
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitToint(KoalaParser.TointContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitToint")));
            } catch (Exception ignored) {
            }
        } else {
            doExitToint();
        }
    }

    //TODO: handle local and global scope
    @Override
    public void exitToint2(KoalaParser.Toint2Context ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitToint")));
            } catch (Exception ignored) {
            }
        } else {
            doExitToint();
        }
    }

    public void doExitToint() {
        Value v = stack.pop();
        if (v.type == VarType.REAL)
            LLVMGenerator.fptosi( v.name );
        else if (v.type == VarType.STRING)
            LLVMGenerator.atoi( v.name );
        stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.INT) );
    }

    //TODO: handle local and global scope
	@Override
    public void exitToreal(KoalaParser.TorealContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitToreal")));
            } catch (Exception ignored) {
            }
        } else {
            doExitToreal();
        }
    }

    //TODO: handle local and global scope
    @Override
    public void exitToreal2(KoalaParser.Toreal2Context ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitToreal")));
            } catch (Exception ignored) {
            }
        } else {
            doExitToreal();
        }
    }

    public void doExitToreal() {
        Value v = stack.pop();
        if (v.type == VarType.INT)
            LLVMGenerator.sitofp( v.name );
        else if (v.type == VarType.STRING)
            LLVMGenerator.atof( v.name );
        stack.push( new Value("%"+(LLVMGenerator.reg-1), VarType.REAL) );
    }

    //TODO: handle local and global scope
	@Override
    public void exitPrint(KoalaParser.PrintContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitPrint",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitPrint(line);
        }
    }

    public void doExitPrint(String line) {
        Value v = stack.pop();
        VarType type = v.type;
        if( type != null ) {
            if( type == VarType.STRING )
                LLVMGenerator.printf( v.name );
            else {
                error(line, "expected type: STRING, got "+type+" instead");
            }
        } else { // TODO: not needed
            error(line, "unknown variable "+v.name);
        }
    }

    //TODO: handle local and global scope
	@Override
    public void exitRead(KoalaParser.ReadContext ctx) {
        String ID = ctx.ID().getText();
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitRead",
                        ID.getClass()), ID));
            } catch (Exception ignored) {
            }
        } else {
            doExitRead(ID);
        }
    }

    public void doExitRead(String ID) {
        HashMap<String, TypeInfo> variables = global ? globalVariables : localVariables;
        if (!existsInScope(ID))
            variables.put(ID, new TypeInfo(VarType.STRING));
        TypeInfo variable = getFromScope(ID);
        if (!variable.stringDeclared) {
            LLVMGenerator.declare_string(ID);
            variable.stringDeclared = true;
        }
        LLVMGenerator.scanf(ID);
        variable.type = VarType.STRING;
    }

    int getTypeId(VarType t) {
        if (t == VarType.STRING)
            return 0;
        else
            return 1;
    }

    void error(String line, String msg){
        System.err.println("Error, line "+line+", "+msg);
        System.exit(1);
    }

}
