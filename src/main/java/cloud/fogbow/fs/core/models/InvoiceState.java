package cloud.fogbow.fs.core.models;

public enum InvoiceState {
	PAID("PAID"),
	WAITING("WAITING"),
	DEFAULTING("DEFAULTING");

	private String value;
	
	private InvoiceState(String value) {
		this.value = value;
	}
	
	public static InvoiceState fromValue(String value) {
		for (InvoiceState state: InvoiceState.values()) {
			if (state.value.equals(value)) {
				return state;
			}
		}
		
		// FIXME treat this
		return null;
	}
}
