import java.io.*;

public class Timing implements Serializable {

    public static final int MON = 1;

    public static final int INT = 2;
    
    public static final int FLEX = 3;
    
    public int type;

    public double period; // hours

    public Timing() {}

    public Timing(int type, double p) {
	this();
	this.type = type;
	this.period = p;
    }

    public String toString() {
	switch (type) {
	case MON:
	    return "MON("+period+")";
	case FLEX:
	    return "FLEX";
	case INT:
	    return "INT("+period+")";
	}
	return "UNK("+type+")";
    }
}
