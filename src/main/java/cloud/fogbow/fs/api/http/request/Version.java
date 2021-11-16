package cloud.fogbow.fs.api.http.request;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.constants.ApiDocumentation;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.util.BuildNumberHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
@RequestMapping(value = Version.ENDPOINT)
@Api(description = ApiDocumentation.Version.API)
public class Version {

    public static final String ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "version";

    private final Logger LOGGER = Logger.getLogger(Version.class);

    @ApiOperation(value = ApiDocumentation.Version.GET_OPERATION)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<cloud.fogbow.fs.api.http.response.Version> getVersion() {
        LOGGER.info(Messages.Log.RECEIVING_GET_VERSION);
        String buildNumber = BuildNumberHolder.getInstance().getBuildNumber();
        String versionNumber = SystemConstants.API_VERSION_NUMBER + "-" + buildNumber;
        return new ResponseEntity<cloud.fogbow.fs.api.http.response.Version>(
                new cloud.fogbow.fs.api.http.response.Version(versionNumber), HttpStatus.OK);
    }
}
