grammar Koala;

prog: block
;

block: (stat NEWLINE)*
;

blockfun: block
;

stat:     PRINT strexpr   	                                                                #print
        | ID '=' expr0		                                                                #assign
	    | READ ID		                                                                    #read
	    | FUNCTION funname '(' defparam (',' defparam)* ')' NEWLINE blockfun END NEWLINE    #fun
        | RET expr0                                                                         #ret
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
