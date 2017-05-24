package com.raytheon.edex.services;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.msgs.AbstractPrivilegedUtilityCommand;
import com.raytheon.uf.common.localization.msgs.AbstractUtilityResponse;
import com.raytheon.uf.common.localization.msgs.DeleteUtilityCommand;
import com.raytheon.uf.common.localization.msgs.PrivilegedUtilityRequestMessage;
import com.raytheon.uf.common.localization.msgs.UtilityResponseMessage;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;

/**
 * Handle privileged requests.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Jun 03, 2010           rgeorge      Initial creation
 * Jul 10, 2014  2914     garmendariz  Remove EnvProperties
 * Feb 14, 2017  6111     njensen      Overrode getRequestType()
 * May 18, 2017  6242     randerso     Changed to use new roles and permissions
 *                                     framework
 *
 * </pre>
 *
 * @author rgeorge
 */
public class PrivilegedUtilityHandler extends
        AbstractPrivilegedLocalizationRequestHandler<PrivilegedUtilityRequestMessage> {

    private static String UTILITY_DIR = EDEXUtil.getEdexUtility();

    @Override
    public UtilityResponseMessage handleRequest(
            PrivilegedUtilityRequestMessage msg) throws Exception {
        // Service each command
        List<AbstractUtilityResponse> responses = new ArrayList<>();
        AbstractPrivilegedUtilityCommand[] cmds = msg.getCommands();
        for (AbstractPrivilegedUtilityCommand cmd : cmds) {
            LocalizationContext context = cmd.getContext();
            if (cmd instanceof DeleteUtilityCommand) {
                DeleteUtilityCommand castCmd = (DeleteUtilityCommand) cmd;
                String fileName = castCmd.getFilename();
                responses.add(UtilityManager.deleteFile(UTILITY_DIR, context,
                        fileName));
            } else {
                throw new EdexException("Unsupported message type: "
                        + cmd.getClass().getName());
            }
        }
        AbstractUtilityResponse[] respArray = responses
                .toArray(new AbstractUtilityResponse[responses.size()]);

        UtilityResponseMessage response = new UtilityResponseMessage(respArray);

        return response;
    }

    @Override
    public AuthorizationResponse authorized(IUser user,
            PrivilegedUtilityRequestMessage request)
            throws AuthorizationException {

        AbstractPrivilegedUtilityCommand[] commands = request.getCommands();
        for (AbstractPrivilegedUtilityCommand command : commands) {
            LocalizationContext context = command.getContext();
            String filename = command.getFilename();
            String operation;
            if (command instanceof DeleteUtilityCommand) {
                operation = "delete";
            } else {
                throw new IllegalArgumentException("Unrecognized command: "
                        + command.getClass().getName());
            }

            AuthorizationResponse resp = getAuthorizationResponse(user,
                    operation, context, filename, command.getMyContextName());
            if (!resp.isAuthorized()) {
                // If we are not authorized for any of the commands, break early
                return resp;
            }
        }
        return new AuthorizationResponse(true);
    }

    @Override
    public Class<?> getRequestType() {
        return PrivilegedUtilityRequestMessage.class;
    }
}
