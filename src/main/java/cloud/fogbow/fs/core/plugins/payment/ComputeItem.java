package cloud.fogbow.fs.core.plugins.payment;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

public class ComputeItem extends ResourceItem {
	public static final String ITEM_TYPE_NAME = "compute";
	
	private int vCPU;
	private int ram;
	
	public ComputeItem(int vCPU, int ram) throws InvalidParameterException {
		setvCPU(vCPU);
		setRam(ram);
	}
	
	public int getvCPU() {
		return vCPU;
	}
	
	// TODO test
	public void setvCPU(int vCPU) throws InvalidParameterException {
		if (vCPU < 0) {
			throw new InvalidParameterException(Messages.Exception.NEGATIVE_COMPUTE_VCPU);
		}
		
		this.vCPU = vCPU;
	}
	
	public int getRam() {
		return ram;
	}
	
	// TODO test
	public void setRam(int ram) throws InvalidParameterException {
		if (ram < 0) {
			throw new InvalidParameterException(Messages.Exception.NEGATIVE_COMPUTE_RAM);
		}
		
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

	@Override
	public String toString() {
		return "{\"type\":\"compute\", \"vCPU\":" + vCPU + ", \"ram\":" + ram + "}";
	}
}
