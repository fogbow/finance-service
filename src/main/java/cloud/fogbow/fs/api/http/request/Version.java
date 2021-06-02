package cloud.fogbow.fs.api.http.request;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.fs.api.http.response.VersionResponse;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.util.BuildNumberHolder;

// TODO documentation
@CrossOrigin
@RestController
@RequestMapping(value = Version.ENDPOINT)
public class Version {

    public static final String ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "version";

    private final Logger LOGGER = Logger.getLogger(Version.class);

    // TODO documentation
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<VersionResponse> getVersion() {
        LOGGER.info(Messages.Log.RECEIVING_GET_VERSION);
        String buildNumber = BuildNumberHolder.getInstance().getBuildNumber();
        String versionNumber = SystemConstants.API_VERSION_NUMBER + "-" + buildNumber;
        return new ResponseEntity<VersionResponse>(new VersionResponse(versionNumber), HttpStatus.OK);
    }
}
