package cloud.fogbow.fs.core.util.accounting;

public class NetworkSpec extends OrderSpec {

    private String cidr;
    private String allocationMode;

    public NetworkSpec(String cidr, String allocationMode) {
        this.cidr = cidr;
        this.allocationMode = allocationMode;
    }

    public NetworkSpec() {}

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getAllocationMode() {
        return allocationMode;
    }

    public void setAllocationMode(String allocationMode) {
        this.allocationMode = allocationMode;
    }

    @Override
    public String toString() {
        return "Resource: Network, Cidr: " + this.cidr + "Allocation mode: " + this.allocationMode;
    }
}
