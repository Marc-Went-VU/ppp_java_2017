package ida.ipl.lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ibis.ipl.IbisIdentifier;

public class SlaveManager {

	private ConcurrentHashMap<IbisIdentifier, SlaveConnection> slaves;
	
	private ArrayList<IbisIdentifier> working_slaves;
	
	public SlaveManager() {
		this.slaves = new ConcurrentHashMap<IbisIdentifier, SlaveConnection>();
		this.working_slaves = new ArrayList<IbisIdentifier>();
	}
	
	public void addSlave(IbisIdentifier slave, SlaveConnection connection) {
		this.slaves.put(slave, connection);
	}
	
	public void finish() {
		for(IbisIdentifier i : this.slaves.keySet()) {
			this.closeIdentifier(i);
		}
	}
	
	public boolean slavesWorking() {
		return this.working_slaves.size() > 0;
	}
	
	public int workingSlaves() {
		return this.working_slaves.size();
	}
	public void closeIdentifier(IbisIdentifier identifier) {
		if(identifier == null || !this.slaves.contains(identifier)) return;
		try {
			this.slaves.remove(identifier).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Set<IbisIdentifier> getIdentifiers() {
		return this.slaves.keySet();
	}
	
	public SlaveConnection getConnection(IbisIdentifier identifier) {
		return this.slaves.get(identifier);
	}
	
	public void slaveStartsWorking(IbisIdentifier slave) {
		if(this.slaves.containsKey(slave) && !this.working_slaves.contains(slave)) {
			this.working_slaves.add(slave);
		}
	}
	
	public void slaveDoneWorking(IbisIdentifier slave) {
		if(this.slaves.containsKey(slave)) {
			this.working_slaves.remove(slave);
		}
	}
}
