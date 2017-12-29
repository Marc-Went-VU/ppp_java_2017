package ida.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ida.ipl.extra.Arguments;
import ida.ipl.extra.consts.Constants;

class Ida {
	
	public final Ibis ibis;

	Ida() throws IbisCreationFailedException {
		ibis = IbisFactory.createIbis(
				Constants.IBIS_CAPABILITIES, 
				null,
				Constants.RECEIVE_PORT_TYPE, 
				Constants.SEND_PORT_TYPE
		);
	}

	void run(String[] args) throws Exception {
		Arguments arg = new Arguments(args);
		// Elect a master
		IbisIdentifier master = ibis.registry().elect(Constants.MASTER_IDENTIFIER);

		arg.setMasterIdentifier(master);
		arg.setMyIdentifier(ibis.identifier());
		
		// If I am the master, run master, else run worker
		if (master.equals(ibis.identifier())) {
			new Master(ibis, arg);
		} else {
			new Slave(ibis, arg);
		}

		// End ibis
		ibis.end();
	}

	

	public static void main(String[] args) {
		try {
			new Ida().run(args);
		} catch (IbisCreationFailedException e) {
			e.printStackTrace();
		} catch (Exception e) {
 			e.printStackTrace();
		}
	}

	
}
