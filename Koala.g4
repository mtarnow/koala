grammar Koala;

prog: ( stat? NEWLINE )* 
;

stat:     PRINT strexpr   	#print
        | ID '=' expr0		#assign
	    | READ ID		    #read
;

expr0:    expr1			    #single0
        | expr0 ADD expr0	#add
	    | expr0 SUB expr0	#sub
	    | strexpr		    #strexp
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
