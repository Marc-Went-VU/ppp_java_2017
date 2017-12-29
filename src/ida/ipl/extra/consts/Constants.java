package ida.ipl.extra.consts;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.PortType;

public class Constants {
	public static final PortType RECEIVE_PORT_TYPE = new PortType(
			PortType.COMMUNICATION_RELIABLE, 
			PortType.SERIALIZATION_OBJECT_IBIS,
			PortType.RECEIVE_AUTO_UPCALLS, 
			PortType.CONNECTION_MANY_TO_ONE,
			PortType.CONNECTION_UPCALLS);
	
	public static final PortType SEND_PORT_TYPE = new PortType(
			PortType.COMMUNICATION_RELIABLE, 
			PortType.SERIALIZATION_OBJECT_IBIS,
			PortType.CONNECTION_MANY_TO_ONE,
			PortType.CONNECTION_UPCALLS,
			PortType.RECEIVE_AUTO_UPCALLS);

	public static final IbisCapabilities IBIS_CAPABILITIES = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT, 
			IbisCapabilities.TERMINATION);

	public static final String MASTER_IDENTIFIER = "master";
	public static final String SLAVE_IDENTIFIER = "slave";
	
	public static final int MAX_MASTER_STEPS = 3;
}
