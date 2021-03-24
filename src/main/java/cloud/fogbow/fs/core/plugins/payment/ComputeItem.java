package cloud.fogbow.fs.core.plugins.payment;

public class ComputeItem extends ResourceItem {
	private int vCPU;
	private int ram;
	
	public ComputeItem(int vCPU, int ram) {
		this.vCPU = vCPU;
		this.ram = ram;
	}
	
	public int getvCPU() {
		return vCPU;
	}
	
	public void setvCPU(int vCPU) {
		this.vCPU = vCPU;
	}
	
	public int getRam() {
		return ram;
	}
	
	public void setRam(int ram) {
		this.ram = ram;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ram;
		result = prime * result + vCPU;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComputeItem other = (ComputeItem) obj;
		if (ram != other.ram)
			return false;
		if (vCPU != other.vCPU)
			return false;
		return true;
	}
}
