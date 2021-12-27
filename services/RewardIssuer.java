package services;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import entities.Post;

public class RewardIssuer implements Runnable {
    private final DataStoreService store;
    private Date lastUpdate = null;
    private long timeInBetween;
    private double authorPercentage;

    public RewardIssuer(DataStoreService store, long timeInBetween, double authorPercentage) {
        this.timeInBetween = timeInBetween;
        this.authorPercentage = authorPercentage;
        this.store = store;
    }

    public double getPostReward(Post post, Date since) {
        return 0;
    }

    public void commitUserWalletBalanceChange(String username, double delta) {
        this.store.updateUserWallet(username, delta);
    }

    public Set<String> getRecentContributors(Post post, Date since) {
        Set<String> contributors = new HashSet<>();

        // add all commenters
        contributors.addAll(
                post.getComments().stream().filter(comment -> comment.getTimestamp().compareTo(since) > 0)
                        .map(comment -> comment.getUser()).collect(Collectors.toSet()));

        // add all upvoters
        contributors.addAll(
                post.getUpvotes().stream().filter(upvote -> upvote.getTimestamp().compareTo(since) > 0)
                        .map(upvote -> upvote.getUser()).collect(Collectors.toSet()));

        return contributors;
    }

    public void computeUserWalletUpdates(String username) {
        Set<Post> postSet = this.store.getUserPosts(username);
        double balanceDelta = 0.0;
        for (Post post : postSet) {
            double postReward = this.getPostReward(post, this.lastUpdate);
            balanceDelta += postReward * this.authorPercentage;
            Set<String> contributors = this.getRecentContributors(post, this.lastUpdate);
            double contributorPostReward = (postReward * (1 - this.authorPercentage)) / contributors.size();
            for (String contributor : contributors) {
                this.commitUserWalletBalanceChange(contributor, contributorPostReward);
            }
        }
        this.commitUserWalletBalanceChange(username, balanceDelta);
    }

    public void updateUsersWallets() {
        for (String username : this.store.getUsernames()) {
            this.computeUserWalletUpdates(username);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(this.timeInBetween * 1000);
                System.out.println("Running!");
                this.updateUsersWallets();
                this.lastUpdate = new Date();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
