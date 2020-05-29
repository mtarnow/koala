
grammar Koala;

prog: block
;

block: (stat NEWLINE)*
;

blockif: block
;

blockifelse: block
;

blockelse: block
;

blockfun: block
;
blockwhile: block
;

stat: IF '(' cond ')' NEWLINE blockifelse ELSE NEWLINE blockelse END #ifelse
    | IF '('cond ')' NEWLINE blockif END                 #if
    | WHILE '(' whilecond ')' NEWLINE blockwhile END          #whileloop
    | FUNCTION funname '(' ID (',' ID)* ')' blockfun END #fun
    | PRINT strexpr   	                                 #print
	| ID '=' expr0		                                 #assign
	| READ ID		                                     #read
    | RET expr0                                          #ret
;
IF: 'if'
;
END: 'end';

WHILE: 'while';

whilecond: cond;

cond: expr0 '==' expr0      #equal
    | expr0 '>' expr0       #more
    | expr0 '<' expr0       #less
    | expr0 '!=' expr0      #notequal
    | expr0 '<=' expr0      #lessequal
    | expr0 '>=' expr0      #moreequal
;

expr0:  expr1			#single0
    | expr0 ADD expr0	#add
	| expr0 SUB expr0	#sub
	| strexpr		#strexp
;

expr1:  expr2			#single1
      | expr1 DIV expr1		#div
      | expr1 MULT expr1	#mult
;

expr2:   INT			#int
       | REAL			#real
       | ID			#id
       | TOINT expr2		#toint
       | TOINT strexpr		#toint2
       | TOREAL expr2		#toreal
       | TOREAL strexpr		#toreal2
       | '(' expr0 ')'		#par
;

strexpr: STRING                 #string
	|ID			#strid
	|TOSTRING expr2		#tostring
        |'(' strexpr ')'	#strpar
        | strexpr ADD strexpr	#stradd
    ;

ELSE: 'else'
;

PRINT:	'print' 
    ;

READ:   'read'
    ;

TOINT: '(int)'
    ;

TOREAL: '(real)'
    ;

TOSTRING: '(str)'
    ;



ID:   ('a'..'z'|'A'..'Z')+
   ;

REAL: '0'..'9'+'.''0'..'9'+
    ;

STRING:  '"' ( ~('\\'|'"') )* '"'
    ;

INT: '0'..'9'+
    ;

ADD: '+'
    ;

SUB: '-'
    ;

MULT: '*'
    ;
	
DIV: '/'
    ;
	
NEWLINE:	'\r'? '\n'
    ;

WS:   (' '|'\t')+ { skip(); }
    ;

RET: 'return'
;

funname: ID
;



FUNCTION: 'function'
;


fparam: expr0
;

strcomp: '=='	#strequal
	|'!='	#strnotequal
;
