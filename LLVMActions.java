
import java.lang.reflect.Type;
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
    public ArrayList<VarType> retTypes = new ArrayList<VarType>();
}

public class LLVMActions extends KoalaBaseListener {

    boolean returns = false;

    // function definition context
    Boolean functionDefContext = false;
    String functionName;
    FunctionInfo functionInfo;

    // call context
    static Boolean global = true;
    static VarType returnedType;
    static FunctionInfo calledFunction;
    Stack<ArrayList<VarType>> callTypeStack = new Stack<ArrayList<VarType>>();
    ArrayList<VarType> callTypes = new ArrayList<VarType>();
    Stack<ArrayList<Value>> argumentValueStack = new Stack<ArrayList<Value>>();

    HashMap<String, TypeInfo> globalVariables = new HashMap<String, TypeInfo>();
    Stack<HashMap<String, TypeInfo>> localVariableStack = new Stack<HashMap<String, TypeInfo>>();
    static HashMap<String, TypeInfo> localVariables = new HashMap<String, TypeInfo>();
    HashMap<String, FunctionInfo> functions = new HashMap<String, FunctionInfo>();
    Stack<Value> stack = new Stack<Value>();

    @Override
    public void enterProg(KoalaParser.ProgContext ctx) {
        // initialize global buffer and register
        LLVMGenerator.scopeBufferStack.push("");
        LLVMGenerator.scopeRegisterStack.push(1);
        LLVMGenerator.scopeBrStack.push(1);
        LLVMGenerator.scopeBrstackStack.push(new Stack<Integer>());
    }

    @Override
    public void exitProg(KoalaParser.ProgContext ctx) {
        System.out.println( LLVMGenerator.generate() );
    }

    @Override public void enterBlockif(KoalaParser.BlockifContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterBlockif")));
            } catch (Exception ignored) {
            }
        } else {
            doEnterBlockif();
        }
    }

    public void doEnterBlockif() {
        LLVMGenerator.ifstart();
    }

    @Override
    public void exitBlockif(KoalaParser.BlockifContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitBlockif")));
            } catch (Exception ignored) {
            }
        } else {
            doExitBlockif();
        }
    }

    public void doExitBlockif() {
        LLVMGenerator.ifend();
    }

    @Override public void enterBlockifelse(KoalaParser.BlockifelseContext ctx){
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterBlockifelse")));
            } catch (Exception ignored) {
            }
        } else {
            doEnterBlockifelse();
        }
    }

    public void doEnterBlockifelse() {
        LLVMGenerator.ifstart();
    }

    @Override
    public void enterBlockelse(KoalaParser.BlockelseContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterBlockelse")));
            } catch (Exception ignored) {
            }
        } else {
            doEnterBlockelse();
        }
    }

    public void doEnterBlockelse() {
        LLVMGenerator.elsestart();
    }

    @Override
    public void exitBlockelse(KoalaParser.BlockelseContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitBlockelse")));
            } catch (Exception ignored) {
            }
        } else {
            doExitBlockelse();
        }
    }

    public void doExitBlockelse() {
        LLVMGenerator.elseend();
    }
    //WHILE
    @Override
    public void enterWhileloop(KoalaParser.WhileloopContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterWhileloop")));
            } catch (Exception ignored) {
            }
        } else {
            doEnterWhileloop();
        }
    }

    public void doEnterWhileloop() {
        LLVMGenerator.enterwhile();
    }
    @Override
    public void enterWhilecond(KoalaParser.WhilecondContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterWhilecond")));
            } catch (Exception ignored) {
            }
        } else {
            doEnterWhilecond();
        }
    }

    public void doEnterWhilecond() {
        LLVMGenerator.exitwhile();
    }
    @Override
    public void enterBlockwhile(KoalaParser.BlockwhileContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doEnterBlockwhile")));
            } catch (Exception ignored) {
            }
        } else {
            doEnterBlockwhile();
        }
    }

    public void doEnterBlockwhile() {
        LLVMGenerator.enterblockwhile();
    }
    @Override
    public void exitBlockwhile(KoalaParser.BlockwhileContext ctx) {
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitBlockwhile")));
            } catch (Exception ignored) {
            }
        } else {
            doExitBlockwhile();
        }
    }

    public void doExitBlockwhile() {
        LLVMGenerator.exitblockwhile();
    }


    @Override
    public void exitEqual(KoalaParser.EqualContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitEqual",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitEqual(line);
        }
    }

    public void doExitEqual(String line) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.eq(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.oeq(v2.name, v1.name);
            //TODO: Compare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(line, "koala doesn't know how compare Strings :C");
        } else {
            error(line, "type mismatch");
        }
    }

    @Override
    public void exitMore(KoalaParser.MoreContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitMore",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitMore(line);
        }
    }

    public void doExitMore(String line) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.sgt(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.ogt(v2.name, v1.name);
            //TODO: Compare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(line, "koala doesn't know how compare Strings :C");
        } else {
            error(line, "type mismatch");
        }
    }

    @Override
    public void exitLess(KoalaParser.LessContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitLess",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitLess(line);
        }
    }

    public void doExitLess(String line) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.slt(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.olt(v2.name, v1.name);
            //TODO: Compare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(line, "koala doesn't know how compare Strings :C");
        } else {
            error(line, " type mismatch");
        }
    }

    @Override
    public void exitNotequal(KoalaParser.NotequalContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitNotequal",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitNotequal(line);
        }
    }

    public void doExitNotequal(String line) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.ne(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.une(v2.name, v1.name);
            //TODO: Compare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(line, "koala doesn't know how compare Strings :C");
        } else {
            error(line, "type mismatch");
        }
    }

    @Override
    public void exitLessequal(KoalaParser.LessequalContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitLessequal",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitLessequal(line);
        }
    }

    public void doExitLessequal(String line) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.sle(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.ole(v2.name, v1.name);
            //TODO: Compare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(line, "koala doesn't know how compare Strings :C");
        } else {
            error(line, "type mismatch");
        }
    }

    @Override
    public void exitMoreequal(KoalaParser.MoreequalContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitMoreequal",
                        line.getClass()), line));
            } catch (Exception ignored) {
            }
        } else {
            doExitMoreequal(line);
        }
    }

    public void doExitMoreequal(String line) {
        Value v1 = stack.pop();
        Value v2 = stack.pop();
        if( v1.type == v2.type ) {
            if( v1.type == VarType.INT )
                LLVMGenerator.sge(v2.name, v1.name);
            if( v1.type == VarType.REAL )
                LLVMGenerator.oge(v2.name, v1.name);
            //TODO: Compare String use strncmp function call i32 @strncmp(i8* %6, i8* %7, i64 2) #4
            if (v1.type == VarType.STRING)
                error(line, "koala doesn't know how compare Strings :C");
        } else {
            error(line, "type mismatch");
        }
    }

	@Override
    public void exitFunname(KoalaParser.FunnameContext ctx) {
        if (!functionDefContext) {
            String ID = ctx.ID().getText();
            if (!functions.containsKey(ID)) {
                functionName = ID;
                functionInfo = new FunctionInfo();
                functionDefContext = true;
            } else {
                error(Integer.toString(ctx.getStart().getLine()), "function "+ID+" already defined");
            }
        } else {
            error(Integer.toString(ctx.getStart().getLine()), "can't define function in local scope");
        }
    }

	@Override
    public void exitDefparam(KoalaParser.DefparamContext ctx) {
        String ID = ctx.ID().getText();
        if (!functionInfo.fargNames.contains(ID))
            functionInfo.fargNames.add(ID);
        else
            error(Integer.toString(ctx.getStart().getLine()), "argument names must be unique");
    }

	@Override
    public void enterBlockfun(KoalaParser.BlockfunContext ctx) {
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
    public void exitBlockfun(KoalaParser.BlockfunContext ctx) {
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
    public void enterCall(KoalaParser.CallContext ctx) {
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
    public void exitCall(KoalaParser.CallContext ctx) {
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
            if (callTypes.size() == calledFunction.fargNames.size()) {
                if (!calledFunction.calledWithTypes.contains(callTypes)) {
                    localVariables = new HashMap<String, TypeInfo>();
                    localVariableStack.push(localVariables);
                    for (int i = 0; i < argumentValues.size(); i++) {
                        VarType type = callTypes.get(i);
                        localVariables.put(calledFunction.fargNames.get(i), new TypeInfo(type));
                        if (type == VarType.INT || type == VarType.REAL)
                            localVariables.get(calledFunction.fargNames.get(i)).numberDeclared = true;
                        else if (type == VarType.STRING)
                            localVariables.get(calledFunction.fargNames.get(i)).stringDeclared = true;
                    }
                    calledFunction.calledWithTypes.add(callTypes);
                    for (FunctionStatement stm : calledFunction.statements) {
                        try {
                            if (stm.args.size() == 0)
                                stm.method.invoke(this);
                            else if (stm.args.size() == 1)
                                stm.method.invoke(this, stm.args.get(0));
                            else
                                stm.method.invoke(this, stm.args.get(0), stm.args.get(1));
                        } catch (Exception ignored) {}
                    }
                    calledFunction.retTypes.add(returnedType);
                }
                int i = calledFunction.calledWithTypes.indexOf(callTypes);
                VarType retType = calledFunction.retTypes.get(i);
                LLVMGenerator.call(ID, argumentValues, callTypes, retType);
                stack.push(new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), retType));
            } else {
                error(line, "number of arguments for function "+ID+" doesn't match the definition");
            }
        } else {
            error(line, "undefined function "+ID);
        }
        
    }

	@Override
    public void exitFparam(KoalaParser.FparamContext ctx) {
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

	@Override
    public void exitRet(KoalaParser.RetContext ctx) {
        String line = Integer.toString(ctx.getStart().getLine());
	    if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitRet",
                                            line.getClass()), line));
            } catch (Exception ignored) {}
        } else {
            doExitRet(line);
        }
    }

    public void doExitRet(String line) {
        Value v = stack.pop();
        VarType retType = LLVMGenerator.returnTypeStack.peek();
        if (retType == VarType.UNKNOWN || v.type == retType) {
            LLVMGenerator.ret(v);
        } else {
            error(line, "all return statements must return a value of the same type");
        }
    }

	@Override
    public void exitAssign(KoalaParser.AssignContext ctx) {
        String ID = ctx.ID().getText();
        if (functionDefContext) {
            try {
                functionInfo.statements.add(new FunctionStatement(this.getClass().getMethod("doExitAssign", ID.getClass()), ID));
            } catch (Exception ignored) {}
        } else {
            doExitAssign(ID);
        }
    }

    public void doExitAssign(String ID) {
        HashMap<String, TypeInfo> variables = global ? globalVariables : localVariables;
        Value v = stack.pop();
        if (!variables.containsKey(ID))
            variables.put(ID, new TypeInfo(v.type));
        TypeInfo variable = variables.get(ID);

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

    public boolean inLocalScope(String ID) {
        if (global) {
            return false;
        } else {
            return localVariables.containsKey(ID);
        }
    }

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
            doExitString(val);
        }
    }

    public void doExitString(String val) {
        LLVMGenerator.createStringConst(val);
        stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.STRING) );
    }

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

    public void doExitInt(String val) {
        stack.push( new Value(val, VarType.INT) );
    }

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

    public void doExitReal(String val) {
        stack.push( new Value(val, VarType.REAL) );
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
    public void exitStrid(KoalaParser.StridContext ctx) {
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
            boolean inLocal = inLocalScope(ID);
            String prefix = inLocal ? "%" : "@";
            VarType type = variable.type;
            if (type == VarType.STRING) {
                LLVMGenerator.getelementptr(ID, prefix);
                stack.push(new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.STRING));
            }
            if(type == VarType.INT){
                LLVMGenerator.load_i32(ID, prefix);
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
            }
            if(type == VarType.REAL){
                LLVMGenerator.load_double(ID, prefix);
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.REAL) );
            }
        } else {
            error(line, "unknown variable "+ID);
        }
    }

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
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
            }*/
            if( v1.type == VarType.INT ){
                LLVMGenerator.add_i32(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
            }
            if( v1.type == VarType.REAL ){
                LLVMGenerator.add_double(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.REAL) );
            }
        } else {
            error(line, "type mismatch for operation '+'");
        }
    }

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
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
            }
            if( v1.type == VarType.REAL ){
                LLVMGenerator.sub_double(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.REAL) );
            }
        } else {
            error(line, "type mismatch for operation '-'");
        }
    }

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
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
            }
            if( v1.type == VarType.REAL ){
                LLVMGenerator.mult_double(v1.name, v2.name);
                stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.REAL) );
            }
        } else {
            error(line, "type mismatch for operation '*'");
        }
    }

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
                    stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
                }
                if( v1.type == VarType.REAL ){
                    LLVMGenerator.div_double(v1.name, v2.name);
                    stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.REAL) );
                }
            } else {
                error(line, "type mismatch for operation '/'");
            }

        } else {
            error(line, "division by zero");
        }
    }

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
            stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-2), VarType.STRING) );
        } else {
            stack.push(v);
        }
    }

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
        stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.INT) );
    }

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
        stack.push( new Value("%"+(LLVMGenerator.scopeRegisterStack.peek()-1), VarType.REAL) );
    }

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
        } else {
            error(line, "undefined variable "+v.name);
        }
    }

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
