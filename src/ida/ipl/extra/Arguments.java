package ida.ipl.extra;

import ibis.ipl.IbisIdentifier;

public class Arguments {
	private String fileName = null;
	private boolean cache = true;
	
	/* Use suitable default value. */
	private int length = 103;
	
	private IbisIdentifier masterIdentifier;
	private IbisIdentifier myIdentifier;
	
	private int print;

	public Arguments(String[] args) {
		this.print = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--file")) {
				fileName = args[++i];
			} else if (args[i].equals("--nocache")) {
				cache = false;
			} else if (args[i].equals("--length")) {
				i++;
				length = Integer.parseInt(args[i]);
			} else if (args[i].equals("--slavePrint")) {
				this.print = 2;
			} else if (args[i].equals("--masterPrint")) {
				this.print = 1;
			} else {
				System.err.println("No such option: " + args[i]);
				System.exit(1);
			}
		}
	}

	public String getFileName() {
		return fileName;
	}

	public boolean useCache() {
		return cache;
	}

	public int getLength() {
		return length;
	}

	public IbisIdentifier getMasterIdentifier() {
		return masterIdentifier;
	}

	public void setMasterIdentifier(IbisIdentifier masterIdentifier) {
		this.masterIdentifier = masterIdentifier;
	}

	public IbisIdentifier getMyIdentifier() {
		return myIdentifier;
	}

	public void setMyIdentifier(IbisIdentifier myIdentifier) {
		this.myIdentifier = myIdentifier;
	}
	
	public boolean canPrint() {
		boolean masterPrint = this.print >= 1 && this.isMaster();
		boolean slavePrint = this.print == 2;
		return masterPrint || slavePrint;
	}

	public boolean isMaster() {
		return this.myIdentifier != null && this.masterIdentifier != null && 
				this.myIdentifier.name().equals(this.masterIdentifier.name());
	}
	
}
