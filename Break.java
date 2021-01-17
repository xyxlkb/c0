public class Break {
    Instruction instruction;
    int position;
    int whileId;

    public Break(Instruction instruction,int position, int whileId){
        this.instruction=instruction;
        this.position = position;
        this.whileId = whileId;
    }

    public int getWhileId() {
        return whileId;
    }
}
