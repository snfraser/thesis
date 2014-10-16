// Seeing scenario
public class Seeing {

    public static final int U = 0;
    public static final int P = 1;
    public static final int A = 2;
    public static final int X = 3;

    long[] change = new long[1000];
    int [] seecat = new int[1000];
    
    int nc;
    public long tau;

    public Seeing(long tt) {
	this.tau = tt;
	int scat = 1;
	long t = 0;
	int it = 0;
	while (t < 12*3600*1000) {
	    
	    double rr = Math.random();
	    long dt = -(long)((double)tau*Math.log(1-rr));
	    t += dt;
	    change[it] = t;
	    seecat[it] = select(scat);
	    scat = seecat[it];
	    it++;

	}

	nc = it;

    }
    
    public void print() {
	
	for (int i = 0; i < nc; i++) {

	    System.err.println("C:"+i+" -> "+((double)change[i]/60000.0)+" "+seecat[i]);

	}
	
    }

    private int select(int scat) {

	// choose anything but not scat;

	int kk = (int)(Math.random()*10)+10;
	
	int k = 0;
	int tcat = scat;
	while (k < kk && tcat == scat) {
	    int cat = (int)(Math.random()*100);
	    if (cat < 25)
		tcat = 0;
	    else if
		(cat < 50)
		tcat = 1;
	    else if
		(cat < 75)
		tcat = 2;
	    else
		tcat = 3;

	    k++;
	}
	return tcat;
    }

    public int at(long t) {
	//System.err.println("Find seeing at: "+(t/60000));
	if (t < 0)
	    return 0;
	
	if (t > change[nc-1])
	    return 0;

	for (int i = 0; i < nc-1; i++) {	    
	    if (change[i] <= t && t <= change[i+1]) {
		//System.err.println("Find block: "+i+", "+(i+1)+" -> "+seecat[i]);
		return seecat[i];
	    }
	}
	return 0;
    }

    public long size(long t) {
	//System.err.println("Find seeing at: "+(t/60000));
	if (t < 0)
	    return 0;
	
	if (t > change[nc-1])
	    return 0;

	for (int i = 0; i < nc-1; i++) {	    
	    if (change[i] <= t && t <= change[i+1]) {
		//System.err.println("Find block: "+i+", "+(i+1)+" -> "+seecat[i]);
		return change[i+1] - change[i];
	    }
	}
	return 0;

    }

    public static void main(String args[]) {
	
	long xt = Integer.parseInt(args[0])*60000;
    
	Seeing s = new Seeing(xt);

	s.print();


    }


}
