import java.util.*;

public final class Channel implements Comparable<Channel> {
    private Set<String> channelNicknames;
    private String owner;
    private boolean prv;

    public Channel(String owner) {
        this.owner = owner;
        channelNicknames = new TreeSet<>();
        channelNicknames.add(owner);
        this.prv = false;
    }

    public Channel(String owner, Boolean prv) {
        this.owner = owner;
        channelNicknames = new TreeSet<>();
        channelNicknames.add(owner);
        this.prv = prv;
    }

    // get users' nicknames in a channel (to implement getUsersInChannel in
    // ServerModel)
    public Set<String> getUsersInChannel() {
        return channelNicknames;
    }

    // get the nickname of owner of the channel (to implement getOwner in
    // ServerModel)
    public String getOwner() {
        return this.owner;
    }

    // remove user from the channel
    public void remove(String user) {
        channelNicknames.remove(user);
    }

    // add user to a channel
    public void add(String nickname) {
        channelNicknames.add(nickname);
    }

    // get the owner of the channel
    public void nameOwner(String name) {
        this.owner = name;
    }

    // check if channel is private or not
    public boolean isPrivate() {
        return this.prv;
    }

    @Override
    public int compareTo(Channel o) {
        return Integer.compare(1, 0);
    }
}