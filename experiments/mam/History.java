public class History {

    public Agroup group;

    public long saveLast;

    public History() {}

    public History(Agroup group) {
	this();
	this.group = group;
	this.saveLast = group.last;
    }

    /** Restore the group's previously saved history.*/
    public void restoreGroupHistory() {
	group.last = saveLast;
    }

}