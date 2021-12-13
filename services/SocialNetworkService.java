package services;

import entities.Comment;
import entities.Post;
import entities.Reaction;
import entities.User;
import exceptions.BadRequestException;
import exceptions.PermissionDeniedException;
import exceptions.ResourceNotFoundException;
import protocol.AuthenticatedRestRequest;
import protocol.RestResponse;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import auth.AuthenticationToken;
import auth.Password;

public class SocialNetworkService {
    private final DataStoreService store;

    public SocialNetworkService(DataStoreService store) {
        this.store = store;
    }

    public RestResponse loginHandler(AuthenticatedRestRequest request)
            throws PermissionDeniedException, BadRequestException {
        String[] bodyTokens = request.getRequest().getBody().split("\n");
        if (bodyTokens.length != 2) {
            // request body is expected to have the username on the
            // first line and the password on the second line
            System.out.println("------- BODY is: " + request.getRequest().getBody() + "-----------");
            System.out.println(
                    "BODY TOKEN LENGTH IS " + bodyTokens.length + "\n\n\nFIRST ONE IS" + bodyTokens[0] + "\n\n\n");
            throw new BadRequestException();
        }

        String username = bodyTokens[0];
        Password password = new Password(bodyTokens[1]);

        System.out.println("username: " + username + "password: " + password.getPassword());

        User authenticatingUser = this.store.getUser(username);

        if (authenticatingUser == null) {
            throw new PermissionDeniedException();
        }
        if (authenticatingUser.getPassword().equals(password)) {
            // correct password, create and return a
            // new authentication token for this user
            AuthenticationToken token = new AuthenticationToken();
            this.store.setUserToken(authenticatingUser, token);
            // client will use this new token to authenticate subsequent requests
            return new RestResponse(200, token.getToken());
        }

        // incorrect password
        throw new PermissionDeniedException();
    }

    public RestResponse logoutHandler(AuthenticatedRestRequest request) {
        this.store.deleteUserToken(new AuthenticationToken(request.getRequest().getHeader("Authorization")));
        return new RestResponse(204);
    }

    public RestResponse userListHandler(AuthenticatedRestRequest request) {
        Set<User> users = this.store.getCompatibleUsers(request.getUser().getUsername());
        String body = new Serializer<User[]>()
                .serialize((User[]) ((users == null ? new HashSet<User>() : users).toArray(new User[0])));
        return new RestResponse(200, body);
    }

    // public RestResponse followingListHandler(AuthenticatedRestRequest request) {

    // }

    public RestResponse followUserHandler(AuthenticatedRestRequest request) throws ResourceNotFoundException {
        if (this.store.addFollower(request.getRequest().getBody(), request.getUser().getUsername())) {
            return new RestResponse(204);
        }
        throw new ResourceNotFoundException();
    }

    public RestResponse unfollowUserHandler(AuthenticatedRestRequest request) throws ResourceNotFoundException {
        if (this.store.removeFollower(request.getRequest().getBody(), request.getUser().getUsername())) {
            return new RestResponse(204);
        }
        throw new ResourceNotFoundException();
    }

    public RestResponse listMyPostsHandler(AuthenticatedRestRequest request) {
        String body = new Serializer<Post[]>()
                .serialize((Post[]) this.store.getUserPosts(request.getUser().getUsername()).toArray(new Post[0]));
        return new RestResponse(200, body);
    }

    public RestResponse showFeedHandler(AuthenticatedRestRequest request) {
        Set<Post> feed = this.store.getUserFeed(request.getUser().getUsername());
        String body = new Serializer<Post[]>()
                .serialize((Post[]) ((feed == null ? new HashSet<Post>() : feed).toArray(new Post[0])));
        return new RestResponse(200, body);
    }

    public RestResponse createPostHandler(AuthenticatedRestRequest request) throws BadRequestException {
        String postData = request.getRequest().getBody();
        Post post;

        try {
            post = new Serializer<Post>().parse(postData, Post.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new BadRequestException();
        }
        post.setAuthor(request.getUser().getUsername());
        this.store.addPost(request.getUser().getUsername(), post);

        return new RestResponse(201, new Serializer<Post>().serialize(post));
    }

    public RestResponse showPostHandler(AuthenticatedRestRequest request) throws ResourceNotFoundException {
        Post post = this.store.getPost(request.getRequest().getPathParameter());
        if (post == null) {
            throw new ResourceNotFoundException();
        }
        String responseBody = new Serializer<Post>().serialize(post);

        return new RestResponse(200, responseBody);

    }

    public RestResponse deletePostHandler(AuthenticatedRestRequest request)
            throws ResourceNotFoundException, PermissionDeniedException {
        // TODO find a way to differentiate between post not found and permission error
        if (!this.store.deletePost(request.getRequest().getPathParameter(), request.getUser().getUsername())) {
            throw new PermissionDeniedException();
        }

        return new RestResponse(204);
    }

    // public RestResponse rewinPostHandler(AuthenticatedRestRequest request) {

    // }

    public RestResponse ratePostHandler(AuthenticatedRestRequest request)
            throws BadRequestException, ResourceNotFoundException {
        Reaction reaction;
        try {
            reaction = new Serializer<Reaction>().parse(request.getRequest().getBody(), Reaction.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException();
        }
        reaction.setUser(request.getUser().getUsername());

        if (this.store.addPostReaction(request.getRequest().getPathParameter(), reaction)) {
            return new RestResponse(200);
        }
        throw new ResourceNotFoundException();
    }

    public RestResponse createCommentHandler(AuthenticatedRestRequest request)
            throws BadRequestException, ResourceNotFoundException {
        Comment comment;
        try {
            comment = new Serializer<Comment>().parse(request.getRequest().getBody(), Comment.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new BadRequestException();
        }
        comment.setUser(request.getUser().getUsername());

        if (this.store.addPostComment(request.getRequest().getPathParameter(), comment)) {
            return new RestResponse(201, new Serializer<Comment>().serialize(comment));
        }
        throw new ResourceNotFoundException();
    }

    // public RestResponse showWalletHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse showWalletInBitcoinHandler(AuthenticatedRestRequest
    // request) {

    // }
}
