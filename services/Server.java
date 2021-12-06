package services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import auth.AuthenticationMiddleware;
import exceptions.BadRequestException;
import exceptions.InvalidTokenException;
import exceptions.MethodNotSupportedException;
import exceptions.NoAuthenticationProvidedException;
import exceptions.PermissionDeniedException;
import exceptions.ResourceNotFoundException;
import exceptions.RouteNotFoundException;
import protocol.AuthenticatedRestRequest;
import protocol.RestRequest;
import protocol.RestResponse;
import routing.ApiRouter;

public class Server {
    private final ApiRouter router;
    private final SocialNetworkService service;
    private final AuthenticationMiddleware authMiddlware;

    private class ClientConnectionState {
        RestResponse pendingResponse;
    }

    private final int BUF_CAPACITY = 4096 * 4096;

    private void readFromKey(SelectionKey key) throws IOException {
        SocketChannel clientSkt = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(this.BUF_CAPACITY);
        int readCount = clientSkt.read(buf);
        if (readCount == -1) { // no data was read, client closed connection
            // the caller will catch this and remove the key from the readset
            throw new IOException();
        }
        buf.flip();

        RestRequest request = RestRequest.parseRequestString(StandardCharsets.UTF_8.decode(buf).toString());
        RestResponse response = handleRequest(request);
        // remove OP_READ from the interest set and replace it with OP_WRITE
        // as there isn't anything to be read from the client, but we now
        // have a response to write to it
        key.interestOps(SelectionKey.OP_WRITE);
        ((ClientConnectionState) key.attachment()).pendingResponse = response;
    }

    private RestResponse handleRequest(RestRequest request) {
        Method handler;
        RestResponse response = null;
        AuthenticatedRestRequest authenticatedRequest;
        boolean requireAuth = !request.isLoginRequest();

        // resolve route to get the handler method for this request
        try {
            handler = this.router.getRequestHandler(request);
        } catch (MethodNotSupportedException e) {
            return new RestResponse(404);
        } catch (RouteNotFoundException e) {
            return new RestResponse(405);
        }

        // check request headers to authenticate the request
        try {
            authenticatedRequest = requireAuth ? this.authMiddlware.authenticateRequest(request)
                    : this.authMiddlware.getAnonymousRestRequest(request);
        } catch (NoAuthenticationProvidedException e) {
            return new RestResponse(401);
        } catch (InvalidTokenException e) {
            return new RestResponse(400);
        }
        // invoke handler to execute the request and get
        // response to write back to client
        try {
            response = (RestResponse) handler.invoke(this.service, authenticatedRequest);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return new RestResponse(500);
        } catch (InvocationTargetException e) {
            try {
                // InvocationTargetException is thrown if the method called via
                // `invoke()` throws an exception; in order to get the original
                // exception, use exception chaining, so the correct HTTP error
                // can ultimately be written back to the client
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
            } catch (BadRequestException e1) {
                return new RestResponse(400);
            } catch (PermissionDeniedException e1) {
                return new RestResponse(403);
            } catch (ResourceNotFoundException e1) {
                return new RestResponse(404);
            } catch (Exception e1) {
                assert false; // never reached
            }
        }

        return response;
    }
}
