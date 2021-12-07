package services;

import entities.Post;
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

    // public RestResponse followUserHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse unfollowUserHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse listMyPostsHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse showFeedHandler(AuthenticatedRestRequest request) {

    // }

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

    // public RestResponse ratePostHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse createCommentHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse showWalletHandler(AuthenticatedRestRequest request) {

    // }

    // public RestResponse showWalletInBitcoinHandler(AuthenticatedRestRequest
    // request) {

    // }
}
