import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;

public class ServerModelTest {
    private ServerModel model;

    /**
     * Before each test, we initialize model to be a new ServerModel (with all new,
     * empty state)
     */
    @BeforeEach
    public void setUp() {
        // We initialize a fresh ServerModel for each test
        model = new ServerModel();
    }

    /**
     * Here is an example test that checks the functionality of your changeNickname
     * error handling. Each line has commentary directly above it which you can use
     * as a framework for the remainder of your tests.
     */
    @Test
    public void testInvalidNickname() {
        // A user must be registered before their nickname can be changed,
        // so we first register a user with an arbitrarily chosen id of 0.
        model.registerUser(0);

        // We manually create a Command that appropriately tests the case
        // we are checking. In this case, we create a NicknameCommand whose
        // new Nickname is invalid.
        Command command = new NicknameCommand(0, "User0", "!nv@l!d!");

        // We manually create the expected Broadcast using the Broadcast
        // factory methods. In this case, we create an error Broadcast with
        // our command and an INVALID_NAME error.
        Broadcast expected = Broadcast.error(command, ServerResponse.INVALID_NAME);

        // We then get the actual Broadcast returned by the method we are
        // trying to test. In this case, we use the updateServerModel method
        // of the NicknameCommand.
        Broadcast actual = command.updateServerModel(model);

        // The first assertEquals call tests whether the method returns
        // the appropriate Broadcast.
        assertEquals(expected, actual, "Broadcast");

        // We also want to test whether the state has been correctly
        // changed.In this case, the state that would be affected is
        // the user's Collection.
        Collection<String> users = model.getRegisteredUsers();

        // We now check to see if our command updated the state
        // appropriately. In this case, we first ensure that no
        // additional users have been added.
        assertEquals(1, users.size(), "Number of registered users");

        // We then check if the username was updated to an invalid value
        // (it should not have been).
        assertTrue(users.contains("User0"), "Old nickname still registered");

        // Finally, we check that the id 0 is still associated with the old,
        // unchanged nickname.
        assertEquals("User0", model.getNickname(0), "User0");
    }

    /*
     * Your TAs will be manually grading the tests you write in this file. Don't
     * forget to test both the public methods you have added to your ServerModel
     * class as well as the behavior of the server in different scenarios. You might
     * find it helpful to take a look at the tests we have already provided you with
     * in ChannelsMessagesTest, ConnectionNicknamesTest, and InviteOnlyTest.
     */

    @Test
    public void testGetId() {
        model.registerUser(5);
        Command command = new NicknameCommand(5, "User 5", "5");
        command.updateServerModel(model);
        assertEquals(5, model.getUserId("5"), "get ID of user");

    }

    @Test
    public void testRegister() {

        model.registerUser(1);
        model.registerUser(2);
        model.registerUser(3);
        Command command = new NicknameCommand(1, "User 1", "1");

        // Test if name is valid or not
        Broadcast expected = Broadcast.okay(command, Collections.singleton("User 1"));
        Broadcast actual = command.updateServerModel(model);
        assertEquals(expected, actual, "Valid name");

        // tests getNickname function
        assertEquals("1", model.getNickname(1), "check getNickname function");

        // tests getRegisteredUsers function
        Collection<String> registeredUsers = model.getRegisteredUsers();
        assertEquals(3, registeredUsers.size(), "Number of registered users");

    }

    @Test
    public void testDeregister() {
        model.registerUser(3);
        model.deregisterUser(3);
        model.registerUser(4);

        assertNull(null, model.getNickname(3));

        // invite an invalid user(deregistered) to a channel
        model.create("Owner4", model.getNickname(4), false);
        Command command = new InviteCommand(4, model.getNickname(4), "Owner4", "3");
        Broadcast expected = Broadcast.error(command, ServerResponse.NO_SUCH_USER);
        Broadcast actual = command.updateServerModel(model);
        assertEquals(expected, actual, "Broadcast");
    }

    @Test
    public void testJoinAndLeave() {
        model.registerUser(2);
        model.registerUser(3);
        model.registerUser(4);
        // owner is 2
        model.create("Owner2", model.getNickname(2), false);
        // Add users 3 and 4 twice
        Channel chan = model.getChannelByChannelName("Owner2");
        model.join(chan, model.getNickname(3));
        model.join(chan, model.getNickname(4));
        model.join(chan, model.getNickname(3));
        model.join(chan, model.getNickname(4));
        Collection<String> usersInTestChan = model.getUsersInChannel("Owner2");
        assertEquals(3, usersInTestChan.size(), "Join channel 2 times");

        // Leave (user 3)
        Command command = new LeaveCommand(3, model.getNickname(3), "Owner2");
        command.updateServerModel(model);
        Collection<String> usersInTestChan2 = model.getUsersInChannel("Owner2");
        assertEquals(2, usersInTestChan2.size(), "Users in channel after user 3 leaves");
    }

    @Test
    public void testMessage() {
        model.registerUser(3);
        model.registerUser(4);
        model.registerUser(5);
        model.registerUser(6);
        model.create("Owner3", model.getNickname(3), false);
        model.create("Test2", model.getNickname(3), false);
        Channel chan = model.getChannelByChannelName("Owner3");
        model.join(chan, model.getNickname(4));
        model.join(chan, model.getNickname(5));
        Command command = new MessageCommand(6, model.getNickname(6), "Owner3", "Anh");
        Broadcast expected = Broadcast.error(command, ServerResponse.USER_NOT_IN_CHANNEL);
        Broadcast actual = command.updateServerModel(model);
        assertEquals(expected, actual, "User not in channel can't send message");

    }

// Test getOtherUsers function
    @Test
    public void testOtherUsers() {
        model.registerUser(4);
        model.registerUser(5);
        model.registerUser(6);

        model.create("Owner4", model.getNickname(4), false);
        Channel chan = model.getChannelByChannelName("Owner4");

        model.join(chan, model.getNickname(5));
        model.join(chan, model.getNickname(6));

        Collection<String> otherthan4 = model.getOtherUsers(model.getNickname(4));
        assertEquals(2, otherthan4.size(), "Number of other users");

        assertTrue(otherthan4.contains(model.getNickname(5)), "User 5");
        assertTrue(otherthan4.contains(model.getNickname(6)), "User 6");
        assertFalse(otherthan4.contains(model.getNickname(4)), "User 4");

    }
}
