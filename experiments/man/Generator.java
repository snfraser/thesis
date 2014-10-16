import ngat.util.*;
import java.io.*;
import java.util.*;
import java.text.*;

public class Generator {

    public static void main(String args[]) {
	
	try {

	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

	    // read points into array

	    int nd = config.getIntValue("nd");
	    BufferedReader in = new BufferedReader(new FileReader(config.getProperty("base")));

	    double x[] = new double[nd];
	    double y[] = new double[nd];
	    //double s[] = new double[nd];
	    int ii = 0;
	    String line = null;
	    while ( (line = in.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(line);
		if (st.countTokens() == 0)
		    break;
		double xx = Double.parseDouble(st.nextToken());
		double yy = Double.parseDouble(st.nextToken());
		//double ss = Double.parseDouble(st.nextToken());
		x[ii] = xx;
		y[ii] = yy;
		//s[ii] = ss;
		ii++;
	    }

	    int np = config.getIntValue("np");

	    double sx = config.getDoubleValue("sx");
	    double sy = config.getDoubleValue("sy");

	    double h = config.getDoubleValue("h");

	    double ceil = config.getDoubleValue("ceiling");

	    GaussianRandom gx = new GaussianRandom(0.0, 0.5*sx);
	    GaussianRandom gy = new GaussianRandom(0.0, 0.5*sy);

	    // compute nd data points
	    for (int i = 0; i < np; i++ ){

		double xi = x[0] + (double)i*(x[nd-1] - x[0])/(double)np + gx.random();
		
		// which x_i are left and right of x

		for (int j = 0; j < nd-1; j++) {
		    
		    if ((x[j] < xi) && (x[j+1] >= xi)) {
			
			// here it is....
			double yi = y[j] + (xi - x[j])/(x[j+1] - x[j]) * (y[j+1] - y[j]) + gy.random();
			
			while (yi >= ceil)
			    yi = yi - gy.random();

			System.err.println(""+i+" "+xi+" "+yi);
			
		    }
		    
		}
		
		
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("Usage: Generator --nd <n in base> --base <basefile> --np <n results> --sx <sdev> --sy <sdev> --h <var> --ceiling <c>");
	}

    }

}
