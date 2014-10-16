public class EnvModel1 {

    private double tau;

    public EnvModel1(double tau) {
	this.tau = tau;
    }

    public double getPeriod() {

	double tt = -tau * Math.log(1.0 - Math.random());

	return tt;

    }

    public int nextSee(int see) {
	int newsee = see;
	while (newsee == see) {
	    newsee = (int) (Math.floor(Math.random()*4.0));
	}
	return newsee;
    }


    public static void main(String args[]) {
	
	try {
	    
	    double tau = Double.parseDouble(args[0]);

	    EnvModel1 env = new EnvModel1(tau); 

	    double avt = 0.0;
	    int nt = 0;
	    

	    int see = 0;
	    
	    double st = 0;
	    double t = st;
	    while (t < st + 7*86400*1000) {

		double dt = env.getPeriod();
		t += dt;

		int newsee = env.nextSee(see);
      
		see = newsee;

		avt += dt;
		nt++;


		System.err.println(""+(t/60000.0)+" "+(dt/1000.0)+" "+newsee);
		

	    }

	    System.err.println("#NT "+nt);
	    System.err.println("#AV "+(avt/(60000.0*(double)nt)));
	    

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }



}