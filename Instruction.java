import java.util.Objects;

public class Instruction {
    private Operation opt;
    //指令的十六进制编号
    int out;
    long x;

    public Instruction(Operation opt,int out,long x) {
        this.opt = opt;
        this.out=out;
        this.x = x;
    }


    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    @Override
    public String toString() {
            return "" + opt + " " +out+" "+ x + '\n';
    }

}
