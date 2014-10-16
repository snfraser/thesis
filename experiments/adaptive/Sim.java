import java.util.*;
import ngat.util.*;

public class Sim {

    final long NIGHT = 12*3600*1000;

    //int[] ta = new int[] {15, 30, 60, 120, 180, 240, 360, 480}; //\minutes

    int[] ta = new int[100];

    double[] qa = new double[] {0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0};

    double[] ha = new double[] {0.25, 0.5, 1.0, 2.0, 4.0}; // hours

    int ntrials = 10;

    int nseq = 500;

    Group[] groups;

    public Sim() {
	
 	groups = new Group[500];
	
	for (int k = 0; k < 100; k++) {
	    ta[k] = 5*(k+1);
	}

	int[] cs = new int[4];

	for (int  i = 0; i < 500; i++) {
	    
	    double p = Math.random()*5.0;
	    long st = (long)(Math.random()*(double)NIGHT);
	    long lt = (long)(Math.random()*110*60*1000) + 110*60*1000;
	    long xt = (long)(Math.random()*25*60*1000) + 5*60*1000;
	    
	    // select s-cat for group - equally distributed in 4 cats
	    int s = 0;
	    double ss = Math.random()*100;
	    if (ss < 25)
		s = 0;
	    else if
		(ss < 50)
		s = 1;
	    else if
		(ss < 75)
		s = 2;
	    else
		s = 3;
	    
	    cs[s]++; // one more in this cat

	    Group g = new Group(p, st, st+lt);
	    g.xt = xt;
	    g.s = s;
	    
	    groups[i] = g;	    

	}
	
	// display the distribution to check its even
	for (int c = 0; c < 4; c++) {
	    System.err.println("RS: "+c+" -> "+cs[c]);
	}
	
    }
    

    public void run() throws Exception {

	// BDS
	for (int ik = 0; ik < ntrials; ik++) {
	    despatch();
	} // next trial
	

	// QLAS
	for (int it = 0; it < ta.length; it++) {
	    
	    long tau = (long)(ta[it]*60000);
	    
	    //System.err.println("QLAS: tau="+(tau/60000));
	    Seeing seeing = new Seeing(tau);


	    for (int ih = 0; ih < ha.length; ih++) {
		
		long h = (long)(ha[ih]*3600000);
		
		//System.err.println("QLAS: h="+(h/1000)+"s");

		PopulationStatistics stats = new PopulationStatistics();
		

		for (int ik = 0; ik < ntrials; ik++) {
		    
		    System.err.println("QLAS: tau: "+(tau/60000)+", h: "+(h/1000)+"s, trial: "+ik);
		    
		    double ascore = qlas(h, seeing);

		    stats.addSample(ascore);

		} // next trial

		
		System.err.printf("STAT: Tau: %4.2f H: %4.2f Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", 
				  (double)tau/60000,
				  (double)h/1000,
				  stats.getAverage(), 
				  stats.getMaximum(), 
				  stats.getMinimum(), 
				  stats.getStandardDeviation());
		
		
		
	    } // next h
	} // next tau

	
	// ALAS
	for (int it = 0; it < ta.length; it++) {
	    
	    long tau = (long)ta[it]*60000;
	    
	    for (int iq = 0; iq < qa.length; iq++) {

		double q = (double)qa[iq];

		for (int ik = 0; ik < ntrials; ik++) {
			    
		    alas(q, tau);

		} // next trial
	    } // next q
	} // next tau

    }

    private void despatch() throws Exception {

	Seeing seeing = new Seeing(15*60*1000); // seeing at 30 minutes changes.

	//seeing.print();

	clearGroupHistory();
	clearGroupTempHistory();

	double score = 0.0;
	
	long t = 0;
	while (t < NIGHT){
	    
	    int s = seeing.at(t);
	    //System.err.println("Seeing at: "+(t/60000)+" -> "+s);
	    // find a group
	    Group g = despatchGroup(t, s);
	    if (g == null) {
		t += 60*1000L;
	    } else {
		//System.err.println("Exec: "+g+" at: "+(t/60000));
		score += scoreGroup(g, t, s);
		t += g.xt;
	    }
	    
	}

	System.err.println("Score: "+score);

    }

    private Group despatchGroup(long t, int s) {
	double bs = -99.99;
	Group best = null;
	int ng = 0;
	//System.err.println("Test groups at: "+(t/60000));
	for (int j = 0; j < groups.length; j++) {
		
	    if (cando(groups[j], t, s)) {
		double gs = scoreGroup(groups[j], t, s);
		if (gs > bs) {
		    bs = gs;
		    best = groups[j];
		}
		ng++;
	    }
	}

	//System.err.println("Found: "+ng+" groups");
	return best;// maybe null
    }

    private double qlas(long h, Seeing seeing) {

	//	Seeing seeing = new Seeing(tau); // seeing at 30 minutes changes.

	clearGroupHistory();
	
	double score = 0.0;
		
	long t = 0;
	while (t < NIGHT){
	    
	    // we will use this value for the whole sequence
	    int s = seeing.at(t);
	    
	    Sequence seq = findBestSequence(t, h, s);
	    score += execSequence(seq, t, seeing);

	    t += h;

	}

	System.err.println("Score: tau: "+(seeing.tau/60000)+" h: "+(h/1000)+"s, ->"+(double)score/(double)NIGHT);

	return (double)score/(double)NIGHT;

    }

    private Sequence findBestSequence(long ts, long h, int s) {

	//System.err.println("Findbestseq: at:"+(ts/60000)+" H:"+(h/60000)+", with seeing="+s);
	
	Sequence best = new Sequence();
	Sequence trial = new Sequence();

	for (int in = 0; in < nseq; in++) {
	    //System.err.println("generate trial sequence: "+in);

	    trial.clear();
	    clearGroupTempHistory();

	    long t = ts;
	    //System.err.println("enter loop: t: "+(t/60000)+" from"+(ts/60000)+" upto: "+((ts+h)/60000));
	    while (t < ts + h) {

		List candidates = new Vector(); // store candidates for timestep
		//System.err.println("Test "+groups.length+" groups");
		for (int ig = 0; ig < groups.length; ig++) {
		    Group g = groups[ig];
		    if (maybedo(g, t, ts+h-t, s)) {
			candidates.add(g);
		    }
		}

		// got list of candidates, pick one
		if (candidates.size() == 0) {
		    // none, use a bg obs
		    // System.err.println("NO candidates at: "+(t/60000));
		    trial.add(null);
		    t += 60000; // 1 minute bg obs

		} else {
		    // pick one at random
		    //System.err.println("candidates at: "+(t/60000)+" "+candidates.size());

		    int ii = (int)Math.floor(Math.random()*(double)candidates.size());
		    Group g = (Group)candidates.get(ii);
		    trial.add(g);
		    trial.addScore(scoreGroup(g, t, s)*(double)g.xt);
		    g.thist = true; // set temp history
		    t += g.xt;
		}

		//System.err.println("Trial: "+in+" Score="+trial.score);
		// trial sequence completed
		if (trial.score > best.score) {
		    best.score = trial.score;
		    best.copyGroups(trial);
		}

	    }

	}

	return best;

    }

    private double execSequence(Sequence seq, long ts, Seeing seeing) {

	double ascore = 0.0;
	double pscore = 0.0;

	int ss = seeing.at(ts);

	long t = ts;
	Iterator is = seq.list.iterator();
	while (is.hasNext()) {

	    Group g = (Group)is.next();
	    if (g == null) {
		t += 60000;
		seq.addbgtime(60000);
	    } else {
		int as = seeing.at(t);
		pscore += scoreGroup(g, t, ss)*g.xt;

		// score against actual seeing at t
		ascore += scoreGroup(g, t, as)*g.xt;

		if (as < g.s)
		    seq.addbstime(g.xt);

		t += g.xt;
	    }

	}
	
	System.err.println("Exec score: seqlen: "+seq.list.size()+", seeing: "+ss+", PS="+seq.score+" AS="+ascore+
			   " BG="+(seq.bgtime/60000)+" BS="+(seq.bstime/60000));

	//return ascore;
	return pscore;
    }


    private void alas(double q, long tau) {

    }


    private boolean cando(Group g, long t, int s) {
	if (t < g.start || t > g.end)
	    return false;
	if (s < g.s)
	    return false;
	if (g.hist)
	    return false;
	
	return true;
    }

    private boolean maybedo(Group g, long t, long maxtime, int s) {
	if (t < g.start || t > g.end)
	    return false;
	if (s < g.s)
	    return false;
	if (g.hist)
	    return false;
	if (g.thist)
	    return false;
	if (g.xt > maxtime)
	    return false;
	return true;
    }



    private double scoreGroup(Group g, long t, int s) {
	
	long tmid = (long)((double)(g.start + g.end)/2.0);

	double ps = g.p * (1 - Math.abs(t - tmid)/(double)(tmid-g.start))/5.0;

	double ss = 0.0;
	if (s == g.s)
	    ss = 1.0;
	else if (s > g.s)
	    ss = 1.0/(1.0 + (s - g.s));
	else
	    ss = 0.0;
	//	    ss = -1.0/(5.0 - (g.s - s));
	//System.err.println("SS: "+ss+" PS: "+ps);
	return 0.5*ps + 0.5*ss;
	
    }

    private void clearGroupHistory() {
	for (int i = 0; i < groups.length; i++) {
	    groups[i].hist = false;
	}
    }

    private void clearGroupTempHistory() {
	for (int i = 0; i < groups.length; i++) {
	    groups[i].thist = false;
	}
    }


    public static void main(String args[]) {

	try {
	    Sim sim = new Sim();
	    sim.run();
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
