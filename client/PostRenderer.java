package client;

import entities.Comment;
import entities.Post;

public class PostRenderer implements IRenderer<Post> {

    @Override
    public String render(Post data) {
        String ret;

        ret = "Title: " + data.getTitle() + " (ID: " + data.getId().toString() + ")\n";
        ret += "Content:\n" + data.getContent() + "\n\n";
        ret += "Upvotes: " + data.getUpVotesCount() + ", downvotes: " + data.getDownVotesCount() + "\n";
        ret += "Comments: " + (data.getComments().size() == 0 ? "no comments" : "\n");
        for (Comment comment : data.getComments()) {
            ret += comment.getUser() + ": " + comment.getContent() + "\n";
        }

        return ret;
    }

    @Override
    public String render(Post[] data) {
        String ret = "ID \t\t\t | Author \t\t\t | Title\n --------------------------- \n";

        for (Post post : data) {
            ret += post.getId().toString() + " | " + post.getAuthor() + "\t\t | " + post.getContent() + "\n";
        }

        return ret;
    }

}
