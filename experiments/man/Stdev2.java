import java.io.*;
import java.util.*;

public class Stdev2 {
    
    public static void main(String args[]) {
	
	try {

	    BufferedReader bin = new BufferedReader(new FileReader(args[0]));

	    int nn = 0;
	    double sx = 0.0;
	    double sy = 0.0;
	    double s2x = 0.0;
	    double s2y = 0.0;
	    String line = null;
	    while ((line = bin.readLine()) != null) {
		System.err.println(line);
		nn++;
		StringTokenizer st = new StringTokenizer(line);
		st.nextToken();
		double x = Double.parseDouble(st.nextToken());
		double y = Double.parseDouble(st.nextToken());
		sx += x;
		s2x += x*x;
		sy += y;
		s2y += y*y;

	    }

	    double mux = sx/(double)(nn);
	    double sdx = Math.sqrt((s2x-sx*sx/(double)nn)/((double)(nn-1)));
	    double muy = sy/(double)(nn);
	    double sdy = Math.sqrt((s2y-sy*sy/(double)nn)/((double)(nn-1)));
	    
	    System.err.printf("Avx %4.2f +-(%6.4f) , Avy %4.2f +- (%6.4f) \n",
			      mux,sdx,muy,sdy);

	} catch (Exception e) { 
	    e.printStackTrace();
	}
    }

}
