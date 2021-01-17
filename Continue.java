public class Continue{
    Instruction instruction;
    int position;
    int whileId;

    public Continue(Instruction instruction,int position, int whileId){
        this.instruction=instruction;
        this.position = position;
        this.whileId = whileId;
    }

    public int getWhileId() {
        return whileId;
    }
}
