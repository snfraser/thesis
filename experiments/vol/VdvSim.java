import ngat.util.*;

public class VdvSim {

    public static void main(String args[]) {

	final int[] pis = new int[] {0, 1, 2, 4, 8, 15, 30, 60, 90, 120, 150, 180, 210, 240, 300, 360};

	final double[] dq = new double[] {0.75, 0.86, 1.05, 1.23};

	try {

	    int ih = 0;
	    for (double dh = 0.5; dh <= 4; dh*=2) {

		//		System.err.println("Horizon: "+ih+"h");
		long h = (long)(dh*3600.0*1000.0);
		
		for (int ipi = 0; ipi < pis.length; ipi+=1) {

		    long pi = (long)pis[ipi]*60000L;

		    //  System.err.println("PI: "+(pi/60000)+"m");
		    
		    PopulationStatistics vstat = new PopulationStatistics();

		    for (int is = 0; is < 100; is++) {

			// sim run is
			double dv = 0.0; // record actual
			double dcv = 0.0; // record potential
			int nev = (int)(35.0 + Math.random()*10.0);

			for (int i = 0; i < nev; i++) {

			    long rho = (long)((30.0 + Math.random()*30.0)*60000.0);

			    //long rho = 30*60*1000L;

			    long t = (long)(Math.random()*10.0*3600.0*1000.0);

			    //System.err.println("T "+(t/60000)+", Pi: "+(pi/60000)+" R: "+(rho/60000));
			    
			    // +rho
			    if (t + pi > 36000000L)
				continue;

			    long dt = t - h*(long)Math.floor((double)t/(double)h);

			    // System.err.println("DT "+(dt/60000));
			    
			    double dsv =  0.2+Math.random()*0.1;
			    dcv += dsv;
			    
			    if (pi + rho + dt > h)
				dv += dsv;


			}

			vstat.addSample(dv);

		    } // next sim run

		    // print stats for H, pi
		    System.err.printf("H%4.2f %4d %6.2f %6.2f\n",
				      dh, (pi/60000), (vstat.getAverage()+(Math.random()-0.5)*dq[ih]), vstat.getStandardDeviation());
		    
		} 

		ih++;
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
