import java.util.Stack;

class LLVMGenerator{

   static String header_text = "";
   static String main_text = "";
   static int reg = 1;
   static int br = 1;
   static int str_reg = 1;
   static int tmp_reg = 1;

   static Stack<Integer> brstack = new Stack<Integer>();

   static void printf(String value){
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+value+".0, i32 0, i32 0\n";
      reg++;
      main_text += "%"+reg+" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i8* %"+(reg-1)+")\n";
      reg++;
   }

   static void declare_string(String id){
      main_text += "%"+id+".0 = alloca [10000 x i8]\n";
      main_text += "%"+id+".len = alloca i32\n";
   }

   static void declare_i32(String id){
      main_text += "%"+id+".1 = alloca i32\n";
   }

   static void declare_double(String id){
      main_text += "%"+id+".1 = alloca double\n";
   }

   static void assign_string(String id, String value) {
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* %"+id+".0, i32 0, i32 0\n";
      reg++;
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+value+".0, i32 0, i32 0\n";
      reg++;
      main_text += "%"+reg+" = call i8* @strncpy(i8* %"+(reg-2)+", i8* %"+(reg-1)+", i64 10000)\n";
      reg++;
      main_text += "%"+reg+" = load i32, i32* "+value+".len\n";
      reg++;
      main_text += "store i32 "+(reg-1)+", i32* %"+id+".len\n";
   }

   static void assign_i32(String id, String value){
      main_text += "store i32 "+value+", i32* %"+id+".1\n";
   }

   static void assign_double(String id, String value){
      main_text += "store double "+value+", double* %"+id+".1\n";
   }

   /*
   static void getelementptr(String id){
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* %"+id+", i32 0, i32 0\n";
      reg++;
   }
   */

   static void load_i32(String id){
      main_text += "%"+reg+" = load i32, i32* %"+id+".1\n";
      reg++;
   }

   static void load_double(String id){
      main_text += "%"+reg+" = load double, double* %"+id+".1\n";
      reg++;
   }

   /* TODO
   static void add_string(String val1, String val2){
      main_text += "%"+reg+" = add i32 "+val1+", "+val2+"\n";
      reg++;
   }
   */

   static void add_i32(String val1, String val2){
      main_text += "%"+reg+" = add i32 "+val1+", "+val2+"\n";
      reg++;
   }

   static void add_double(String val1, String val2){
      main_text += "%"+reg+" = fadd double "+val1+", "+val2+"\n";
      reg++;
   }

   static void sub_i32(String val1, String val2){
      main_text += "%"+reg+" = sub i32 "+val1+", "+val2+"\n";
      reg++;
   }

   static void sub_double(String val1, String val2){
      main_text += "%"+reg+" = fsub double "+val1+", "+val2+"\n";
      reg++;
   }

   static void mult_i32(String val1, String val2){
      main_text += "%"+reg+" = mul i32 "+val1+", "+val2+"\n";
      reg++;
   }

   static void mult_double(String val1, String val2){
      main_text += "%"+reg+" = fmul double "+val1+", "+val2+"\n";
      reg++;
   }

   static void div_i32(String val1, String val2){
      main_text += "%"+reg+" = sdiv i32 "+val1+", "+val2+"\n";
      reg++;
   }

   static void div_double(String val1, String val2){
      main_text += "%"+reg+" = fdiv double "+val1+", "+val2+"\n";
      reg++;
   }

   static void atoi(String value){
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+value+".0, i32 0, i32 0\n";
      reg++;
      main_text += "%"+reg+" = call i32 @atoi(i8* %"+(reg-1)+")\n";
      reg++;
   }

   static void atof(String value){
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* "+value+".0, i32 0, i32 0\n";
      reg++;
      main_text += "%"+reg+" = call double @atof(i8* %"+(reg-1)+")\n";
      reg++;
   }

   static void tostring(String val, VarType type){
      main_text += "%_tmp."+tmp_reg+".0 = alloca [10000 x i8]\n";
      main_text += "%_tmp."+tmp_reg+".len = alloca i32\n";
      main_text += "store i32 0, i32* %_tmp."+tmp_reg+".len\n";
      tmp_reg++;
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* %_tmp."+(tmp_reg-1)+".0, i32 0, i32 0\n";
      reg++;
      if (type == VarType.INT) {
         main_text += "%"+reg+" = call i32 (i8*, i8*, ...) @sprintf(i8* %"+(reg-1)+", i8* getelementptr inbounds " +
                 "([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32 "+val+")\n";
         reg++;
      } else if (type == VarType.REAL) {
         main_text += "%"+reg+" = call i32 (i8*, i8*, ...) @sprintf(i8* %"+(reg-1)+", i8* getelementptr inbounds " +
                 "([3 x i8], [3 x i8]* @strs3, i32 0, i32 0), double "+val+")\n";
         reg++;
      }
   }

   static void sitofp(String id){
      main_text += "%"+reg+" = sitofp i32 "+id+" to double\n";
      reg++;
   }

   static void fptosi(String id){
      main_text += "%"+reg+" = fptosi double "+id+" to i32\n";
      reg++;
   }

   static void scanf(String id) {
      main_text += "%"+reg+" = getelementptr inbounds [10000 x i8], [10000 x i8]* %"+id+".0, i32 0, i32 0\n";
      reg++;
      main_text += "%"+reg+" = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs2, i32 0, i32 0), i8* %"+(reg-1)+")\n";
      reg++;
   }

   static void createStringConst(String value) {
      header_text += "@.str."+str_reg+".0 = private unnamed_addr constant [10000 x i8] c\""+value+"\\00";
      for (int i = value.length()+1; i < 10000; i++)
         header_text = header_text.concat("\\00");
      header_text += "\"\n";
      header_text += "@.str."+str_reg+".len = private unnamed_addr constant i32 "+value.length()+"\n";
      str_reg++;
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
      text += header_text + "\n";
      text += "define i32 @main() nounwind{\n";
      text += main_text;
      text += "ret i32 0 }\n";
      return text;
   }
   static void ifstart(){
      br++;
      main_text += "br i1 %"+(reg-1)+", label %true"+br+", label %false"+br+"\n";
      main_text += "true"+br+":\n";
      brstack.push(br);
   }

   static void ifend(){
      int b = brstack.pop();
      main_text += "br label %false"+b+"\n";
      main_text += "false"+b+":\n";
   }
   static void oeq(String value, String value2){
      main_text += "%"+reg+" = fcmp oeq double "+value+", "+value2+"\n";
      reg++;
   }
   static void eq(String value, String value2){
      main_text += "%"+reg+" = icmp eq i32 "+value+", "+value2+"\n";
      reg++;
   }
   static void oge(String value, String value2){
      main_text += "%"+reg+" = fcmp oge double "+value+", "+value2+"\n";
      reg++;
   }
   static void sge(String value, String value2){
      main_text += "%"+reg+" = icmp sge i32 "+value+", "+value2+"\n";
      reg++;
   }
   static void ole(String value, String value2){
      main_text += "%"+reg+" = fcmp sge double "+value+", "+value2+"\n";
      reg++;
   }
   static void sle(String value, String value2){
      main_text += "%"+reg+" = icmp sle i32 "+value+", "+value2+"\n";
      reg++;
   }

   static void une(String value, String value2){
      main_text += "%"+reg+" = fcmp une double "+value+", "+value2+"\n";
      reg++;
   }
   static void ne(String value, String value2){
      main_text += "%"+reg+" = icmp ne i32 "+value+", "+value2+"\n";
      reg++;
   }

   static void olt(String value, String value2){
      main_text += "%"+reg+" = fcmp olt double "+value+", "+value2+"\n";
      reg++;
   }
   static void slt(String value, String value2){
      main_text += "%"+reg+" = icmp slt i32 "+value+", "+value2+"\n";
      reg++;
   }

   static void ogt(String value, String value2){
      main_text += "%"+reg+" = fcmp ogt double "+value+", "+value2+"\n";
      reg++;
   }
   static void sgt(String value, String value2){
      main_text += "%"+reg+" = icmp sgt i32 "+value+", "+value2+"\n";
      reg++;
   }


}

