import com.sun.jdi.IntegerValue;

import java.util.ArrayList;
import java.util.Stack;

class LLVMGenerator{
   static String str_const_text = "";
   static String header_text = "";
   static String main_text = "";
   static int str_reg = 1;
   static int tmp_reg = 1;
   static Stack<String> scopeBufferStack = new Stack<String>();
   static Stack<Integer> scopeRegisterStack = new Stack<Integer>();
   static Stack<VarType> returnTypeStack = new Stack<VarType>();

   static void functionstart(String id, ArrayList<VarType> callTypes){
      String buffer = "@"+id+"(";
      for (int i = 0; i < callTypes.size(); i++) {
         VarType type = callTypes.get(i);
         if (i > 0)
            buffer = buffer.concat(", ");

         if (type == VarType.INT)
            buffer = buffer.concat("i32");
         else if (type == VarType.REAL)
            buffer = buffer.concat("double");
         else if (type == VarType.STRING)
            buffer = buffer.concat("i8*");
      }
      buffer += ") nounwind {\n";
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(1 + callTypes.size());
      returnTypeStack.push(VarType.UNKNOWN);

      initializeLocalVariables();
   }

   static void initializeLocalVariables() {
      for (int i = 0; i < LLVMActions.calledFunction.fargNames.size(); i++) {
         String name = LLVMActions.calledFunction.fargNames.get(i);
         VarType type = LLVMActions.localVariables.get(name).type;
         if (type == VarType.INT) {
            declare_i32(name);
            String buffer = scopeBufferStack.pop();
            int reg = scopeRegisterStack.pop();
            //buffer += "%"+reg+" = load i32, i32* %"+(i+1)+"\n";
            //reg++;
            scopeBufferStack.push(buffer);
            scopeRegisterStack.push(reg);
            assign_i32(name, "%"+i);
         } else if (type == VarType.REAL) {
            declare_double(name);
            String buffer = scopeBufferStack.pop();
            int reg = scopeRegisterStack.pop();
            //buffer += "%"+reg+" = load double, double* %"+(i+1)+"\n";
            //reg++;;
            scopeBufferStack.push(buffer);
            scopeRegisterStack.push(reg);
            assign_double(name, "%"+i);
         } else if (type == VarType.STRING) {
            declare_string(name);
            String buffer = scopeBufferStack.pop();
            int reg = scopeRegisterStack.pop();
            //buffer += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* %"+(i+1)+", i32 0, i32 0\n";
            //reg++;
            scopeBufferStack.push(buffer);
            scopeRegisterStack.push(reg);
            assign_string(name, "%"+i);
         }
      }
   }

   static void functionend(){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      VarType retType = returnTypeStack.pop();

      if (retType == VarType.UNKNOWN) {
         buffer = "define i32 "+buffer;
         buffer += "ret i32 0\n";
         LLVMActions.returnedType = VarType.INT;
      } else if (retType == VarType.INT) {
         buffer = "define i32 "+buffer;
         buffer += "ret i32 0\n";
         LLVMActions.returnedType = VarType.INT;
      } else if (retType == VarType.REAL) {
         buffer = "define double "+buffer;
         buffer += "ret double 0.0\n";
         LLVMActions.returnedType = VarType.REAL;
      } else if (retType == VarType.STRING) {
         buffer = "define i8* "+buffer;
         scopeBufferStack.push(buffer);
         scopeRegisterStack.push(reg);
         createStringConst("");
         buffer = scopeBufferStack.pop();
         reg = scopeRegisterStack.pop();
         buffer += "ret i8* %"+(reg-1)+"\n";
         LLVMActions.returnedType = VarType.STRING;
      }

      buffer += "}\n\n";
      main_text += buffer;
   }

   static void call(String ID, ArrayList<Value> arguments, ArrayList<VarType> types, VarType retType){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      ArrayList<String> stringArgs = new ArrayList<String>();

      String typeStr = "";
      if (retType == VarType.INT)
         typeStr = "i32";
      else if (retType == VarType.REAL)
         typeStr = "double";
      else if (retType == VarType.STRING)
         typeStr = "i8*";

      buffer += "%"+reg+" = call "+typeStr+" @"+ID+"(";
      for (int i = 0; i < arguments.size(); i++) {
         VarType type = types.get(i);
         if (i > 0)
            buffer = buffer.concat(", ");

         if (type == VarType.INT) {
            buffer = buffer.concat("i32 " + arguments.get(i).name);
         } else if (type == VarType.REAL) {
            buffer = buffer.concat("double " + arguments.get(i).name);
         } else if (type == VarType.STRING) {
            buffer = buffer.concat("i8* " + arguments.get(i).name);
         }
      }
      buffer += ")\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void ret(Value v){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      VarType retType = returnTypeStack.pop();

      if (v.type == VarType.INT) {
         buffer += "ret i32 " + v.name + "\n";
         reg++;
         retType = VarType.INT;
      } else if (v.type == VarType.REAL) {
         buffer += "ret double " + v.name + "\n";
         reg++;
         retType = VarType.REAL;
      } else if (v.type == VarType.STRING) {
         buffer += "ret i8* " + v.name + "\n";
         reg++;
         retType = VarType.STRING;
      }

      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
      returnTypeStack.push(retType);
   }

   static void printf(String value){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i8* "+value+")\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void declare_string(String id){
      String prefix = LLVMActions.global ? "@" : "%";
      if (prefix.equals("@")) {
         str_const_text += prefix+id+".0 = global [10000 x i8] c\"";
         for (int i = 0; i < 10000; i++)
            str_const_text = str_const_text.concat("\\00");
         str_const_text += "\"\n";
         str_const_text += prefix+id+".len = global i32 0\n";
      } else {
         String buffer = scopeBufferStack.pop();
         buffer += prefix + id + ".0 = alloca [10000 x i8]\n";
         buffer += prefix + id + ".len = alloca i32\n";
         scopeBufferStack.push(buffer);
      }
   }

   static void declare_i32(String id){
      String prefix = LLVMActions.global ? "@" : "%";
      if (prefix.equals("@")) {
         header_text += prefix + id + ".1 = global i32 0\n";
      } else {
         String buffer = scopeBufferStack.pop();
         buffer += prefix + id + ".1 = alloca i32\n";
         scopeBufferStack.push(buffer);
      }
   }

   static void declare_double(String id){
      String prefix = LLVMActions.global ? "@" : "%";
      if (prefix.equals("@")) {
         header_text += prefix + id + ".1 = global double 0.0\n";
      } else {
         String buffer = scopeBufferStack.pop();
         buffer += prefix + id + ".1 = alloca double\n";
         scopeBufferStack.push(buffer);
      }
   }

   static void assign_string(String id, String value) {
      String prefix = LLVMActions.global ? "@" : "%";
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+prefix+id+".0, i32 0, i32 0\n";
      reg++;
      buffer += "%"+reg+" = call i8* @strncpy(i8* %"+(reg-1)+", i8* "+value+", i64 10000)\n";
      reg++;
      buffer += "store i32 0, i32* "+prefix+id+".len\n";
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void assign_i32(String id, String value){
      String prefix = LLVMActions.global ? "@" : "%";
      String buffer = scopeBufferStack.pop();
      if (prefix.equals("@")) {
         buffer += "store i32 " + value + ", i32* " + prefix + id + ".1\n";
      } else {
         buffer += "store i32 " + value + ", i32* " + prefix + id + ".1\n";
      }
      scopeBufferStack.push(buffer);
   }

   static void assign_double(String id, String value){
      String prefix = LLVMActions.global ? "@" : "%";
      String buffer = scopeBufferStack.pop();
      if (prefix.equals("@")) {
         buffer += "store double " + value + ", double* " + prefix + id + ".1\n";
      } else {
         buffer += "store double " + value + ", double* " + prefix + id + ".1\n";
      }
      scopeBufferStack.push(buffer);
   }

   static void getelementptr(String id, String prefix){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+prefix+id+".0, i32 0, i32 0\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void load_i32(String id, String prefix){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = load i32, i32* "+prefix+id+".1\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void load_double(String id, String prefix){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = load double, double* "+prefix+id+".1\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   /* TODO
   static void add_string(String val1, String val2){
      main_text += "%"+reg+" = add i32 "+val1+", "+val2+"\n";
      reg++;
   }
   */

   static void add_i32(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = add i32 "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void add_double(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = fadd double "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void sub_i32(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = sub i32 "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void sub_double(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = fsub double "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void mult_i32(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = mul i32 "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void mult_double(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = fmul double "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void div_i32(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = sdiv i32 "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void div_double(String val1, String val2){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = fdiv double "+val1+", "+val2+"\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void atoi(String value){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = call i32 @atoi(i8* %"+(reg-1)+")\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void atof(String value){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = call double @atof(i8* %"+(reg-1)+")\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void tostring(String val, VarType type){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%_tmp."+tmp_reg+".0 = alloca [10000 x i8]\n";
      buffer += "%_tmp."+tmp_reg+".len = alloca i32\n";
      buffer += "store i32 0, i32* %_tmp."+tmp_reg+".len\n";
      tmp_reg++;
      buffer += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* %_tmp."+(tmp_reg-1)+".0, i32 0, i32 0\n";
      reg++;
      if (type == VarType.INT) {
         buffer += "%"+reg+" = call i32 (i8*, i8*, ...) @sprintf(i8* %"+(reg-1)+", i8* getelementptr inbounds " +
                 "([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32 "+val+")\n";
         reg++;
      } else if (type == VarType.REAL) {
         buffer += "%"+reg+" = call i32 (i8*, i8*, ...) @sprintf(i8* %"+(reg-1)+", i8* getelementptr inbounds " +
                 "([3 x i8], [3 x i8]* @strs3, i32 0, i32 0), double "+val+")\n";
         reg++;
      }
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void sitofp(String id){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = sitofp i32 "+id+" to double\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void fptosi(String id){
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = fptosi double "+id+" to i32\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void scanf(String id) {
      String prefix = LLVMActions.global ? "@" : "%";
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      buffer += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+prefix+id+".0, i32 0, i32 0\n";
      reg++;
      buffer += "%"+reg+" = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs2, i32 0, i32 0), i8* %"+(reg-1)+")\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static void createStringConst(String value) {
      String buffer = scopeBufferStack.pop();
      int reg = scopeRegisterStack.pop();
      str_const_text += "@.str."+str_reg+".0 = private unnamed_addr constant [10000 x i8] c\""+value+"\\00";
      for (int i = value.length()+1; i < 10000; i++)
         str_const_text = str_const_text.concat("\\00");
      str_const_text += "\"\n";
      str_const_text += "@.str."+str_reg+".len = private unnamed_addr constant i32 "+value.length()+"\n";
      str_reg++;
      buffer += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* @.str."+(str_reg-1)+".0, i32 0, i32 0\n";
      reg++;
      scopeBufferStack.push(buffer);
      scopeRegisterStack.push(reg);
   }

   static String generate(){
      String text = "";
      text += "declare i32 @printf(i8*, ...)\n";
      text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
      text += "; Function Attrs: nounwind\n";
      text += "declare i8* @strncpy(i8*, i8*, i64)\n";
      text += "; Function Attrs: nounwind\n";
      text += "declare i32 @sprintf(i8*, i8*, ...)\n";
      text += "; Function Attrs: nounwind readonly\n";
      text += "declare double @atof(i8*)\n";
      text += "; Function Attrs: nounwind readonly\n";
      text += "declare i32 @atoi(i8*)\n";

      text += "@strpi = constant [4 x i8] c\"%d\\0A\\00\"\n";
      text += "@strpd = constant [4 x i8] c\"%f\\0A\\00\"\n";
      text += "@strp = constant [4 x i8] c\"%s\\0A\\00\"\n";
      text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
      text += "@strs2 = constant [3 x i8] c\"%s\\00\"\n";
      text += "@strs3 = constant [3 x i8] c\"%f\\00\"\n";

      text += str_const_text + "\n";
      text += header_text + "\n";
      text += main_text + "\n";
      text += "define i32 @main() nounwind{\n";
      text += scopeBufferStack.pop();
      text += "ret i32 0 }\n";
      return text;
   }

}
