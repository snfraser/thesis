public class Group {

    public long start;

    public long end;

    public long xt;

    public double p;

    public int s;

    public boolean hist = false;

    public boolean thist = false;

    public Group(double gp, long gstart, long gend) {
	this.p = gp;
	this.start = gstart;
	this.end = gend;	
    }

}
