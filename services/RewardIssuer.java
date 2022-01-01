package services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import entities.Comment;
import entities.Post;
import entities.Reaction;

public class RewardIssuer implements Runnable {
    private final DataStoreService store;
    private Date lastUpdate = new Date(0L); // guard value
    private long timeInBetween;
    private double authorPercentage;
    private final HashMap<UUID, Integer> postIterations = new HashMap<>();
    private final HashMap<String, Integer> userCommentsCount = new HashMap<>();
    private final ServerConfig config;
    private DatagramSocket skt;

    public RewardIssuer(DataStoreService store, long timeInBetween, double authorPercentage, ServerConfig config) {
        this.timeInBetween = timeInBetween;
        this.authorPercentage = authorPercentage;
        this.store = store;
        this.config = config;
        // TODO how to close
        try {
            this.skt = new DatagramSocket(5555);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private double getPostReward(Post post, PostRewardData contributors) {
        double ret = 0.0;

        int reactionScore = 0;
        for (Reaction reaction : contributors.newReactions) {
            reactionScore += reaction.getValue();
        }

        double commentScore = 0.0;
        for (Comment comment : contributors.newComments) {
            // TODO count comments made to THIS post by this commenter, not to all posts!
            int commenterCount = this.userCommentsCount.computeIfAbsent(comment.getUser(),
                    (username) -> this.store.getUserCommentCount(username));

            commentScore += 2 / (1 + Math.exp(-commenterCount - 1));
        }

        ret = Math.log(Math.max(reactionScore, 0) + 1) + Math.log(commentScore + 1);

        return ret / this.postIterations.get(post.getId());
    }

    private void commitUserWalletBalanceChange(String username, double delta) {
        this.store.updateUserWallet(username, delta);
    }

    private class PostRewardData {
        /**
         * Convenience class that encapsulates the "stakeholders" related to a post
         * inside of an iteration of rewards. Contains data about who the
         * "contributors" of a post are, and what entities (upvotes and comments)
         * made them contributors
         * 
         */
        Set<String> upvoters;
        Set<String> commenters;
        Set<Reaction> newReactions;
        Set<Comment> newComments;

        PostRewardData(Post post, Date since) {
            this.newComments = (post.getComments().stream()
                    .filter(comment -> comment.getTimestamp().compareTo(since) > 0)
                    .collect(Collectors.toSet()));
            this.commenters = (this.newComments.stream()
                    .map(comment -> comment.getUser())
                    .collect(Collectors.toSet()));
            this.newReactions = (post.getReactions().stream()
                    .filter(reaction -> reaction.getTimestamp().compareTo(since) > 0)
                    .collect(Collectors.toSet()));
            this.upvoters = (post.getUpvotes().stream().filter(upvote -> upvote.getTimestamp().compareTo(since) > 0)
                    .map(upvote -> upvote.getUser())
                    .collect(Collectors.toSet()));
        }

        Set<String> getContributors() {
            Set<String> ret = new HashSet<>(this.commenters);
            ret.addAll(this.upvoters);
            return ret;
        }

    }

    private PostRewardData getPostRewardData(Post post, Date since) {
        return new PostRewardData(post, since);
    }

    private void computeUserWalletUpdates(String username) {
        Set<Post> postSet = this.store.getUserPosts(username);
        double balanceDelta = 0.0;
        for (Post post : postSet) {
            // initialize post iteration count if this is the first iteration for this post
            this.postIterations.computeIfAbsent(post.getId(), (__) -> 1);

            PostRewardData rewardData = this.getPostRewardData(post, this.lastUpdate);
            Set<String> contributors = rewardData.getContributors();

            double postReward = this.getPostReward(post, rewardData);

            balanceDelta += postReward * this.authorPercentage;
            double contributorPostReward = (postReward * (1 - this.authorPercentage)) / contributors.size();
            for (String contributor : contributors) {
                this.commitUserWalletBalanceChange(contributor, contributorPostReward);
            }

            this.postIterations.compute(post.getId(), (__, count) -> count + 1);
        }
        this.commitUserWalletBalanceChange(username, balanceDelta);
    }

    public void updateUsersWallets() {
        for (String username : this.store.getUsernames()) {
            this.computeUserWalletUpdates(username);
        }
    }

    private void notifyWalletUpdates() throws IOException {
        byte[] buf = "WALLETS_UPDATED".getBytes();
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, this.config.getMulticastAddr(),
                this.config.getMulticastPort());
        this.skt.send(pkt);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(this.timeInBetween * 1000);
                System.out.println("Running!");
                this.updateUsersWallets();
                this.lastUpdate = new Date();
                this.notifyWalletUpdates();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
