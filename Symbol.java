import java.util.ArrayList;
import java.util.List;

public class Symbol {
    //名字
    String name;
    /**类型
     * 函数，变量类型,int,double,fun
     */
    String type;
    //是否是常量
    boolean isConst;


    //都是-1表示是参数
    //局部变量id
    int Lid;

    //全局变量id
    int Gid;

    //参数的函数编号
    int funid;
    //参数编号
    int paraid;
    //参数列表？
    List<Symbol> param = new ArrayList<>();


    //返回值类型
    String back;
    //大小？
    //层数？
    int level;

    public Symbol(){}
    public Symbol(String name,String type,boolean isConst,int level,int Lid,int Gid,int funid,int paraid){
        this.name=name;
        this.type=type;
        this.isConst=isConst;
        this.level=level;
        this.Lid=Lid;
        this.Gid=Gid;
        this.funid=funid;
        this.paraid=paraid;
    }

    //函数构造如下
    public Symbol(String name,String type,List<Symbol> param,int level,String back){
        this.name=name;
        this.type=type;
        this.param=param;
        this.level=level;
        this.back=back;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public String getBack() {
        return back;
    }
}