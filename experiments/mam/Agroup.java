import java.io.*;

public class Agroup implements Serializable {

    public static final int BRIGHT = 1;
    public static final int DARK = 2;
    public static final int POOR = 3;
    public static final int AVER = 4;
    public static final int GOOD = 5;
    public static final int NIGHT = 6;
    public static final int TWI = 7;
    public static final int USAB = 8;

    public Agroup() {}

    public String name;

    public double ra;

    public double dec;

    public String id;

    public Timing timing;

    public int seecat;

    public int lunar;

    public int tod;

    public int priority;

    public long xt;

    public int rn;

    public long last;

    public String toString() {
	
	StringBuffer buff = new StringBuffer();

	buff.append(id);

	buff.append("  ");
	buff.append(name);
	
	buff.append("  ");
	switch (seecat) {
	case POOR:
	    buff.append("P");
	    break;
	case AVER:
	    buff.append("A");
	    break;
	case GOOD:
	    buff.append("X");
	    break;
	case USAB:
	    buff.append("USAB");
	    break;
	}

	buff.append("  ");
	switch (tod) {
	case NIGHT:
	    buff.append("N");
		break;
	case TWI:
	    buff.append("T");
	    break;
	}

	buff.append("  ");
	switch (lunar) {
	case DARK:
	    buff.append("D");
	    break;
	case BRIGHT:
	    buff.append("B");
	    break;
	}
	
	buff.append("  ");
	buff.append(timing.toString());

	buff.append("  ");
	buff.append(""+priority);


	return buff.toString();
    }

}
