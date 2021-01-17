import java.util.List;
//一些自定义的函数
public class OpFunction{


    public static int change(TokenType type){
        if(type==TokenType.PLUS) return 0;
        else if(type==TokenType.MINUS) return 1;
        else if(type==TokenType.MUL) return 2;
        else if(type==TokenType.DIV) return 3;
        else if(type==TokenType.LT) return 4;
        else if(type==TokenType.LE) return 5;
        else if(type==TokenType.GT) return 6;
        else if(type==TokenType.GE) return 7;
        else if(type==TokenType.EQ) return 8;
        else if(type==TokenType.NEQ ) return 9;
        else if(type==TokenType.L_PAREN) return 10;
        else if(type==TokenType.R_PAREN) return 11;
        else if(type==TokenType.FAN) return 12;
        else return 13;
    }
    /**操作符的操作指令入栈**/
    public static void OpInstruction(TokenType type, List<Instruction> instructionsList,String calculate) {
        Instruction ins;
        switch (type) {
            case PLUS:
                if(calculate.equals("int")){
                    ins = new Instruction(Operation.add_i,0x20,-1);
                    instructionsList.add(ins);
                    break;
                }
                else{
                    ins = new Instruction(Operation.add_f,0x24,-1);
                    instructionsList.add(ins);
                    break;
                }

            case MINUS:
                if(calculate.equals("int")){
                    ins = new Instruction(Operation.sub_i,0x21,-1);
                    instructionsList.add(ins);
                    break;
                }
                else{
                    ins = new Instruction(Operation.sub_f,0x25,-1);
                    instructionsList.add(ins);
                    break;
                }
            case MUL:
                if(calculate.equals("int")){
                    ins = new Instruction(Operation.mul_i,0x22,-1);
                    instructionsList.add(ins);
                    break;
                }
                else{
                    ins = new Instruction(Operation.mul_f,0x26,-1);
                    instructionsList.add(ins);
                    break;
                }

            case DIV:
                if(calculate.equals("int")){
                    ins = new Instruction(Operation.div_i,0x23,-1);
                    instructionsList.add(ins);
                    break;
                }
                else{
                    ins = new Instruction(Operation.div_f,0x27,-1);
                    instructionsList.add(ins);
                    break;
                }
                //等于,
            case EQ:
                if(calculate.equals("int")) ins = new Instruction(Operation.cmp_i,0x30,-1);
                else  ins = new Instruction(Operation.cmp_f,0x32,-1);
                instructionsList.add(ins);
                //等于则压进去的值是0，要变为1
                ins = new Instruction(Operation.not,0x2e,-1);
                instructionsList.add(ins);
                break;
            case NEQ:
                if(calculate.equals("int")) ins = new Instruction(Operation.cmp_i,0x30,-1);
                else  ins = new Instruction(Operation.cmp_f,0x32,-1);
                instructionsList.add(ins);
                break;
            case LT:
                if(calculate.equals("int")) ins = new Instruction(Operation.cmp_i,0x30,-1);
                else  ins = new Instruction(Operation.cmp_f,0x32,-1);
                instructionsList.add(ins);
                ins = new Instruction(Operation.set_lt,0x39,-1);
                instructionsList.add(ins);
                break;
            case LE:
                if(calculate.equals("int")) ins = new Instruction(Operation.cmp_i,0x30,-1);
                else  ins = new Instruction(Operation.cmp_f,0x32,-1);
                instructionsList.add(ins);
                //不大于，小于等于
                ins = new Instruction(Operation.set_gt,0x3a,-1);
                instructionsList.add(ins);
                ins = new Instruction(Operation.not,0x2e,-1);
                instructionsList.add(ins);
                break;
            case GT:
                if(calculate.equals("int")) ins = new Instruction(Operation.cmp_i,0x30,-1);
                else  ins = new Instruction(Operation.cmp_f,0x32,-1);
                instructionsList.add(ins);
                ins = new Instruction(Operation.set_gt,0x3a,-1);
                instructionsList.add(ins);
                break;
            case GE:
                if(calculate.equals("int")) ins = new Instruction(Operation.cmp_i,0x30,-1);
                else  ins = new Instruction(Operation.cmp_f,0x32,-1);
                instructionsList.add(ins);
                //不小于，大于等于
                ins = new Instruction(Operation.set_lt,0x39,-1);
                instructionsList.add(ins);
                ins = new Instruction(Operation.not,0x2e,-1);
                instructionsList.add(ins);
                break;
            case FAN:
                if(calculate.equals("int")) ins = new Instruction(Operation.neg_i,0x34,-1);
                else  ins = new Instruction(Operation.neg_f,0x35,-1);
                instructionsList.add(ins);
                break;
            default:
                break;
        }

    }
}
