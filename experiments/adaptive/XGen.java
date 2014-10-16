public class XGen {

    static double hh[] = new double[] {0.5,  1.0,  2.0,  4.0,  6.0};
    
    static double a[] = new double[] {36, 65, 130, 250, 372};
    static double b[] = new double[] {88, 94, 101, 104, 103};
    static double c[] = new double[] {85, 82, 80, 77, 74, 70};
    
    public static void main(String args[]) {

	try {

	    // 1. Generate stability plot

	    for (int ih = 0; ih < hh.length; ih++) {
	    
		for (int it = 0; it < 480; it+=30) {

		    double t = (double)it;

		    // calculate score at t
		    double y = f(t, ih);
		
		    double dy = (Math.random()-0.5)*1.0;

		    double erry = 3.0 + (Math.random()-0.5)*3.0;

		    System.err.println("SCORE "+hh[ih]+" "+t+" "+(y+dy)+" "+erry);

    
		} // next minute t

	    } // next h


	    // 2. Generate reliability plot

	    // reliability over/under estimate
	    double q[] = new double[] {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 4.0};

	    for (int ih = 0; ih < hh.length; ih++) {

		for (int iq = 0; iq< q.length; iq++) {

		    double qq = q[iq];

		    // work out the f value for h if we overestimated t
		    double t = (double)ih;
		    double fo = f(t/qq, ih);

		    System.err.println("OVER "+ih+" "+qq+" "+fo);


		} // next q

	    } // next h

	} catch (Exception e ){
	    e.printStackTrace();
	}

    }

    private static double f(double t, int ih) {

	//double ah = 1.1h + 0.05;   // turning
	//double bh = 85 + 3*h;   // peak
	//double ch = 92 - 2*h; // start

	double ah = a[ih];
	double bh = b[ih];
	double ch = c[ih];

	//	double m1 = (b[ih]-c[ih])/a[ih];
	//double m2 = -0.02*m1;
	//double c2 = b[ih] - m2*a[ih];

	double m1 = (bh - ch)/ah;
	double m2 = -0.1*m1;
	double c2 = bh - m2*ah;

	double ff = -99;
	if (t <= ah)
	    ff =  m1*t+ch;
	else
	    ff = m2*t+c2;

	System.err.println("Calc: f "+t+", "+ih+" = "+ff);

	return ff;

    }

}