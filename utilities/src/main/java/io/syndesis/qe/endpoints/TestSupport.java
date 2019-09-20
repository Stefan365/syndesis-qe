package io.syndesis.qe.endpoints;

import static org.junit.Assert.fail;

import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.RestUtils;
import io.syndesis.qe.utils.TestUtils;

import org.apache.commons.io.FileUtils;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * TestSupport class contains utility methods for usage of the test-support endpoint.
 *
 * @author jknetl
 */
@Slf4j
public final class TestSupport {

    private static final String ENDPOINT_NAME = "/test-support";
    private static final String API_PATH = TestConfiguration.syndesisRestApiPath();
    private static Client client;
    private static TestSupport instance = null;

    private TestSupport() {
        client = RestUtils.getClient();
    }

    public static TestSupport getInstance() {
        if (instance == null) {
            instance = new TestSupport();
        }
        return instance;
    }

    /**
     * Resets Syndesis database.
     */
    public void resetDB() {
        int tries = 0;
        while (tries < 10) {
            if (resetDbWithResponse() == 204) {
                return;
            }
            TestUtils.sleepIgnoreInterrupt(5000L);
            tries++;
        }
        fail("Unable to successfully reset DB after 10 tries");
    }


    /**
     * Resets Syndesis database.
     *
     * @return HTTP response code
     */
    private int resetDbWithResponse() {
        String url = getEndpointUrl();
        final Invocation.Builder invocation = client
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-User", "pista")
                .header("X-Forwarded-Access-Token", "kral");
        int responseCode = -1;
        try {
            responseCode = invocation.get().getStatus();
        } catch (ProcessingException e) {
            log.error("Error while invoking reset-db: ", e);
            final String fileName = "error-" + new Date().getTime() + ".log";
            final String content = TestUtils.printPods() + "\n" + OpenShiftUtils.getPodLogs("syndesis-server");
            try {
                FileUtils.writeStringToFile(new File("log/" + fileName), content, "UTF-8");
                log.error("Wrote server debug stuff to " + fileName);
            } catch (IOException ex) {
                log.error("Unable to write string to file: ", ex);
            }
        }
        log.info("syndesis-db has been reset, via url: *{}*, responseCode:*{}*", url, responseCode);
        log.debug("Reset endpoint reponse: {}", responseCode);
        return responseCode;
    }

    public String getEndpointUrl() {
        String restEndpoint = String.format("%s%s%s%s", RestUtils.getRestUrl(), API_PATH, ENDPOINT_NAME, "/reset-db");
        log.debug("Reset endpoint URL: *{}*", restEndpoint);
        return restEndpoint;
    }
}
