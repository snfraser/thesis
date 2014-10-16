import java.util.*;

public class Sequence {

    public Vector list;

    public double score = 0.0;

    public long xtime;

    public long bgtime;

    public long bstime;

    public Sequence() {
	list = new Vector();
    }

    public void add(Group g) {
	list.add(g);
    }

    public void addScore(double s) {
	score += s;
    }

    public void addxtime(long s) {
	xtime += s;
    }

    public void addbgtime(long s) {
	bgtime += s;
    }

    public void addbstime(long s) {
	bstime += s;
    }

    public void append(Sequence other) {
	Iterator ig = other.list.iterator();
	while (ig.hasNext()) {
	    Group g = (Group)ig.next();
	    list.add(g);
	}	
    }

    public void clear() {
	list.clear();
	score = 0.0;
	bgtime = 0;
	bstime = 0;
	xtime = 0;
    }

    public void copyGroups(Sequence other) {
	list.clear();
	Iterator ig = other.list.iterator();
	while (ig.hasNext()) {
	    Group g = (Group)ig.next();
	    list.add(g);
	}
    }

}
