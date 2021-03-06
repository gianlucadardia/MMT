package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonElement;
import eu.modernmt.core.cluster.error.SystemShutdownException;
import eu.modernmt.core.facade.exceptions.InternalErrorException;
import eu.modernmt.core.facade.exceptions.ValidationException;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.RESTResponse;
import eu.modernmt.rest.framework.routing.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class JSONAction implements Action {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public final void execute(RESTRequest req, RESTResponse resp) {
        try {
            unsecureExecute(req, resp);
        } catch (TemplateException e) {
            if (logger.isDebugEnabled())
                logger.debug("Template exception while executing action " + this, e);
            resp.resourceNotFound(e);
        } catch (Parameters.ParameterParsingException e) {
            resp.badRequest(e);
        } catch (InternalErrorException e) {
            if (logger.isDebugEnabled())
                logger.debug("Internal error while executing action " + this, e);
            resp.unexpectedError(e);
        } catch (ValidationException e) {
            if (logger.isDebugEnabled())
                logger.debug("Validation exception while executing action " + this, e);
            resp.badRequest(e);
//        } catch (AuthException e) {
//            if (logger.isDebugEnabled())
//                logger.debug("Auth exception while executing action " + this, e);
//            resp.forbidden(e);
        } catch (SystemShutdownException e) {
            if (logger.isDebugEnabled())
                logger.debug("Unable to complete action " + this + ": system is shutting down", e);
            resp.unavailable(e);
        } catch (Throwable e) {
            logger.error("Unexpected exceptions while executing action " + this, e);
            resp.unexpectedError(e);
        }
    }

    protected final void unsecureExecute(RESTRequest req, RESTResponse resp) throws Throwable {
        Parameters params = getParameters(req);
        JSONActionResult result = getResult(req, params);

        if (result == null) {
            resp.resourceNotFound();
        } else {
            result.beforeDump(req, params);
            JsonElement json = result.dump(this, req, params);

            resp.ok(json);
        }
    }

    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Parameters(req);
    }

    protected abstract JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable;

    protected void decorate(JsonElement element) {
        // Default implementation does nothing
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }

}
