package services;

import entities.Post;
import exceptions.ResourceNotFoundException;
import protocol.AuthenticatedRestRequest;
import protocol.RestResponse;

public class SocialNetworkService {
    private final DataStoreService store;

    public RestResponse userListHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse followingListHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse followUserHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse unfollowUserHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse listMyPostsHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse showFeedHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse createPostHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse showPostHandler(AuthenticatedRestRequest request) throws ResourceNotFoundException {
        Post post = this.store.getPost(request.getRequest().getPathParameter());
        if (post == null) {
            throw new ResourceNotFoundException();
        }
        String responseBody = new Serializer<Post>().serialize(post);

        return new RestResponse(200, responseBody);

    }

    public RestResponse deletePostHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse rewinPostHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse ratePostHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse createCommentHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse showWalletHandler(AuthenticatedRestRequest request) {

    }

    public RestResponse showWalletInBitcoinHandler(AuthenticatedRestRequest request) {

    }
}
