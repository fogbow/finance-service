package cloud.fogbow.fs.api.http.response;

// TODO change name to Version
public class VersionResponse {
    // TODO documentation
    private String version;

    public VersionResponse() {}

    public VersionResponse(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
