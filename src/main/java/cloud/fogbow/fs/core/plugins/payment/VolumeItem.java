package cloud.fogbow.fs.core.plugins.payment;

public class VolumeItem extends ResourceItem {
	public static final String ITEM_TYPE_NAME = "volume";
	
	private int size;

	public VolumeItem(int size) {
		this.size = size;
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + size;
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
		VolumeItem other = (VolumeItem) obj;
		if (size != other.size)
			return false;
		return true;
	}
}
