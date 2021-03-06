package services;

import entities.Comment;
import entities.Post;
import entities.Reaction;
import entities.User;
import exceptions.BadRequestException;
import exceptions.InternalServerErrorException;
import exceptions.PermissionDeniedException;
import exceptions.ResourceNotFoundException;
import protocol.AuthenticatedRestRequest;
import protocol.RestResponse;
import services.DataStoreService.OperationStatus;
import services.DataStoreService.OperationStatus.Status;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import auth.AuthenticationToken;
import auth.Password;

public class SocialNetworkService {
    private final DataStoreService store;
    private final FollowerNotificationService followerService;
    private final WalletConversionService walletService;
    private final ServerConfig config;

    public SocialNetworkService(DataStoreService store, FollowerNotificationService followerService,
            WalletConversionService walletService, ServerConfig config) {
        this.store = store;
        this.followerService = followerService;
        this.walletService = walletService;
        this.config = config;
    }

    public RestResponse loginHandler(AuthenticatedRestRequest request)
            throws PermissionDeniedException, BadRequestException {
        String[] bodyTokens = request.getRequest().getBody().split("\n");
        if (bodyTokens.length != 2) {
            // request body is expected to have the username on the
            // first line and the password on the second line
            throw new BadRequestException();
        }

        String username = bodyTokens[0];
        Password password = new Password(bodyTokens[1]);

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
            return new RestResponse(200,
                    token.getToken() + "\n" + // also send IP address and port of multicast group
                            this.config.getMulticastAddr().toString() + "\n"
                            + this.config.getMulticastPort());
        }

        // incorrect password
        throw new PermissionDeniedException();
    }

    public RestResponse logoutHandler(AuthenticatedRestRequest request) throws PermissionDeniedException {
        String claimedUsername = request.getRequest().getBody().trim();
        AuthenticationToken token = new AuthenticationToken(
                request.getRequest().getHeader("Authorization").substring("Bearer ".length()));
        if (this.store.deleteUserToken(token, claimedUsername)) {
            return new RestResponse(204);
        }
        throw new PermissionDeniedException();
    }

    public RestResponse userListHandler(AuthenticatedRestRequest request) {
        Set<User> users = this.store.getCompatibleUsers(request.getUser().getUsername());
        String body = new Serializer<User[]>()
                .serialize((User[]) ((users == null ? new HashSet<User>() : users).toArray(new User[0])));
        return new RestResponse(200, body);
    }

    public RestResponse followingListHandler(AuthenticatedRestRequest request) {
        Set<User> users = this.store.getUserFollowing(request.getUser().getUsername());
        String body = new Serializer<User[]>()
                .serialize((User[]) ((users == null ? new HashSet<User>() : users).toArray(new User[0])));
        return new RestResponse(200, body);
    }

    public RestResponse followUserHandler(AuthenticatedRestRequest request)
            throws ResourceNotFoundException, PermissionDeniedException {
        String toFollow = request.getRequest().getBody().trim();
        String newFollower = request.getUser().getUsername();

        if (toFollow.equals(newFollower)) {
            // cannot follow yourself
            throw new PermissionDeniedException();
        }
        synchronized (this) {
            if (this.store.addFollower(toFollow, newFollower)) {
                // send RMI notification to user who just acquired a follower
                this.followerService.notifyUser(toFollow);
                return new RestResponse(204);
            }
        }
        throw new ResourceNotFoundException();
    }

    public RestResponse unfollowUserHandler(AuthenticatedRestRequest request) throws ResourceNotFoundException {
        String target = request.getRequest().getBody().trim();
        synchronized (this) {
            if (this.store.removeFollower(target, request.getUser().getUsername())) {
                // send RMI notification to user who just lost a follower
                this.followerService.notifyUser(target);
                return new RestResponse(204);
            }
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
        OperationStatus outcome = this.store.deletePost(request.getRequest().getPathParameter(),
                request.getUser().getUsername());
        if (outcome.status == Status.ILLEGAL_OPERATION) {
            throw new PermissionDeniedException();
        } else if (outcome.status == Status.NOT_FOUND) {
            throw new ResourceNotFoundException();
        }

        return new RestResponse(204);
    }

    public RestResponse rewinPostHandler(AuthenticatedRestRequest request)
            throws ResourceNotFoundException, PermissionDeniedException {
        Post post = this.store.getPost(request.getRequest().getPathParameter());
        if (post == null) {
            throw new ResourceNotFoundException();
        }
        if (post.getAuthor() == request.getUser().getUsername()) {
            // can't rewin your own posts
            throw new PermissionDeniedException();
        }

        Post rewinPost = new Post(request.getUser().getUsername(), post);
        this.store.addPost(request.getUser().getUsername(), rewinPost);
        return new RestResponse(200, new Serializer<Post>().serialize(rewinPost));
    }

    public RestResponse ratePostHandler(AuthenticatedRestRequest request)
            throws BadRequestException, ResourceNotFoundException, PermissionDeniedException {
        Reaction reaction;
        try {
            reaction = new Serializer<Reaction>().parse(request.getRequest().getBody(), Reaction.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new BadRequestException();
        }
        reaction.setUser(request.getUser().getUsername());

        OperationStatus outcome = this.store.addPostReaction(request.getRequest().getPathParameter(), reaction);
        if (outcome.status == Status.OK) {
            return new RestResponse(200);
        } else if (outcome.status == Status.NOT_FOUND) {
            throw new ResourceNotFoundException();
        } else {
            throw new PermissionDeniedException();
        }
    }

    public RestResponse createCommentHandler(AuthenticatedRestRequest request)
            throws BadRequestException, ResourceNotFoundException, PermissionDeniedException {
        Comment comment;
        try {
            comment = new Serializer<Comment>().parse(request.getRequest().getBody(), Comment.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException();
        }
        comment.setUser(request.getUser().getUsername());
        OperationStatus outcome = this.store.addPostComment(request.getRequest().getPathParameter(), comment);
        if (outcome.status == Status.NOT_FOUND) {
            throw new ResourceNotFoundException();
        } else if (outcome.status == Status.ILLEGAL_OPERATION) {
            throw new PermissionDeniedException();
        }

        return new RestResponse(201, new Serializer<Comment>().serialize(comment));
    }

    public RestResponse showWalletHandler(AuthenticatedRestRequest request) {
        String body = new Serializer<Wallet>().serialize(this.store.getUserWallet(request.getUser().getUsername()));
        return new RestResponse(200, body);
    }

    public RestResponse showWalletInBitcoinHandler(AuthenticatedRestRequest request)
            throws InternalServerErrorException {
        Wallet wallet = this.store.getUserWallet(request.getUser().getUsername());
        double conversionValue = this.walletService.getConversionRate();
        if (conversionValue == 0.0) {
            throw new InternalServerErrorException();
        }

        String body = Double.toString(wallet.getBalance() * conversionValue);
        return new RestResponse(200, body);
    }
}
