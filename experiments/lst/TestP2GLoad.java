import ngat.phase2.*;
import ngat.oss.simulation.*;
import java.io.*;
import java.rmi.*;

public class TestP2GLoad {
    
    /** Test Load a P2 model.*/
    public static void main(String args[]) {
	
        try {
	    
            File file = new File(args[0]);
	    
            Phase2ModelGenerator gen = new Phase2ModelGenerator();
	    gen.loadPhase2Model(file);
            
            Naming.rebind("rmi://localhost/Test_Phase2ModelProvider", gen);

            System.err.println("PhaseModelGenerator provider bound to registry");

            // busy wait to keep alive
            while (true) {
                try{Thread.sleep(60000L);} catch (Exception e) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
