package client;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import entities.Comment;
import entities.Post;

public class PostRenderer implements IRenderer<Post> {

    @Override
    public String render(Post data) {
        String ret;

        ret = "Title: " + data.getTitle() + " (ID: " + data.getId().toString() + ")\n";
        ret += "Content:\n" + data.getContent() + "\n\n";
        ret += "Upvotes: " + data.getUpvotesCount() + ", downvotes: " + data.getDownvotesCount() + "\n";
        ret += "Comments: " + (data.getComments().size() == 0 ? "no comments" : "\n");
        for (Comment comment : data.getComments()) {
            ret += comment.getUser() + ": " + comment.getContent() + "\n";
        }

        return ret;
    }

    @Override
    public String render(Post[] data) {
        if (data.length == 0) {
            return "No posts to show.";
        }
        String ret = "ID";
        for (int i = 0; i < data[0].getId().toString().length() - 1; i++) {
            ret += " ";
        }

        ret += "| Author";

        int longestAuthorLength = this.getLongestAuthorLength(data);
        int longestTitleLength = this.getLongestTitleLength(data);

        for (int i = 0; i < Math.max(longestAuthorLength - "Author".length(), 0); i++) {
            ret += " ";
        }

        ret += " | Title\n";

        for (int i = 0; i < data[0].getId().toString().length() + longestAuthorLength + longestTitleLength
                + "ID".length() + "Author".length()
                + "Title".length() + 6; i++) {
            ret += "-";
        }

        ret += "\n";

        for (Post post : data) {
            ret += post.getId().toString();
            ret += " | " + post.getAuthor();
            for (int i = 0; i < longestAuthorLength - post.getAuthor().length(); i++) {
                ret += " ";
            }
            ret += " | " + post.getTitle() + "\n";
        }

        return ret;
    }

    private int getLongestTitleLength(Post[] data) {
        return Collections.max(
                Arrays.asList(data)
                        .stream()
                        .map(post -> post.getTitle().length())
                        .collect(Collectors.toList()));
    }

    private int getLongestAuthorLength(Post[] data) {
        return Collections.max(
                Arrays.asList(data)
                        .stream()
                        .map(post -> post.getAuthor().length())
                        .collect(Collectors.toList()));
    }

}
