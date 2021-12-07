package services;

import entities.Comment;
import entities.Post;
import entities.Reaction;
import exceptions.BadRequestException;
import exceptions.PermissionDeniedException;
import exceptions.ResourceNotFoundException;
import protocol.AuthenticatedRestRequest;
import protocol.RestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public class SocialNetworkService {
    private final DataStoreService store;

    public SocialNetworkService(DataStoreService store) {
        this.store = store;
    }

    public RestResponse userListHandler(AuthenticatedRestRequest request) {
        return new RestResponse(500);
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
                .serialize((Post[]) this.store.getUserPosts(request.getUser().getUsername()).toArray());
        return new RestResponse(200, body);
    }

    public RestResponse showFeedHandler(AuthenticatedRestRequest request) {
        String body = new Serializer<Post[]>()
                .serialize((Post[]) this.store.getUserFeed(request.getUser().getUsername()).toArray());
        return new RestResponse(200, body);
    }

    public RestResponse createPostHandler(AuthenticatedRestRequest request) throws BadRequestException {
        String postData = request.getRequest().getBody();
        Post post;

        try {
            post = new Serializer<Post>().parse(postData, Post.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException();
        }
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
