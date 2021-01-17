import java.util.ArrayList;
import java.util.List;
//生成最终代码
public class Target {
    //全局变量表
    List<Global> globalmap;
    //函数表
    List<Function> functionmap;
    //目标代码
    List<Byte> target=new ArrayList<>();

    public Target(List<Global> globalmap, List<Function> functionmap){
        this.globalmap = globalmap;
        this.functionmap= functionmap;
    }

    private void PutIn(int x,int length){
        for(int i = length-1 ; i >=0; i--){
            //每次得到前面的8位
            int last = x >> (i * 8) & 0xFF;
            target.add((byte) last);
        }
    }
    private void PutIn(long x,int length){
        for(int i = length-1 ; i >=0; i--){
            //每次得到前面的8位
            long last = x >> (i * 8) & 0xFF;
            target.add((byte) last);
        }
    }

    private void PutIn(int x){
        int length=4;
        for(int i = length-1 ; i >=0; i--){
            //每次得到前面的8位
            int last = x >> (i * 8) & 0xFF;
            target.add((byte) last);
        }
    }
    //方法重写
    private void PutIn(long x){
        int length=8;
        for(int i = length-1 ; i >=0; i--){
            //每次得到最前面的8位
            long last = x >> (i * 8) & 0xFF;
            target.add((byte) last);
        }
    }
    //方法重写
    private void PutIn(String x){
        for(int i = 0 ; i < x.length(); i++){
            char b = x.charAt(i);
            target.add((byte)b);
        }
    }
    //方法重写
    private void PutIn(Boolean x){
        if(x) target.add((byte)1);
        else target.add((byte)0);
    }

    public List<Byte> out(){
        int magic=0x72303b3e;
        int version=0x00000001;
        //magic
        PutIn(magic,4);
        //version
        PutIn(version,4);

        //globals.count
        PutIn(globalmap.size(),4);
        //全局变量表
        for(Global global:globalmap) outGlobal(global);

        //直接把_start放进函数表
        //functions.count
        PutIn(functionmap.size(),4);
        //放_start
        //outFunction(_start);

        //放其他函数
        for(Function fun:functionmap) outFunction(fun);
        return target;
    }

    public void outGlobal(Global global){
        //is_const
        PutIn(global.is_const);
        //2.value.count
        //3.value.items
        //全局
        if(global.items == null){
            PutIn(8,4);
            PutIn(0L,8);
        }
        //函数或者字符串常量
        else{
            PutIn(global.items.length());
            PutIn(global.items);
        }
    }
    public void outFunction(Function function){
        //name
        PutIn(function.name,4);
        //ret_slots
        PutIn(function.returnSlots,4);
        //param_slots
        PutIn(function.getParamSlots(),4);
        //loc_slots
        PutIn(function.localSlots,4);
        //body.count
        PutIn(function.getBody().size(),4);
        //body.items
        //函数体的指令集
        List<Instruction> ins = function.body;
        for(Instruction instruction:ins){
            //指令
            int op = instruction.out;
            PutIn(op,1);
            //操作数
            if(instruction.x!= -1){
                //只有push的操作数是64位
                if(op==1)
                    PutIn(instruction.getX(),8);
                else
                    PutIn(instruction.getX(),4);
            }
        }

    }

}
