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

blockwhile: block
;

blockfun: block
;

stat:     PRINT strexpr   	                                                                #print
        | ID '=' expr0		                                                                #assign
	    | READ ID		                                                                    #read
	    | FUNCTION funname '(' defparam (',' defparam)* ')' NEWLINE blockfun END NEWLINE    #fun
	    | IF '(' cond ')' NEWLINE blockifelse ELSE NEWLINE blockelse END                    #ifelse
        | IF '('cond ')' NEWLINE blockif END                                                #if
        | WHILE '(' whilecond ')' NEWLINE blockwhile END                                    #whileloop
        | RET expr0                                                                         #ret
;

whilecond: cond
;

cond: expr0 '==' expr0      #equal
    | expr0 '>' expr0       #more
    | expr0 '<' expr0       #less
    | expr0 '!=' expr0      #notequal
    | expr0 '<=' expr0      #lessequal
    | expr0 '>=' expr0      #moreequal
;

expr0:    expr1			                        #single0
        | expr0 ADD expr0	                    #add
	    | expr0 SUB expr0	                    #sub
	    | strexpr		                        #strexp
	    | ID '(' (fparam (',' fparam)*)? ')'    #call
;

expr1:  expr2			    #single1
      | expr1 DIV expr1		#div
      | expr1 MULT expr1	#mult
;

expr2:   INT			    #int
       | REAL			    #real
       | ID			        #id
       | TOINT expr2		#toint
       | TOINT strexpr		#toint2
       | TOREAL expr2		#toreal
       | TOREAL strexpr		#toreal2
       | '(' expr0 ')'		#par
;

strexpr:  STRING                #string
	    | ID			        #strid
	    | TOSTRING expr2		#tostring
        | '(' strexpr ')'	    #strpar
        |  strexpr ADD strexpr	#stradd
    ;

funname: ID
;

defparam: ID
;

fparam: expr0
;

IF: 'if'
;

ELSE: 'else'
;

WHILE: 'while'
;

FUNCTION: 'function'
;

END: 'end'
;

RET: 'return'
;

PRINT: 'print'
;

READ: 'read'
;

TOINT: '(int)'
;

TOREAL: '(real)'
;

TOSTRING: '(str)'
;

ID: ('a'..'z'|'A'..'Z')+
;

REAL: '0'..'9'+'.''0'..'9'+
;

STRING: '"' ( ~('\\'|'"') )* '"'
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
	
NEWLINE: '\r'? '\n'
;

WS: (' '|'\t')+ { skip(); }
;
