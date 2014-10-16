public class EnvStats {

    public static void main(String args[]) {

	    try {
	    
	    double tau = Double.parseDouble(args[0]);

	    EnvModel1 env = new EnvModel1(tau); 

	    double avt = 0.0;
	    int nt = 0;

	    // collect distribution	    
	    int[] count = new int[100];
	    for (int i = 0; i < 100; i++) {
		count[i] = 0;
	    }
	    int overflow = 0;
	    

	    int see = 0;
	    
	    double st = 0;
	    double t = st;
	    while (t < st + 100.0*86400.0*1000.0) {

		double dt = env.getPeriod();
		t += dt;

		int newsee = env.nextSee(see);
      
		see = newsee;

		avt += dt;
		nt++;

		// which bin ?
		double bin = 100.0*(dt/(10.0*tau));
		int ibin = (int)bin;
		if (bin >= 100)
		    overflow++;
		else
		    count[ibin]++;

	    }

	    System.err.println("#NT "+nt);
	    System.err.println("#AV "+(avt/(60000.0*(double)nt)));	    
	    System.err.println("#OVR "+((double)overflow/(double)nt));
	    
	    double cum = 0.0;
	    double cact = 0.0;
	    for (int i = 0; i < 100; i++) {
		double time = 0.1*(double)i/(60000.0*tau);
		double rel = (double)count[i]/(double)nt;
		double actual = (double)count[i]*time; // actual amount of time spent in stable period of this length...
		cum += rel;
		cact += actual;
		System.err.println(""+time+" "+rel+" "+cum+" "+actual+" "+cact);

	    }

	    
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
