public class Global {
    //全局变量的编号
    int id;
    Boolean is_const;
    int count;
    String items;

    public Global(int id,Boolean is_const){
        this.id=id;
        this.is_const=is_const;
    }

    public Global(int id,Boolean is_const,int count,String items){
        this.id=id;
        this.is_const=is_const;
        this.count=count;
        this.items=items;
    }

    @Override
    public String toString() {
        return "Global{" +
                "id=" + id +
                "is_Const=" + is_const +
                ", count=" + count +
                ", items=" + items +
                '}';
    }
}
