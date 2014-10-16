import ngat.util.*;

public class DisSim {

    public static void main(String args[]) {

	try {

	    DisSim sim = new DisSim();
	    sim.run();

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    public DisSim() {

    }

    public void run() throws Exception {

	int[] bb = new int[] {24, 12, 6, 4, 3, 2};

	double[] hh = new double[] {0.5, 1.0, 2.0, 3.0, 4.0, 6.0};

	// check each horizon length
	for (int ih = 0; ih < hh.length; ih++) {

	    double h = hh[ih]; 
	    int nb = bb[ih];

	    System.err.println("Horizon "+h+"h using "+nb+" buckets");

	    double[] b = new double[nb];
	    
	    double score0 = q(h)*11.0/12.0; // no-disruptor score is scaled 11/12 to match time lost in dis sims.
	    System.err.printf("TEST H %4.2f N  0 -> %6.2f 0.0\n",
				  h, score0 );
	    // split disruption into chunks 1-12
	    for (int in = 1; in <= 12; in++) {

		int jn = in;

		double dt = 1/(double)in;

		if (dt >= h) {
		    // the disruptor spans more than one horizon slot
		    dt /=2;
		    jn = 2*in;
		}

		System.err.println("Test "+jn+" disruptions of length "+(60*dt)+"m");

		// calculate offset for this series (n at h)
		double offset = (Math.random() - 0.5); // +-2
	
		// do a load of trials
		PopulationStatistics stats = new PopulationStatistics();
		for (int it = 0; it < 1000; it++) {

		    clearBuckets(b);
		    
		    // create jn disruptors and place these
		    for (int id = 0; id < jn; id++) {

			// create a disruptor and place it, max in any bucket
			double dd = -1.0;
			boolean disok = false;
			while (!disok) {			    
			    dd = Math.random()*12.0; // random in 12 hour period
			    int ib = (int)Math.floor(dd/h); // which bucket
			    double cb = b[ib]; // what is the count in this bucket
			    if (cb < h) {
				disok = true;
				b[ib] += dt;
				System.err.println("Create disruptor at "+dd+
						   ", Count for bucket "+ib+
						   " of "+nb+" is "+b[ib]); 
			    }
			}
		
		    } // next disruptor

		    // calculate score for each horizon slot, there are nb of these
		    System.err.println("Calculate score for "+nb+" slots of length "+h+"h");
		    double score = q(h);
		    for (int is = 0; is < nb; is++) {
			double tdis = b[is];
			double tok = h - tdis;
			//			System.err.println("Slot "+is+" of "+nb+" dis "+(tdis*60)+"m, ok "+(60*tok)+"m");
			
			double scorefrag = score/(double)nb;
			double disfrac = tdis/h; 
			double subscore = scorefrag*g(disfrac);

			System.err.println("Slot "+is+" of "+nb+" dis "+(tdis*60)+"m, ok "+(60*tok)+
					   "m, sfrag "+scorefrag+" disfrac "+disfrac+", subtract "+subscore);

			score -= subscore;
		    }

		    System.err.println("Trial score: "+score);
		    stats.addSample(score+offset);

		} // next trial
		System.err.printf("TEST H %4.2f N %4d -> %6.2f %6.2f\n",
				  h, in, stats.getAverage(), stats.getStandardDeviation());
		
	    } // next n

	} // next horizon size

    }

    /** Clear a bucket array.*/
    private void clearBuckets(double[] b) {	
	for (int k = 0; k < b.length; k++) {
	    b[k] = 0.0;
	}	
    }

    private double q(double h) {
	if (h <= 0.5)
	    return 128.5;
	else if (h <= 1.0)
	    return 134.0;
	else if
	    (h <= 2.0)
	    return 141.0;
	else if
	    (h <= 3.0)
	    return 146.0;
	else if
	    (h <= 4.0)
	    return 148.0;
	else if
	    (h <= 5.0)
	    return 150.0;
	else 
	    return 151.0;	
    }

    private double g(double frac) {
	double act = 1.0 - Math.exp(-4.0*frac);
	return act;//*(Math.random()+0.5)*0.15;
    }

}

