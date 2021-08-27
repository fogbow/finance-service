package cloud.fogbow.fs.api.http.response;

import io.swagger.annotations.ApiModelProperty;

public class Version {
    @ApiModelProperty(example = "v.3.0.0-ras-ab4684b8e-as-f87023f-common-4d0eb1f-fs-3ddef3g")
    private String version;

    public Version() {}

    public Version(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
