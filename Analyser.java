import error.AnalyzeError;
import error.CompileError;
import error.ErrorCode;
import error.TokenizeError;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import util.Pos;

public class Analyser {
    Tokenizer tokenizer;

    /**层数**/
    int LEVEL=1;
    /** 当前偷看的 token */
    Token peekedToken = null;

    /**当前函数**/
    Symbol functionNow=new Symbol();

    /** 符号表 */
    List<Symbol> symbolmap=new ArrayList<>();

    /**上一个返回的函数**/
    Symbol ReturnNow;

    /**全局变量表**/
    List<Global> globalmap=new ArrayList<>();
    /**全局变量个数**/
    int Gnum=0;

    /**某函数局部变量个数**/
    int Lnum=0;

    /**函数个数**/
    int Fnum=0;

    /**函数输出表**/
    List<Function> functionmap=new ArrayList<>();

    /**指令集表**/
    List<Instruction> instructionmap = new ArrayList<>();

    /**优先矩阵**/
    //1表示优先级左比右高，2表示不可比，-1表示左比右低，0表示等
    int suan[][]={
            {1,1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {1,1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {1,1,1,1,1,1,1,1,1,1,-1,1,-1},
            {1,1,1,1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,1,1,1,1,1,1,-1,1,-1},
            {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,0,-1},
            {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2,-1,-1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1},
    };    //0 + ;1 - ;2 *;3 /;4 <;5 <=;6 >;7 >=;8 ==;9 !=;10 (;11 );12 取反

    /**符号栈**/
    //用来表达式计算
    Stack<TokenType> stack = new Stack<>();

    /**break和continue相关**/
    int InWhile=0;
    //一个while中可能不止一个break和continue
    List<Break> BreakList=new ArrayList<>();
    List<Continue> ContinueList=new ArrayList<>();

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructionmap = new ArrayList<>();
    }

    public Analyser() {

    }

    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            System.out.print(token.getValue()+ " ");
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    private boolean check(TokenType tt) throws TokenizeError {
        Token token = peek();
        return token.getTokenType() == tt;
    }

    private Token expect(TokenType tt) throws CompileError {
        Token token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    public void analyse() throws CompileError {
        analyseProgram();
    }

    public void analyseProgram() throws CompileError {
        //程序结构
        //program -> decl_stmt* function*
        while(check(TokenType.LET_KW)||check(TokenType.CONST_KW)) analyseDeclStmt();

        //_start是0
        Fnum=1;
        //保存之前的，给_start
        List<Instruction> init=instructionmap;
        while(check(TokenType.FN_KW)){
            //每次分析函数之前重置指令表
            instructionmap = new ArrayList<>();
            //重置局部变量
            Lnum=0;
            analyseFunction();

            //全局，函数
            Gnum++;
            Fnum++;
        }
        //看是否有main函数,符号表
        int mainId=SearchByNameExist("main");
        if(mainId == -1) throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        Symbol main = symbolmap.get(mainId);
        int findmain=findIDbyName("main");

        if (main.back.equals("void")) {
            //没有返回值则分配0个地址
            init.add(new Instruction(Operation.stackalloc,0x1a, 0));
            init.add(new Instruction(Operation.call,0x48,findmain));
        }
        else {
            //有返回值分配一个空间
            init.add(new Instruction(Operation.stackalloc,0x1a, 1));
            init.add(new Instruction(Operation.call,0x48,findmain));
            init.add(new Instruction(Operation.popn,0x03,1));
        }
        //向全局变量填入口程序_start
        Global global = new Global(Gnum,true, 6, "_start");
        globalmap.add(global);
        //_start加入函数表,该函数的编号为0,指令集是init
        Function function = new Function(0,Gnum,"_start",0,0,0,init);
        //添加到索引为0的地方,后面后移
        functionmap.add(0,function);
        Gnum++;
    }

    /**声明语句*/
    private void analyseDeclStmt() throws CompileError {
        //decl_stmt -> let_decl_stmt | const_decl_stmt
        if(check(TokenType.LET_KW)) analyseLetDeclStmt();
        else if(check(TokenType.CONST_KW)) analyseConstDeclStmt();
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        if(LEVEL==1) Gnum++;
        else Lnum++;
    }

    private void analyseLetDeclStmt() throws CompileError {
        //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
        expect(TokenType.LET_KW);
        //记录标识符名字
        Token ident=expect(TokenType.IDENT);
        String name= (String)ident.getValue();
        expect(TokenType.COLON);

        //类型
        Token ty=analyseTy();
        String type=(String)ty.getValue();
        if(type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        //全局变量入表
        if(LEVEL==1) globalmap.add(new Global(Gnum,false));
        //加符号表
        int k=SearchByNameAdd(name);
        //System.out.println("\n"+k);
        if(k==-1)  symbolmap.add(new Symbol(name,type,false,LEVEL,Lnum,Gnum,-1,-1));
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        //赋值才需要取地址
        if(check(TokenType.ASSIGN)){
            next();
            Instruction ins;
            if(LEVEL==1) ins = new Instruction(Operation.globa,0x0c,Gnum);
            //局部变量指令
            else ins = new Instruction(Operation.loca,0x0a,Lnum);
            instructionmap.add(ins);

            String type2;
            //类型是否一致
            type2=analyseExpr();
            if(!type2.equals(type)) throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());
            //表达式运算后需要弹栈(把剩下的操作符的指令)
            popZ(type2);

            //存值
            ins = new Instruction(Operation.store_64,0x17,-1);
            instructionmap.add(ins);
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseConstDeclStmt() throws CompileError {
        //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
        expect(TokenType.CONST_KW);

        //记录标识符名字
        Token ident=expect(TokenType.IDENT);
        String name= (String)ident.getValue();

        expect(TokenType.COLON);

        Token ty=analyseTy();
        String type=(String)ty.getValue();
        if(type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        //全局变量入表
        if(LEVEL==1){
            globalmap.add(new Global(Gnum,true));
            //此时指令为globa，压入地址
            Instruction ins = new Instruction(Operation.globa,0x0c,Gnum);
            instructionmap.add(ins);
        }
        //局部变量指令
        else{
            //生成 loca 指令，准备赋值
            Instruction ins = new Instruction(Operation.loca,0x0a,Lnum);
            instructionmap.add(ins);
        }

        //不重复就放进去
        if(SearchByNameAdd(name)==-1) symbolmap.add(new Symbol(name,type,true,LEVEL,Lnum,Gnum,-1,-1));
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        expect(TokenType.ASSIGN);

        //类型不一致要报错
        String type2=analyseExpr();
        if(!type.equals(type2)) throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        //表达式运算后需要弹栈(把剩下的操作符的指令)
        popZ(type2);

        expect(TokenType.SEMICOLON);

        Instruction ins = new Instruction(Operation.store_64,0x17,-1);
        instructionmap.add(ins);
    }

    /**类型系统*/
    private Token analyseTy() throws CompileError {
        //ty -> IDENT 只能是void和int和double
        Token tt=peek();
        if(tt.getValue().equals("void")||tt.getValue().equals("int")||tt.getValue().equals("double")){
            next();
            return tt;
        }
        //否则抛出异常
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

    }


    /**函数声明*/
    private void analyseFunction() throws CompileError {
        //function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
        expect(TokenType.FN_KW);
        Lnum=0;

        Token tt= expect(TokenType.IDENT);
        String name=(String)tt.getValue();

        //检查函数是否重复
        if(SearchByNameAdd(name)!=-1)  throw new AnalyzeError(ErrorCode.Break,peekedToken.getEndPos());

        expect(TokenType.L_PAREN);

        //参数列表
        List<Symbol> n=new ArrayList<>();
        if(!check(TokenType.R_PAREN)){
            n=analyseFunctionParamList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);

        int returnSlot=0;

        //函数返回值类型
        Token ty=analyseTy();
        String back=(String)ty.getValue();
        if(back.equals("int")||back.equals("double")) returnSlot=1;

        //加入符号表,存当前函数
        Symbol fun=new Symbol(name,"fun",n,LEVEL,back);

        
        //System.out.println(name+LEVEL);
        functionNow=fun;
        symbolmap.add(fun);

        //加入函数表，为了输出
        Function function = new Function(Fnum,Gnum,name,returnSlot,n.size(),0,null);
        functionmap.add(function);

        analyseBlockStmt();

        //局部变量个数分析完了才能写出来

        function.localSlots=Lnum;
        function.body=instructionmap;
        function.name=Gnum;

        if(back.equals("void"))
            instructionmap.add(new Instruction(Operation.ret,0x49, -1));
        else if(!ReturnNow.getName().equals(functionNow.getName())){
            throw new AnalyzeError(ErrorCode.Break, peekedToken.getStartPos());
        }

        Global global = new Global(Gnum,true,name.length(),name);
        globalmap.add(global);
    }

    private List<Symbol> analyseFunctionParamList() throws CompileError {
        //function_param_list -> function_param (',' function_param)*
        List<Symbol> n=new ArrayList<>();
        //把参数加入参数列表,传入id编号
        int i=0;
        n.add(analyseFunctionParam(i));
        while(check(TokenType.COMMA)){
            next();
            i++;
            n.add(analyseFunctionParam(i));
        }
        return n;
    }

    private Symbol analyseFunctionParam(int paraid) throws CompileError {
        //function_param -> 'const'? IDENT ':' ty
        String type=" ";
        boolean isConst=false;
        if(check(TokenType.CONST_KW)){
            isConst=true;
            next();
        }
        Token ident=expect(TokenType.IDENT);
        String name= (String)ident.getValue();

        expect(TokenType.COLON);

        Token ty= analyseTy();
        type=(String)ty.getValue();

        int level=LEVEL+1;
        Symbol param=new Symbol(name,type,isConst,level,-1,-1,Fnum,paraid);
        //System.out.println(name+level);
        //没变量会跟参数重名
        symbolmap.add(param);
        return param;
    }

    /**表达式*/
    private String analyseExpr() throws CompileError {
        //expr ->
        //    | negate_expr
        //    | assign_expr
        //    | call_expr
        //    | literal_expr
        //    | ident_expr
        //    | group_expr
        //     (binary_operator expr||'as' ty)*
        String type="void";
        //减号开头，取反或者运算
        if(check(TokenType.MINUS)){
            type=analyseNegateExpr();
        }
        else if(check(TokenType.L_PAREN)){
            type=analyseGroupExpr();
        }
        else if(check(TokenType.UINT_LITERAL)||check(TokenType.DOUBLE_LITERAL)||check(TokenType.STRING_LITERAL)||check(TokenType.CHAR_LITERAL)){
            System.out.println("到这1");
            type=analyseLiteralExpr();
            System.out.println("到这2");
        }

        else if(check(TokenType.IDENT)){
            //三个以IDENT开头的非终结符
            Token ident= next();
            String name= (String)ident.getValue();
            int position=SearchByNameExist(name);
            Symbol symbol;
            //是否是库函数
            boolean ku=false;
            //记录IDENT的type
            //如果没有找到，看看是不是标准库函数
            if (position==-1){
                symbol=judgeKu(name);
                type="fun";
                ku=true;
                //不是标准库函数
                if(symbol==null) throw new AnalyzeError(ErrorCode.Break,ident.getStartPos());
            }
            else{
                symbol=symbolmap.get(position);
                type=symbol.getType();
            }
            //函数调用,把type赋值为返回值
            if(check(TokenType.L_PAREN)){
                if(!type.equals("fun")) throw new AnalyzeError(ErrorCode.Break,ident.getStartPos());
                type=analyseCallExpr(symbol,ku);
            }
            //赋值
            else if(check(TokenType.ASSIGN)){
                //常量和函数不能在等号左边
                if(symbol.isConst||(symbol.type.equals("fun"))) throw new AnalyzeError(ErrorCode.Break,ident.getStartPos());
                type=analyseAssignExpr(symbol,type);
            }
            else {
                analyseIdentExpr(symbol);
            }
        }
        while(check(TokenType.AS_KW)||check(TokenType.PLUS)||check(TokenType.MINUS)||check(TokenType.MUL)||check(TokenType.DIV)||check(TokenType.EQ)||check(TokenType.NEQ)||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE)){
            if(check(TokenType.AS_KW)){
                type=analyseAsExpr(type);
            }
            else{
                type=analyseOperatorExpr(type);
            }
        }
        return type;
    }

    /**运算符表达式*/
    private void analyseBinaryOperator(String type) throws CompileError {
        //binary_operator -> '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' | '>='
        if(check(TokenType.PLUS)||check(TokenType.MINUS)||check(TokenType.MUL)||check(TokenType.DIV)||check(TokenType.EQ)||check(TokenType.NEQ)||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE)){
            //如果不空则继续
            //算符优先
            Token n=next();
            TokenType last=n.getTokenType();
            while(!stack.empty()){
                TokenType front=stack.peek();
                //前面优先级高，运算
                if(suan[OpFunction.change(front)][OpFunction.change(last)]==1){
                    front = stack.pop();
                    OpFunction.OpInstruction(front,instructionmap,type);
                }
                //优先级低，继续
                else break;
            }
            //入栈
            stack.push(last);
        }
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
    }

    private String analyseOperatorExpr(String typeLeft) throws CompileError {
        //operator_expr -> expr binary_operator expr
        //消除左递归
        analyseBinaryOperator(typeLeft);
        String typeRight=analyseExpr();
        //左右类型一样
        if(typeLeft.equals(typeRight)) return typeLeft;
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
    }

    /**取反表达式*/
    private String analyseNegateExpr() throws CompileError {
        //negate_expr -> '-' expr
        expect(TokenType.MINUS);
        //放上面试试
        stack.push(TokenType.FAN);
        String type= analyseExpr();
        if(type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
        return type;
    }

    /**赋值表达式*/
    private String analyseAssignExpr(Symbol symbol,String typeLeft) throws CompileError {
        //assign_expr -> l_expr '=' expr
        //l_expr已经判断过了,传进来
        expect(TokenType.ASSIGN);
        //参数
        if(symbol.paraid!=-1&&symbol.funid!=-1){
            Function fun= functionmap.get(symbol.funid-1);
            //参数的位置在返回值之后
            instructionmap.add(new Instruction(Operation.arga,0x0b, fun.returnSlots+symbol.paraid));
        }
        //局部变量
        else if(symbol.level!= 1) instructionmap.add(new Instruction(Operation.loca,0x0a,symbol.Lid));
            //如果该ident是全局变量
        else instructionmap.add(new Instruction(Operation.globa,0x0c,symbol.Gid));

        String typeRight=analyseExpr();
        popZ(typeRight);
        //存储
        //存储到地址中
        Instruction ins = new Instruction(Operation.store_64,0x17,-1);
        instructionmap.add(ins);
        //类型是否一样
        if(typeLeft.equals(typeRight)&&(!typeLeft.equals("void"))) return "void";
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
    }

    /**类型转换表达式*/
    private String analyseAsExpr(String typeL) throws CompileError {
        //as_expr -> expr 'as' ty
        //消除左递归
        expect(TokenType.AS_KW);
        Token ty=analyseTy();
        String type=(String)ty.getValue();

        //如果是将int转为double
        if(typeL.equals("int") && type.equals("double")){
            instructionmap.add(new Instruction(Operation.itof, 0x36,-1));
            return "double";
        }
        //如果是将double转为int
        else if(typeL.equals("double") && type.equals("int")){
            instructionmap.add(new Instruction(Operation.ftoi,0x37, -1));
            return "int";
        }
        //只能是int 和double
        if(type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
        return type;
    }

    /**函数调用表达式*/
    //参数
    private void analyseCallParamList(Symbol function) throws CompileError {
        //call_param_list -> expr (',' expr)*
        int position =0;
        String type="int";
        List<Symbol> param=function.param;

        type=analyseExpr();

        //把函数里没有运算的符号弹出来
        while((stack.peek()!=TokenType.L_PAREN)&&(!stack.empty())) {
            TokenType tt=stack.pop();
            OpFunction.OpInstruction(tt,instructionmap,type);
        }
        //参数类型不同
        if(!param.get(position).type.equals(type)) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
        position++;
        while(check(TokenType.COMMA)){
            next();
            type=analyseExpr();
            if(!param.get(position).type.equals(type)) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
            position++;
            while(stack.peek()!=TokenType.L_PAREN&&!stack.empty()) {
                TokenType tt=stack.pop();
                OpFunction.OpInstruction(tt,instructionmap,type);
            }
        }
        //参数个数不同
        if(position!=param.size()) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
    }

    //调用
    private String analyseCallExpr(Symbol function,boolean ku) throws CompileError {
        //call_expr -> IDENT '(' call_param_list? ')'
        //IDENT判断过了
        Instruction ins;

        if(ku){
            //库函数需要被加进全局变量表
            Global global=new Global(Gnum,true,function.name.length(),function.name);
            globalmap.add(global);
            //调用操作
            ins = new Instruction(Operation.callname,0x4a,Gnum);
            Gnum++;
        }
        else{
            //一般函数，找寻函数id
            int id= findIDbyName(function.name);
            //自己调自己
            if(id==-1) id=Fnum;
            ins = new Instruction(Operation.call,0x48,id);
        }

        //分配返回值空间
        int x=1;
        if(function.back.equals("void"))  x=0;
        instructionmap.add(new Instruction(Operation.stackalloc, 0x1a,x));

        expect(TokenType.L_PAREN);
        //左括号入栈
        stack.push(TokenType.L_PAREN);

        if(!check(TokenType.R_PAREN)){
            analyseCallParamList(function);
        }
        expect(TokenType.R_PAREN);

        stack.pop();

        //最后压入call命令
        instructionmap.add(ins);
        return function.getBack();
    }

    /**字面量表达式*/
    private String analyseLiteralExpr() throws CompileError {
        //literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL|CHAR_LITERAL
        if(check(TokenType.UINT_LITERAL)){
            Token number=next();
            //把常数压入栈
            Instruction ins = new Instruction(Operation.push,0x01,(long)number.getValue());
            instructionmap.add(ins);
            return "int";
        }
        else if(check(TokenType.CHAR_LITERAL)){
            Token number=next();
            char name=(char)number.getValue();
            int num=name;
            //把常数压入栈
            Instruction ins = new Instruction(Operation.push,0x01,num);
            instructionmap.add(ins);
            return "int";
        }
        else if(check(TokenType.DOUBLE_LITERAL)){
            Token number=next();
            //直接放进去试试
             String binary = Long.toBinaryString(Double.doubleToRawLongBits((Double) number.getValue()));
             instructionmap.add(new Instruction(Operation.push,0x01, toTen(binary)));
          

            //Instruction ins = new Instruction(Operation.push,0x01,Double.doubleToRawLongBits((double)number.getValue()));
            //instructionmap.add(ins);
            return "double";
        }
        else if(check(TokenType.STRING_LITERAL)){
            Token str=next();
            String name =(String)str.getValue();
            //字符串是全局变量,加入全局变量量表
            Global global = new Global(Gnum,true,name.length(),name);
            globalmap.add(global);

            //加入指令集，只需放入全局变量编号即可
            Instruction ins = new Instruction(Operation.push,0x01,Gnum);
            instructionmap.add(ins);

            //变量数量+1
            Gnum++;
            return "string";
        }
        else throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
    }

    /**标识符表达式*/
    private void analyseIdentExpr(Symbol symbol) throws CompileError {
        //ident_expr -> IDENT
        if(symbol.type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());

        //参数
        if(symbol.paraid!=-1&&symbol.funid!=-1){
            Function fun= functionmap.get(symbol.funid-1);
            //参数的位置在返回值之后
            instructionmap.add(new Instruction(Operation.arga,0x0b, fun.returnSlots+symbol.paraid));
        }
        //局部变量
        else if(symbol.level!= 1) instructionmap.add(new Instruction(Operation.loca,0x0a,symbol.Lid));
            //如果该ident是全局变量
        else instructionmap.add(new Instruction(Operation.globa,0x0c,symbol.Gid));

        instructionmap.add(new Instruction(Operation.load,0x13, -1));

    }

    /**括号表达式*/
    private String analyseGroupExpr() throws CompileError {
        //group_expr -> '(' expr ')'
        expect(TokenType.L_PAREN);
        stack.push(TokenType.L_PAREN);
        String type=analyseExpr();
        //遇到右括号，算括号里的所有东西

        while(stack.peek()!= TokenType.L_PAREN) {
            TokenType tt=stack.pop();
            OpFunction.OpInstruction(tt,instructionmap,type);
        }
        stack.pop();
        expect(TokenType.R_PAREN);
        return type;
    }

    /**语句*/
    private void analyseStmt() throws CompileError {
        //stmt ->
        //      expr_stmt
        //    | decl_stmt *
        //    | if_stmt *
        //    | while_stmt *
        //    | return_stmt *
        //    | block_stmt *
        //    | empty_stmt *
        if(check(TokenType.IF_KW)) analyseIfStmt();
        else if(check(TokenType.WHILE_KW)) analyseWhileStmt();
        else if(check(TokenType.RETURN_KW)) analyseReturnStmt();
        else if(check(TokenType.L_BRACE)) analyseBlockStmt();
        else if(check(TokenType.SEMICOLON)) analyseEmptyStmt();
        else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)) analyseDeclStmt();
        else if(check(TokenType.BREAK_KW))  analyseBreakStmt();
        else if(check(TokenType.CONTINUE_KW))  analyseContinueStmt();

        else analyseExprStmt();
    }
    /**break语句块**/
    private void analyseBreakStmt()throws CompileError{
        expect(TokenType.BREAK_KW);
        //不在循环里报错
        if(InWhile==0) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
        Instruction instruction = new Instruction(Operation.br,0x41, 0);
        //位置是break语句序号
        instructionmap.add(instruction);
        BreakList.add(new Break(instruction,instructionmap.size(), InWhile));
        expect(TokenType.SEMICOLON);
    }

    /**continue语句块**/
    private void analyseContinueStmt()throws CompileError{
        expect(TokenType.CONTINUE_KW);
        //不在循环里报错
        if(InWhile==0) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
        Instruction instruction = new Instruction(Operation.br,0x41, 0);
        //位置是break语句序号
        instructionmap.add(instruction);
        ContinueList.add(new Continue(instruction,instructionmap.size(), InWhile));
        expect(TokenType.SEMICOLON);
    }


    /**表达式语句*/
    private void analyseExprStmt() throws CompileError {
        //expr_stmt -> expr ';'
        String type=analyseExpr();
        //弹栈运算
        popZ(type);
        expect(TokenType.SEMICOLON);
    }

    /**控制流语句*/
    private void analyseIfStmt() throws CompileError {
        //if_stmt -> 'if' expr block_stmt ('else' (block_stmt | if_stmt))?
        expect(TokenType.IF_KW);
        String type=analyseExpr();
        popZ(type);
        if(type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());

        instructionmap.add(new Instruction(Operation.br_t,0x43, 1));
        //无条件跳转到else
        Instruction jumptoElse=new Instruction(Operation.br,0x41, 0);
        instructionmap.add(jumptoElse);

        //执行前指令位置
        int positionL=instructionmap.size();

        analyseBlockStmt();

        //执行后指令位置
        int positionR=instructionmap.size();

        //if里面有返回值
        //z最后一个是返回值
        if (instructionmap.get(positionR-1).getOpt().equals(Operation.ret)) {
            //直接跳到else
            //偏移量
            int off=positionR-positionL;
            jumptoElse.setX(off);

            if(check(TokenType.ELSE_KW)){
                expect(TokenType.ELSE_KW);
                if(check(TokenType.L_BRACE)){
                    analyseBlockStmt();
                    //if里返回，不用跳过else，直接返回即可
                    //if (!instructionmap.get(positionR -1).getOpt().equals("ret"))
                     //   instructionmap.add(new Instruction(Operation.br,0x41,0));
                }
                else if(check(TokenType.IF_KW))
                    analyseIfStmt();
                else throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
            }
        }
        else {
            //if里需要加入一句跳过else
            Instruction jumptoEnd = new Instruction(Operation.br,0x41,-1);
            instructionmap.add(jumptoEnd);

            int zhong=instructionmap.size();
            //再算偏移
            int off =  zhong-positionL;
            jumptoElse.setX(off);

            if(check(TokenType.ELSE_KW)){
                expect(TokenType.ELSE_KW);
                if(check(TokenType.L_BRACE)){
                    analyseBlockStmt();
                    //else最后需要加一个跳转
                    instructionmap.add(new Instruction(Operation.br,0x41,0));
                }
                else if(check(TokenType.IF_KW))
                    analyseIfStmt();
                else throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());

            }
            //跳过else的偏移
            off = instructionmap.size() -zhong ;
            jumptoEnd.setX(off);
        }

    }
    private void analyseWhileStmt() throws CompileError {
        //while_stmt -> 'while' expr block_stmt
        expect(TokenType.WHILE_KW);
        //先找到起始位置
        instructionmap.add(new Instruction(Operation.br,0x41,0));
        int whileStart = instructionmap.size();

        String type=analyseExpr();
        if(type.equals("void")) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());
        popZ(type);

        //真的就执行
        instructionmap.add(new Instruction(Operation.br_t,0x43,1));
        //无条件跳转，准备跳过while
        Instruction jumptoEnd = new Instruction(Operation.br,0x41,0);
        instructionmap.add(jumptoEnd);

        int positionL = instructionmap.size();

        //进入一层循环加一下
        InWhile++;
        analyseBlockStmt();
        //出循环减一下
        InWhile--;

        //跳回while
        Instruction jumptoWhile = new Instruction(Operation.br,0x41,0);
        instructionmap.add(jumptoWhile);

        int whileEnd = instructionmap.size();
        jumptoWhile.setX(whileStart - whileEnd);

        jumptoEnd.setX(whileEnd - positionL);

        for(Break bb:BreakList){
            int off=whileEnd-bb.position;
            //跳到while后第一条语句
            if(bb.getWhileId() == InWhile+1) bb.instruction.setX(off);
        }
        for(Continue cc:ContinueList){
            int off=whileEnd-cc.position;
            //跳到while的最后一条语句，即跳回while
            if(cc.getWhileId() == InWhile+1) cc.instruction.setX(off-1);
        }
        if(InWhile==0){
            BreakList.clear();
            ContinueList.clear();
        }


    }

    private void analyseReturnStmt() throws CompileError {
        //return_stmt -> 'return' expr? ';'
        expect(TokenType.RETURN_KW);
        String backType="void";
        //加载返回地址
        if(!functionNow.getBack().equals("void")){
            instructionmap.add(new Instruction(Operation.arga,0x0b,0));
            if(!check(TokenType.SEMICOLON)){
                backType=analyseExpr();
                popZ(backType);
            }
            //放入地址中
            instructionmap.add(new Instruction(Operation.store_64,0x17,-1));
        }

        //如果返回值不一样，就报错
        if(!backType.equals(functionNow.getBack())) throw new AnalyzeError(ErrorCode.Break,peekedToken.getStartPos());

        expect(TokenType.SEMICOLON);
        //ret
        instructionmap.add(new Instruction(Operation.ret,0x49,-1));
        ReturnNow=functionNow;
    }
    /**代码块*/
    private void analyseBlockStmt() throws CompileError {
        //进入分程序，改变LEVEL
        LEVEL++;
        //block_stmt -> '{' stmt* '}'
        expect(TokenType.L_BRACE);
        while(!check(TokenType.R_BRACE)) analyseStmt();
        expect(TokenType.R_BRACE);

        //出栈
        outZhan();

        LEVEL--;
    }
    /**空语句*/
    private void analyseEmptyStmt() throws CompileError {
        //empty_stmt -> ';'
        expect(TokenType.SEMICOLON);
    }




   /**按照名字查询符号表**/
   //如果该层没有，返回-1,声明时调用
    public int SearchByNameAdd(String name){
        Symbol n;
        for(int i = symbolmap.size()-1;i >=0;i--) {
            n = symbolmap.get(i);
            if (name.equals(n.name)&&LEVEL==n.getLevel()) return i;
        }
        return -1;
    }

    //如果不存在，用时调用
    public int SearchByNameExist(String name){
        Symbol n=new Symbol();
        for(int i = symbolmap.size()-1;i >=0;i--) {
            n = symbolmap.get(i);
            if (name.equals(n.name)) return i;
        }
        return -1;
    }

    //判断是不是标准库函数，并且返回一个Symbol
    public Symbol judgeKu(String name){
        List<Symbol> param=new ArrayList<>();
        String back="void";
        if(name.equals("getint")||name.equals("getchar")) back="int";
        else if(name.equals("getdouble")) back="double";
        else if(name.equals("putln")) back="void";
        else if(name.equals("putint")){
            param.add(new Symbol("param1","int",false,LEVEL+1,-1,-1,-1,0));
            back="void";
        }
        else if(name.equals("putdouble")){
            param.add(new Symbol("param1","double",false,LEVEL+1,-1,-1,-1,0));
            back="void";
        }
        else if(name.equals("putchar")){
            param.add(new Symbol("param1","int",false,LEVEL+1,-1,-1,-1,0));
            back="void";
        }
        else if(name.equals("putstr")){
            param.add(new Symbol("param1","string",false,LEVEL+1,-1,-1,-1,0));
            back="void";
        }
        else return null;

        Symbol kuFun=new Symbol(name,"fun",param,LEVEL,back);
        return kuFun;
    }

    //结束一层之后，出栈
    public void outZhan(){
        Symbol n;
        for(int i = symbolmap.size()-1;i >=0;i--) {
            //局部变量减少
            n = symbolmap.get(i);
            if (n.level==LEVEL) symbolmap.remove(i);
        }
    }

    /**符号栈弹栈**/
    public void popZ(String cal){
        //弹栈运算
        while (!stack.empty()) {
            TokenType type= stack.pop();
            OpFunction.OpInstruction(type,instructionmap,cal);
        }
    }

    /**在函数表通过全局编号找函数编号**/
    private int findIDbyName(String name){
        Function n;
        for(int i = functionmap.size()-1;i >=0;i--) {
            n = functionmap.get(i);
            if (n.name1.equals(name)) return n.id;
        }
        return -1;
    }

   public static Long toTen(String a){
        Long aws = 0L;
        Long xi = 1L;
        for(int i=a.length()-1; i>=0; i--){
            if(a.charAt(i) == '1')
                aws += xi;
            xi *=2;
        }
        return aws;
    }
}