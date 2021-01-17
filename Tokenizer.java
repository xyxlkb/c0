import error.ErrorCode;
import error.TokenizeError;

public class Tokenizer {
    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    //跳过空白字符
    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();
        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        /**无符号整数  浮点数*/
        if (Character.isDigit(peek)) {
            return lexUIntOrDouble();
        }
        /**关键字  标识符*/
        else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        }
        /**标识符*/
        else if (peek=='_') {
            return lexIdent();
        }
        /**字符串常量*/
        else if (peek=='"') {
            return lexString();
        }
        /**字符常量*/
        else if (peek=='\'') {
            return lexChar();
        }
        /**下划线*/
        else if (peek=='_') {
            return lexString();
        }
        else if (peek=='\\') {
            return lexString();
        }
        else if (peek=='|') {
            return lexString();
        }
        /**运算符号   注释*/
        else {
            return lexOtherOrUnknown();
        }
    }

    /**无符号整数   浮点数*/
    //目前只有无符号整数
    private Token lexUIntOrDouble() throws TokenizeError {
        String shu="";
        //整数是0，浮点数1
        int type=0;
        double f=0.0;
        long m=0;
        int i=5535;
        while(i>0&&(Character.isDigit(it.peekChar())||it.peekChar()=='.')){
            if(it.peekChar()=='.') type=1;
            shu=shu+it.nextChar();
            i--;
        }
        if(type==1){
            if(it.peekChar() == 'e'||it.peekChar() == 'E'){
                shu=shu+it.nextChar();
                if(it.peekChar() == '+'||it.peekChar() == '-'){
                    shu=shu+it.nextChar();
                }
                i=3553;
                while(i>0&&Character.isDigit(it.peekChar())) shu=shu+it.nextChar();
            }
            return new Token(TokenType.DOUBLE_LITERAL,Double.valueOf(shu.toString()),it.previousPos(),it.currentPos());
        }
        else {
            m=Long.parseLong(shu);
            return new Token(TokenType.UINT_LITERAL,m, it.previousPos(), it.currentPos());
        }
    }

    /**关键字  标识符*/
    private Token lexIdentOrKeyword() throws TokenizeError {
        String word="";
        //下划线或者字母
        while(Character.isLetterOrDigit(it.peekChar())||(it.peekChar()=='_')){
            word=word+it.nextChar();
        }
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        if(word.equals("fn"))  return new Token(TokenType.FN_KW,"fn", it.previousPos(), it.currentPos());
        else if(word.equals("let"))  return new Token(TokenType.LET_KW,"let", it.previousPos(), it.currentPos());
        else if(word.equals("const"))  return new Token(TokenType.CONST_KW,"const", it.previousPos(), it.currentPos());
        else if(word.equals("as"))  return new Token(TokenType.AS_KW,"as", it.previousPos(), it.currentPos());
        else if(word.equals("while"))  return new Token(TokenType.WHILE_KW,"while", it.previousPos(), it.currentPos());
        else if(word.equals("if"))  return new Token(TokenType.IF_KW,"if", it.previousPos(), it.currentPos());
        else if(word.equals("else"))  return new Token(TokenType.ELSE_KW,"else", it.previousPos(), it.currentPos());
        else if(word.equals("return"))  return new Token(TokenType.RETURN_KW,"return", it.previousPos(), it.currentPos());
        /**扩展c0*/
        else if(word.equals("break"))  return new Token(TokenType.BREAK_KW,"break", it.previousPos(), it.currentPos());
        else if(word.equals("continue"))  return new Token(TokenType.CONTINUE_KW,"continue", it.previousPos(), it.currentPos());
        else return new Token(TokenType.IDENT,word, it.previousPos(), it.currentPos());
    }

    /**标识符*/
    private Token lexIdent() throws TokenizeError {
        String word="";
        while(Character.isLetterOrDigit(it.peekChar())||it.peekChar()=='_'){
            word=word+it.nextChar();
        }
        return new Token(TokenType.IDENT,word, it.previousPos(), it.currentPos());
    }


    /**运算符号   注释 */
    private Token lexOtherOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            //无歧义运算符
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());
            case '_':
                return new Token(TokenType.UNDERLINE, '_', it.previousPos(), it.currentPos());
            case '\\':
                return new Token(TokenType.GANG, '\\', it.previousPos(), it.currentPos());

            //减号  ->
            case '-':
                if(it.peekChar()=='>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());

            //赋值  等于
            case '=':
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());

            //不等于
            case '!':
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());

            //小于  小于等于
            case '<':
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());
            //大于  大于等于
            case '>':
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());

                //出发还是注释
            case '/':
                if(it.peekChar()=='/'){
                    it.nextChar();
                    lexComment();
                    return nextToken();
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());

            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    /**注释**/
    //直接略过注释分析即可
    private  Token lexComment() throws TokenizeError{
        while(true){
            char now=it.nextChar();
            if(now=='\n') break;
        }
        return new Token(TokenType.COMMENT, '/', it.previousPos(), it.currentPos());
    }


    /**字符常量**/
    private Token lexChar() throws TokenizeError {
        //char_regular_char -> [^'\\]
        //CHAR_LITERAL -> '\'' (char_regular_char | escape_sequence) '\''
        char look =' ';
        it.nextChar();

        char now=it.nextChar();
        System.out.println(now+"\n");
        if(now=='\\'){
            //转义
            if (it.peekChar() == 'n') {
                it.nextChar();
                look = '\n';
            } else if (it.peekChar() == '\'') {
                it.nextChar();
                look = '\'' ;
            } else if (it.peekChar() == '\\') {
                look = '\\';
                it.nextChar();
            }else if (it.peekChar() == '"') {
                look = '\"';
                it.nextChar();
            }
            else if (it.peekChar() == 't') {
                look = '\t';
                it.nextChar();
            }
            else if (it.peekChar() == 'r') {
                look = '\r';
                it.nextChar();
            }
        }
        else look = now;
        it.nextChar();
        //System.out.println((int)look);
        return new Token(TokenType.CHAR_LITERAL,look,it.previousPos(), it.currentPos());
    }
    /**字符串常量*/
    private Token lexString() throws TokenizeError {
        String chuan="";
        it.nextChar();
        int i = 6553;
        //\的数量
        while(i>0) {
            char look = it.nextChar();
            System.out.println(look + "\n");
            i--;
           if (look == '\\') {
                //转义
                if (it.peekChar() == 'n') {
                    it.nextChar();
                    chuan = chuan + '\n';
                } else if (it.peekChar() == '\'') {
                    it.nextChar();
                    chuan = chuan + '\'';
              } else if (it.peekChar() == '\\') {
                    chuan = chuan + '\\';
                    it.nextChar();
                } else if (it.peekChar() == '"') {
                    it.nextChar();
                    chuan = chuan + '"';
                }
                else if (it.peekChar() == 't') {
                    it.nextChar();
                    chuan = chuan + '\t';
                }
                else if (it.peekChar() == 'r') {
                    it.nextChar();
                    chuan = chuan + '\r';
                }
           }
           else if (look == '"') {
                break;
            }
            else chuan = chuan + look;
        }
        return new Token(TokenType.STRING_LITERAL, chuan, it.previousPos(), it.currentPos());
    }


}
