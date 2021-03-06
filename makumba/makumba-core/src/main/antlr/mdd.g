header {
    package org.makumba.providers.datadefinition.mdd;
}


{@SuppressWarnings("all")} class MDDLexer extends Lexer;

options {
    exportVocab=MDD;
    k = 4;
}
	    
LEFT_PAREN options { paraphrase = "'('";}: '(';
RIGHT_PAREN options { paraphrase = "')'";}: ')';
LEFT_CUBR options { paraphrase = "'{'";}: '{';
RIGHT_CUBR options { paraphrase = "'}'";}: '}';
LEFT_SQBR options { paraphrase = "'['";}: '[';
RIGHT_SQBR options { paraphrase = "']'";}: ']';

EQ options { paraphrase = "'='";}: '=';
LT options { paraphrase = "'<'";}: '<';
GT options { paraphrase = "'>'";}: '>';
SQL_NE options { paraphrase = "'<>'";}: "<>";
NE options { paraphrase = "'!=' or '^='";}: "!=" | "^=";
LE options { paraphrase = "'<='";}: "<=";
GE options { paraphrase = "'>='";}: ">=";

PERCENT options { paraphrase = "'%'";}: '%';
SEMICOLON options { paraphrase = "';'";}: ';';
COLON options { paraphrase = "':'";}: ':';
COMMA options { paraphrase = "','";}: ',';
DOT options { paraphrase = "'.'";}: '.';
QUOTMARK options { paraphrase = "'\"'";}: '"';
EXMARK options { paraphrase = "'!'";}: '!';
INTMARK options { paraphrase = "?:'";}: '?';
MINUS options { paraphrase = "'-'";}: '-';
PLUS options { paraphrase = "'+'";}: '+';

SUBFIELD options { paraphrase = "'->'";}
    : '-' '>'
    ;


// we allow identifiers to start with a number
IDENT options { testLiterals=true; paraphrase = "an identifier";}
    : ID_START_LETTER ( ID_LETTER )*
    ;

protected
ID_START_LETTER
    :    'a'..'z'
    |    'A'..'Z'
    |    '_'
    |    '\u0080'..'\ufffe'
    ;

protected
ID_LETTER
    :    ID_START_LETTER
    |    '0'..'9'
    ;
    
POSITIVE_INTEGER options { paraphrase = "a positive integer";}
    : NUMBER
    ;

NEGATIVE_INTEGER options { paraphrase = "a negative integer";}
    : '-' NUMBER
    ;

protected    
NUMBER
    : '0'..'9' ('0'..'9')*
    ;


WHITESPACE
    : (' ' | 't' | 'r' | 'n' | '\t') { $setType(Token.SKIP); }
    ;

// Single-line comments
SL_COMMENT
    :  "#"
        (~('\n'|'\r'))* ('\n'|'\r'('\n')?)
        {$setType(Token.SKIP); newline();}
    ;
    
LINEBREAK options { paraphrase = "a line break";}
    :   '\n'      { newline(); } // unix
    |   '\r' '\n' { newline(); } // dos
    |   '\r'      { newline(); } // mac
    ;
	
FIELDCOMMENT
	: SEMICOLON (~('\n'|'\r'))* LINEBREAK //('\n'|'\r'('\n')?) {newline();}
	;


//MESSAGE options { paraphrase = "an error message";}
//	: COLON (~('\n'|'\r'))* LINEBREAK //('\n'|'\r'('\n')?) {newline();}
//	;
	
FUNCTION_BODY
	: LEFT_CUBR (~('}'))* RIGHT_CUBR
	;
	
// string literals
STRING_LITERAL options { paraphrase = "a string litteral";}
    :   '"' (ESC|~('"'|'\\'|'\n'|'\r'))* '"'
    ;


// escape sequence -- note that this is protected; it can only be called
// from another lexer rule -- it will not ever directly return a token to
// the parser
// There are various ambiguities hushed in this rule. The optional
// '0'...'9' digit matches should be matched here rather than letting
// them go back to STRING_LITERAL to be matched. ANTLR does the
// right thing by matching immediately; hence, it's ok to shut off
// the FOLLOW ambig warnings.
protected
ESC
    :   '\\'
        (   'n'
        |   'r'
        |   't'
        |   'b'
        |   'f'
        |   '"'
        |   '\''
        |   '\\'
        |   ('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
        |   '0'..'3'
            (
                options {
                    warnWhenFollowAmbig = false;
                }
            :   '0'..'7'
                (
                    options {
                        warnWhenFollowAmbig = false;
                    }
                :   '0'..'7'
                )?
            )?
        |   '4'..'7'
            (
                options {
                    warnWhenFollowAmbig = false;
                }
            :   '0'..'7'
            )?
        )
    ;


// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
    :   ('0'..'9'|'A'..'F'|'a'..'f')
    ;
    
    
NOW: "$now";
TODAY: "$today";
UPPER: "upper";
LOWER: "lower";


// TODO throw better exceptions when no message for validation rules
{@SuppressWarnings("all")} class MDDBaseParser extends Parser;

options {
        buildAST=true;
        k = 3;
}

tokens {
    FIELD;
    VALIDATION;
    FUNCTION;
    
	// title field
	// !title=name
	// !title = nameSurname()
	TITLE="title";
    TITLEFIELD;
    TITLEFIELDFIELD;
    TITLEFIELDFUNCTION;
    
    // macro types
	// !type.genTyp=int{"Female"=0, "Male"=1}
    TYPENAME;
    TYPEDEF;
    TYPE="type";
    
    INCLUDE="include";
    INCLUDED;
    
    // MDD field
    // name = unique char[255] ;some comment
    FIELDNAME;
    MODIFIER;
    FIELDTYPE;
    PATH;
    
    // MDD subfield
    // cars = set
    // cars->brand = char[255]
    PARENTFIELDNAME;
    SUBFIELDNAME;
    SUBFIELDTYPE;
    
    // field types
    CHAR="char";
    INT="int";
    INTENUM;
    INTENUMTEXT;
    INTENUMINDEX;
    CHARENUM;
    CHARENUMELEMENT;
    REAL="real";
    BOOLEAN="boolean";
    TEXT="text";
    BINARY="binary";
    FILE="file";
    DATE="date";
    PTR="ptr";
    SET="set";
    SETCOMPLEX;
    SETINTENUM;
    SETCHARENUM;
    PTRONE;
    UNKNOWN_TYPE; // for type shorthands
    
    //modifiers
    UNIQUE="unique";
    FIXED="fixed";
    
    // native validation rules
    NOTNULL;
    NAN="NaN"; // not a number
    NOTEMPTY="notEmpty";
    NOTINT="notInt";
    NOTREAL="notReal";
    NOTBOOLEAN="notBoolean";
    
    // field type attributes
    CHAR_LENGTH;
    POINTED_TYPE;
    DEPRECATED="deprecated"; // intEnum
        
    // validation rules
    VALIDATIONNAME;
    MESSAGE;
    NATIVE_MESSAGE;
    
    RANGE="range";
    LENGTH="length";
    RANGE_FROM;
    RANGE_TO;
    
    MATCHES="matches";
    COMPARE="compare";
    
    COMPARE_EXPRESSION;
    
    // functions
    
    FUNCTION_ARGUMENT_NAME;
    FUNCTION_ARGUMENT;
    FUNCTION_NAME;
    SESSIONVAR_NAME;
    
    // Literal tokens
	NUM_DOUBLE;
	NUM_FLOAT;
	NUM_LONG;
	TRIPLE_DOT;
}

{
    
    RecognitionException error;
    
    public void reportError(RecognitionException e) {
        error=e;
    }

    public void reportError(String s) {
        if (error == null)
            error = new RecognitionException(s);
    }
    
    private void checkNumber(AST n) {
    	if(n == null)
    	   reportError("Incorrect value for number");
    }
    
    private void removeQuotation(AST a) {
    	a.setText(a.getText().substring(1, a.getText().length() -1));
    }
    
    protected String typeName;
    
    private AST currentField;
    
    protected boolean included = false;

    protected void disableField(AST field) { }
    
    protected AST include(AST type) { return null; }
    
    protected AST includeSubField(AST type, AST parentField) { return null; }
    
    protected AST parseExpression(AST expr) { return null; }
    
    protected void errorNestedSubfield(AST s) {}
    
    protected void checkFieldName(AST n) {}
    
    protected AST parseFunctionBody(AST b) { return null; }
    
    protected void addParsedFunction(AST a, AST b) {}
        
}

dataDefinition
    : (LINEBREAK!)* (declaration)* EOF!
    ;

declaration
    : fd:fieldDeclaration {currentField = #fd;} (LINEBREAK!)*
    | subFieldDeclaration (LINEBREAK!)*
    | titleDeclaration (LINEBREAK!)*
    | typeDeclaration (LINEBREAK!)*
    | includeDeclaration (LINEBREAK!)*
    | validationRuleDeclaration (LINEBREAK!)*
    | functionDeclaration (LINEBREAK!)*
    | nativeValidationRuleMessage (LINEBREAK!)*
    ;
    
    
//////////////// FIELD DECLARATION

fieldDeclaration
    : fn:fieldName
      e:EQ^ {#e.setType(FIELD); #e.setText(#fn.getText()); ((MDDAST)#e).wasIncluded = this.included; boolean hasBody = false;}
      (options{greedy=true;}:
          (modifier)* ft:fieldType
          (fieldComment)? {hasBody = true;}
      )?
      {
      	if(!hasBody) {
      	    disableField(#fieldDeclaration);
      	    #fieldDeclaration = null;
      	}
      }
    ;
    
    
subFieldDeclaration
    : 
      fn:atom {#fn.setType(PARENTFIELDNAME); } (SUBFIELD! a:atom! { #fn.setText(#fn.getText() + "->" + #a.getText()); } )* s:SUBFIELD^
      (
          titleDeclaration (fieldComment!)? // allow comment but do not store them
          | validationRuleDeclaration
          | functionDeclaration
          | EXMARK! INCLUDE! EQ! t:type { #subFieldDeclaration = includeSubField(#t, #fn); }
          | subFieldBody
      )
    ;
    
subFieldBody
	: a:fieldName { #a.setType(SUBFIELDNAME); ((MDDAST)#a).wasIncluded = this.included; } EQ! (modifier)* fieldType (fieldComment)?
    ;
    
fieldName
    : a:atom { checkFieldName(#a); #a.setType(FIELDNAME); }
    // this is an ugly workaround to allow certain keywords as a field name
    | k:keyword { checkFieldName(#k); #k.setType(FIELDNAME); }
    ;

fieldType
    : a:atom { #a.setType(UNKNOWN_TYPE); }
    | c:CHAR^ (LEFT_SQBR! (l:POSITIVE_INTEGER {#l.setType(CHAR_LENGTH); })? RIGHT_SQBR!)?
    | INT
    | intEnum
    | charEnum
    | REAL
    | BOOLEAN
    | TEXT
    | BINARY
    | FILE
    | DATE
    | PTR^ (options{greedy=true;}: p:type {#p.setType(POINTED_TYPE);})?
    | SET^ (options{greedy=true;}: s:type {#s.setType(POINTED_TYPE);})?
    | SET! si:intEnum {#si.setType(SETINTENUM);}
    | SET! sc:charEnum {#sc.setType(SETCHARENUM);}
    ;
    
//int { "aa"=5, "bb"=2 deprecated, "cc"=10}
intEnum
	// intEnum has no function body as such, but the syntax is the same so we use this trick
	: ie:INT^ {#ie.setType(INTENUM);} parsedExpression 
	;

//char { "aa", "bb" deprecated, "cc"}
charEnum
	// charEnum has no function body as such, but the syntax is the same so we use this trick
	: ce:CHAR^ {#ce.setType(CHARENUM);} parsedExpression
	;
	
fieldComment
	: f:FIELDCOMMENT { int k = #f.getText().indexOf(";"); #fieldComment.setText(#f.getText().substring(k+1).trim()); }
	;

errorMessage
	: COLON! m:STRING_LITERAL {#m.setType(MESSAGE); removeQuotation(#m); }
	;
    
modifier
    : u:UNIQUE { #u.setType(MODIFIER); }
    | f:FIXED { #f.setType(MODIFIER); }
    | "not" "null" { #modifier = #[MODIFIER, "not null"]; }
    | "not" "empty" { #modifier = #[MODIFIER, "not empty"]; }
    ;
    
// !title = name
titleDeclaration
    : EXMARK! TITLE! EQ! t:title
    ;
    
title
    : t:type { #t.setType(TITLEFIELDFIELD);}
    | f:functionCall
    ;
    
includeDeclaration
    : EXMARK! INCLUDE! EQ! t:type { #includeDeclaration = include(#t); }
    ;

// !type.genDef = ...
typeDeclaration
    : EXMARK! TYPE! DOT! n:atom { #n.setType(TYPENAME); } EQ! fieldType
    ;
    
    
    
//////////////// VALIDATION RULES

validationRuleDeclaration
	: 	(
			rangeValidationRuleDeclaration
			| uniquenessValidationRuleDeclaration
			| comparisonValidationRuleDeclaration
			| regexValidationRuleDeclaration
		)
		errorMessage
		(FIELDCOMMENT!)?
	;
	
comparisonValidationRuleDeclaration
	: COMPARE^ functionArguments parsedExpression
	;
		
rangeValidationRuleDeclaration
	: (RANGE^ | LENGTH^) functionArguments parsedExpression
	;

regexValidationRuleDeclaration
	: MATCHES^ functionArguments f:functionBody // {removeQuotation(#f);}
	;

// unique(field1, field2) : "These need to be unique"
uniquenessValidationRuleDeclaration
	: UNIQUE^ functionArguments
	;

nativeValidationRuleMessage
    : fieldName DOT!
      (
        UNIQUE
      | n:"notNull" { #n.setType(NOTNULL); }
      | NAN
      | NOTEMPTY
      | NOTINT
      | NOTREAL
      | NOTBOOLEAN
      )
      EQ!
      m:STRING_LITERAL {#m.setType(NATIVE_MESSAGE); removeQuotation(#m); }
    ;

//////////////// FUNCTIONS

functionDeclaration { AST p = null; }
	: (s:atom {#s.setType(SESSIONVAR_NAME);} PERCENT!)? a:functionName {#a.setType(FUNCTION_NAME);} d:functionArgumentDeclaration p = b:parsedFunctionBody (errorMessage)? (FIELDCOMMENT!)?
	{
		#functionDeclaration = #(#[FUNCTION, "function"], #functionDeclaration);
		addParsedFunction(#functionDeclaration, #p);
	}
	;

functionName
	: atom
	| keyword
	;

functionArgumentDeclaration
	: LEFT_PAREN! (functionArgumentBody)? (COMMA! functionArgumentBody)*  RIGHT_PAREN!
	;

functionArgumentBody
	: fieldType n:type {#n.setType(FUNCTION_ARGUMENT_NAME); }
	;

functionCall
	: a:atom {#a.setType(FUNCTION_NAME);} functionArguments
	;

functionArguments
	: LEFT_PAREN! (a:type {#a.setType(FUNCTION_ARGUMENT);} )? (COMMA! b:type {#b.setType(FUNCTION_ARGUMENT);} )* RIGHT_PAREN!
	;

functionBody
	: b:FUNCTION_BODY
	  {
	  	String body = ""; body = #b.getText().substring(1); body = body.substring(0, body.length() - 1); #b.setText(body.trim());
	  }
	;
	
parsedFunctionBody returns [AST p = null; ]
    : b:functionBody
    {
    	p = parseFunctionBody(#b);
    }
    ;

parsedExpression
	: b:functionBody
	  {
	  	#parsedExpression = parseExpression(#b);
	  }
	;

//////////////// COMMON

// general.Person
// general.Person->extraData
type
    : {String type="";} (a:atom {type+=#a.getText();} | k:keyword {type+=#k.getText();})
        (
              (DOT! {type += ".";} | SUBFIELD! {type += "->";})
              ( b:atom! {type += #b.getText(); } | ke:keyword! {type += #ke.getText(); }) 
        )*
        { #type.setText(type); #type.setType(PATH); }
    ;

number
    : POSITIVE_INTEGER | NEGATIVE_INTEGER
        {checkNumber(#number);}
    ;

operator
	: EQ | LT | GT | LE | GE | NE | SQL_NE | LIKE
	;

keyword
    : LENGTH
    | CHAR
    | TYPE
    | FILE
    | TEXT
    | TITLE
    | DATE
    ;

atom
    : IDENT
    ;