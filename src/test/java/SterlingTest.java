import classes.*;
import org.junit.jupiter.api.*;

import static classes.Logger.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Random;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SterlingTest
{
    SterlingTest()
    {

    }

    static Connections connectionTest = new Connections();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @Order(0)
    public void LogTest()
    {
        log(LogLevel.message, "Message Test");
        logDebug("Message Debug");
        logWarning("Message Warning");
        logError("Message Error");
        logFatal("Message Fatal");
    }

    @Test
    @Order(0)
    public void connectionTest()
    {
        Assertions.assertTrue(connectionTest.connect("jdbc:postgresql://localhost","postgres","postgres","dumbpassword"));
    }

    @Test
    @Order(1)
    public void successfulLoginTest()
    {
        User result = connectionTest.login("Jalil2", "dumbpassword");
        Assertions.assertTrue(result.getUsername().compareTo("jalil2") == 0);
    }

    @Test
    @Order(1)
    public void createAndDeleteTestAccount()
    {
        User dumbUser = connectionTest.login("Jalil2", "dumbpassword");
        Account sacAccount = connectionTest.openNewAccount("sacrificial account", 1, "Checking", dumbUser);
        Account[] testAccounts = connectionTest.getAccounts(dumbUser);
        boolean check = false;
        for (Account account : testAccounts)
        {
            if (account.getAccountIdentifier().compareTo(sacAccount.getAccountIdentifier()) == 0)
            {
                check = true;
                sacAccount=account;
            }
        }
        Assertions.assertTrue(check);
        Random rand= new Random();

        for (int i = 0, n = 5000; i < n ; i++)
        {
            int deposit=rand.nextInt(10000);
            connectionTest.updateTransaction(sacAccount, deposit);
            sacAccount.sumBalance(deposit);

            double withdraw=  Math.floor(-rand.nextDouble()*sacAccount.getBalance()*100)/100;
            connectionTest.updateTransaction(sacAccount, withdraw);
            sacAccount.sumBalance(withdraw);
        }
        connectionTest.getTransactionHistory(sacAccount);

        //delete Test
        connectionTest.deleteAccount(sacAccount);
        testAccounts = connectionTest.getAccounts(dumbUser);
        for (Account account : testAccounts)
        {
            if (account.getAccountIdentifier().compareTo(sacAccount.getAccountIdentifier()) == 0) check = false;
        }
        Assertions.assertTrue(check);
    }

    @Test
    @Order(1)
    public void failedUniqueAccount()
    {
        Assertions.assertFalse(connectionTest.validateUniqueAccount("Razgriz"));
    }
}
