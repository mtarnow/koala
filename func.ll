@z = global i32 4, align 4
@.str = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1

; Function Attrs: noinline nounwind optnone uwtable
define i32 @func(i32) {
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  %4 = load i32, i32* %3, align 4
  %5 = icmp eq i32 %4, 0
  br i1 %5, label %6, label %8

; <label>:6:
  store i32 10, i32* %2, align 4
  %7 = load i32, i32* %2, align 4
  ret i32 %7

; <label>:8:
  store i32 11, i32* %2, align 4
  %9 = load i32, i32* %2, align 4
  ret i32 %9

; <label>:10:
  ret i32 0
}

; Function Attrs: noinline nounwind optnone uwtable
define i32 @main() {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  %3 = load i32, i32* %1, align 4
  %4 = call i32 @func(i32 %3)
  store i32 %4, i32* %2, align 4
  store i32 5, i32* @z, align 4
  %5 = load i32, i32* %2, align 4
  %6 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str, i32 0, i32 0), i32 %5)
  ret i32 0
}

declare i32 @printf(i8*, ...)
