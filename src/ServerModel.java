import java.util.*;

/**
 * The {@code ServerModel} is the class responsible for tracking the state of
 * the server, including its current users and the channels they are in. This
 * class is used by subclasses of {@link Command} to: 1. handle commands from
 * clients, and 2. handle commands from {@link ServerBackend} to coordinate
 * client connection/disconnection.
 */
public final class ServerModel implements ServerModelApi {
    private Map<Integer, String> registeredUsers;
    private Map<String, Channel> createdChannels;

    /**
     * Constructs a {@code ServerModel} and initializes any collections needed for
     * modeling the server state.
     */
    public ServerModel() {
        registeredUsers = new TreeMap<>();
        createdChannels = new TreeMap<>();
    }

    // ==========================================================================
    // Client connection handlers
    // ==========================================================================

    /**
     * Informs the model that a client has connected to the server with the given
     * user ID. The model should update its state so that it can identify this user
     * during later interactions. The newly connected user will not yet have had the
     * chance to set a nickname, and so the model should provide a default nickname
     * for the user. Any user who is registered with the server (without being later
     * deregistered) should appear in the output of {@link #getRegisteredUsers()}.
     *
     * @param userId The unique ID created by the backend to represent this user
     * @return A {@link Broadcast} to the user with their new nickname
     */
    public Broadcast registerUser(int userId) {
        String nickname = generateUniqueNickname();
        registeredUsers.put(userId, nickname);
        return Broadcast.connected(nickname);

    }

    /**
     * Generates a unique nickname of the form "UserX", where X is the smallest
     * non-negative integer that yields a unique nickname for a user.
     * 
     * @return the generated nickname
     */
    private String generateUniqueNickname() {
        int suffix = 0;
        String nickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            nickname = "User" + suffix++;
        } while (existingUsers != null && existingUsers.contains(nickname));
        return nickname;
    }

    /**
     * Determines if a given nickname is valid or invalid (contains at least one
     * alphanumeric character, and no non-alphanumeric characters).
     * 
     * @param name The channel or nickname string to validate
     * @return true if the string is a valid name
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Informs the model that the client with the given user ID has disconnected
     * from the server. After a user ID is deregistered, the server backend is free
     * to reassign this user ID to an entirely different client; as such, the model
     * should remove all state of the user associated with the deregistered user ID.
     * The behavior of this method if the given user ID is not registered with the
     * model is undefined. Any user who is deregistered (without later being
     * registered) should not appear in the output of {@link #getRegisteredUsers()}.
     *
     * @param userId The unique ID of the user to deregister
     * @return A {@link Broadcast} instructing clients to remove the user from all
     *         channels
     */
    public Broadcast deregisterUser(int userId) {
        // get nickname of the user ID
        String nickname = registeredUsers.get(userId);
        registeredUsers.remove(userId);
        Set<String> others = (Set<String>) getOtherUsers(nickname);

        // removes users from all the channels user is in
        Set<String> channelsToRmv = new TreeSet<String>();
        for (Map.Entry<String, Channel> mapEntry : createdChannels.entrySet()) {
            Channel c = mapEntry.getValue();
            String s = mapEntry.getKey();
            if (mapEntry.getValue().getUsersInChannel().contains(nickname)) {
                c.remove(nickname);
            }
            if (c.getOwner().equals(nickname)) {
                channelsToRmv.add(s);
            }
        }
        for (String chan : channelsToRmv) {
            createdChannels.remove(chan);
        }
        // let other users know that the user is disconnected
        return Broadcast.disconnected(nickname, others);
    }

    // helper: get all the other users in the channels that the user is in (not
    // including that user)
    public Collection<String> getOtherUsers(String nickname) {
        Set<String> other = new TreeSet<>();
        for (Map.Entry<String, Channel> mapEntry : createdChannels.entrySet()) {
            if (mapEntry.getValue().getUsersInChannel().contains(nickname)) {
                other.addAll(mapEntry.getValue().getUsersInChannel());
            }
        }
        other.remove(nickname);// not include that user
        return other;
    }

    // ==========================================================================
    // Server model queries
    // These functions provide helpful ways to test the state of your model.
    // You may also use them in your implementation.
    // ==========================================================================

    /**
     * Gets the user ID currently associated with the given nickname. The returned
     * ID is -1 if the nickname is not currently in use.
     *
     * @param nickname The nickname for which to get the associated user ID
     * @return The user ID of the user with the argued nickname if such a user
     *         exists, otherwise -1
     */
    public int getUserId(String nickname) {
        // nickname in the map -> key

        for (Map.Entry<Integer, String> mapEntry : registeredUsers.entrySet()) {
            if (nickname.equals(mapEntry.getValue())) {
                int userID = mapEntry.getKey();
                return userID;
            }
        }
        return -1;
    }

    /**
     * Gets the nickname currently associated with the given user ID. The returned
     * nickname is null if the user ID is not currently in use.
     *
     * @param userId The user ID for which to get the associated nickname
     * @return The nickname of the user with the argued user ID if such a user
     *         exists, otherwise null
     */
    public String getNickname(int userId) {
        // userID in the map -> value
        for (Map.Entry<Integer, String> mapEntry : registeredUsers.entrySet()) {
            if (userId == mapEntry.getKey()) {
                String nickname = mapEntry.getValue();
                return nickname;
            }
        }
        return null;
    }

    /**
     * Gets a collection of the nicknames of all users who are registered with the
     * server. Changes to the returned collection should not affect the server
     * state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of registered user nicknames
     */
    public Collection<String> getRegisteredUsers() {
        Collection<String> u = new TreeSet<>(registeredUsers.values());
        return u;

    }

    /**
     * Gets a collection of the names of all the channels that are present on the
     * server. Changes to the returned collection should not affect the server
     * state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of channel names
     */
    public Collection<String> getChannels() {
        Collection<String> c = new TreeSet<>(createdChannels.keySet());
        return c;
    }

    /**
     * Gets a collection of the nicknames of all the users in a given channel. The
     * collection is empty if no channel with the given name exists. Modifications
     * to the returned collection should not affect the server state.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get member nicknames
     * @return The collection of user nicknames in the argued channel
     */
    public Collection<String> getUsersInChannel(String channelName) {
        Set<String> channelNicknames = createdChannels.get(channelName).getUsersInChannel();
        Collection<String> u = new TreeSet<>(channelNicknames);
        return u;

    }

    /**
     * Gets the nickname of the owner of the given channel. The result is
     * {@code null} if no channel with the given name exists.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get the owner nickname
     * @return The nickname of the channel owner if such a channel exists, otherwise
     *         null
     */
    public String getOwner(String channelName) {
        return createdChannels.get(channelName).getOwner();
    }

    // get the channels user is in by nickname
    public Collection<Channel> getChannelsByUserNickname(String nickname) {
        Set<Channel> c = new TreeSet<>();
        for (Map.Entry<String, Channel> mapEntry : createdChannels.entrySet()) {
            Collection<String> u = mapEntry.getValue().getUsersInChannel();
            if (u.contains(nickname)) {
                c.add(mapEntry.getValue());
            }
        }
        return c;
    }

// helper function: change nickname of user
    public void changeNickname(int userId, String nickname) {

        String oldName = registeredUsers.get(userId);
        registeredUsers.put(userId, nickname);
        for (Channel c : createdChannels.values()) {
            Set<String> u = c.getUsersInChannel();
            if (u.contains(oldName)) {
                c.remove(oldName);
                c.add(nickname);
                String owner = c.getOwner();
                if (owner.equals(oldName)) {
                    c.nameOwner(nickname);
                }
            }
        }
    }

//helper function: create a channel
    public void create(String nameC, String owner, Boolean prv) {
        Channel c = new Channel(owner, prv);
        // add new channel to the map of existing channels
        createdChannels.put(nameC, c);
        c.add(owner);

    }
//helper function: add users to a channel 

    public void join(Channel c, String name) {
        c.add(name);
    }

// helper function: leave a channel

    public void leave(String nameC, String nickname) {
        // delete channel if the owner leaves
        if (nickname.equals(createdChannels.get(nameC).getOwner())) {
            createdChannels.remove(nameC);
        } else {
            createdChannels.get(nameC).remove(nickname);
        }
    }

//helper function: find channel by name 
    public Channel getChannelByChannelName(String nameC) {
        return createdChannels.get(nameC);
    }

}
