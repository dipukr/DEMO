package com.example.order.client;

import com.example.order.saga.StepFailedException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.util.function.Supplier;

/**
 * Sorts the failures of a saga command into the two cases the orchestrator cares about.
 * <ul>
 *   <li>The participant definitely did NOT apply the change: it rejected the command (4xx) or the
 *       request never reached it (connection refused). Rethrown as {@link StepFailedException}.</li>
 *   <li>Anything else (read timeout, 5xx): the outcome is unknown and the exception propagates
 *       unchanged.</li>
 * </ul>
 */
public final class RemoteCall {

    private RemoteCall() {
    }

    public static <T> T command(String participant, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            // 4xx: a business rejection. The participant rolled back its local transaction.
            throw new StepFailedException(participant + " rejected the request: " + detailOf(e));
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof ConnectException) {
                // The request never reached the participant, so nothing was applied.
                throw new StepFailedException(participant + " is unavailable");
            }
            throw e; // e.g. a read timeout: the participant may or may not have applied the change
        }
    }

    /** Reads "detail" from the participant's RFC 9457 problem response. */
    private static String detailOf(HttpClientErrorException e) {
        try {
            ProblemBody body = e.getResponseBodyAs(ProblemBody.class);
            if (body != null && body.detail() != null) {
                return body.detail();
            }
        } catch (RuntimeException ignored) {
            // unreadable body: fall back to the status code
        }
        return e.getStatusCode().toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemBody(String detail) {
    }
}
