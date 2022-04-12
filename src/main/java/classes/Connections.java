package classes;

import java.io.PrintStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;


public class Connections
{
    private Connection conn;

    public Connections()
    {

    }

    public boolean connect(String server, String database, String username, String password)
    {
        try
        {
            //conn = DriverManager.getConnection("jdbc:postgresql://razgriz-db.c7cl5ukvaqrg.us-east-2.rds.amazonaws.com/sterlingbank?user=postgres&password=u3UnU6Vc0hn0zmgLpI");
            conn = DriverManager.getConnection(String.format( "%s/%s?user=%s&password=%s",server,database,username,password));
        }
        catch (SQLException e)
        {
            System.err.println("Failed to connect to server " + e);
            return false;
        }
        return true;
    }

    public boolean validUsername(String username)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("select * from userDatabase where username = ?");
            statement.setString(1, username.toLowerCase(Locale.ROOT));
            ResultSet results = statement.executeQuery();
            return !results.next();
        }
        catch (Exception e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }
        return false;

    }

    public boolean createNewUser(User user)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("insert into userDatabase values (?,?,?,?)");
            user.prepareStatement(statement);
            statement.execute();
            return true;
        }
        catch (SQLException e)
        {
            Logger.logFatal("Failed to create new User");
            e.printStackTrace();
        }
        return false;
    }

    public User login(String username, String passcode)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("select * from userDatabase where username = ? and passcode = ?");
            statement.setString(1, username.toLowerCase(Locale.ROOT));
            statement.setString(2, passcode);
            ResultSet results = statement.executeQuery();
            if (!results.next())
            {
                String invalidAttempt = "Invalid username or password";
                Logger.logWarning(invalidAttempt);
            }
            else
            {
                return new User(results.getString("username"), results.getString("passcode"), results.getString("firstName"), results.getString("lastName"));
            }
        }
        catch (Exception e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }
        return null;
    }

    public Account[] getAccounts(User user)
    {
        Account[] accounts = null;
        try
        {
            PreparedStatement statement = conn.prepareStatement(String.format("select * from userAccountDatabase where username = '%s'", user.getUsername()), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet userAccounts = statement.executeQuery();
            userAccounts.last();
            accounts = new Account[userAccounts.getRow()];
            userAccounts.beforeFirst();
            for (int i = 0; i < accounts.length; i++)
            {
                userAccounts.next();
                String accountIdentifier = userAccounts.getString("accountIdentifier");
                statement = conn.prepareStatement(String.format("select * from accountDatabase left join accountType on accountDatabase.accountType=accountType.accountType where accountIdentifier = '%s'", accountIdentifier));
                ResultSet accountResult = statement.executeQuery();
                if (accountResult.next())
                    accounts[i] = new Account(accountIdentifier, accountResult.getString("accountName"), accountResult.getDouble("balance"), accountResult.getString("accountTypeName"));
            }
        }
        catch (Exception e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }

        return accounts;
    }

    public User[] getAccountUsers(Account account)
    {
        User[] users = null;
        try
        {
            PreparedStatement statement = conn.prepareStatement("select * from userAccountDatabase left join userdatabase on userAccountDatabase.username = userdatabase.username where accountidentifier = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.setString(1, account.getAccountIdentifier());
            ResultSet userAccounts = statement.executeQuery();
            userAccounts.last();
            users = new User[userAccounts.getRow()];
            userAccounts.beforeFirst();
            for (int i = 0; i < users.length; i++)
            {
                userAccounts.next();
                users[i] = new User(userAccounts.getString("username"), userAccounts.getString("passcode"), userAccounts.getString("firstname"), userAccounts.getString("lastname"));
            }
        }
        catch (Exception e)
        {
            System.err.println("An Error has occurred " + e);
            e.printStackTrace();
        }

        return users;
    }

    public boolean validateUniqueAccount(String identify)
    {
        ResultSet results = getAccountResultSet(identify);
        try
        {
            if (results == null || !results.next()) return true;
        }
        catch (SQLException e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }
        return false;
    }

    public Account openNewAccount(String accountName, int accountType, String accountTypeName, User user)
    {
        try
        {
            String accountIdentity;
            ResultSet results;
            do
            {
                accountIdentity = Logger.generateIdentifier();
                results = getAccountResultSet(accountIdentity);
            } while (results == null || results.next());

            PreparedStatement statement = conn.prepareStatement("insert into accountDatabase values (?,?,?,?)");
            statement.setString(1, accountIdentity);
            statement.setString(2, accountName);
            statement.setDouble(3, 0);
            statement.setInt(4, accountType);
            statement.execute();

            statement = conn.prepareStatement("insert into userAccountDatabase values (?,?)");
            statement.setString(1, user.getUsername());
            statement.setString(2, accountIdentity);
            statement.execute();

            return new Account(accountIdentity, accountName, 0, accountTypeName);
        }
        catch (SQLException e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }
        return null;
    }

    public boolean associateAccount(Account account, User newUser)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("insert into userAccountDatabase values (?,?)");
            statement.setString(1, newUser.getUsername());
            statement.setString(2, account.getAccountIdentifier());
            statement.execute();
            Logger.logDebug(String.format("Associated Account %s with User %s",account.getAccountName(),newUser.getUsername()));
            return  true;
        }
        catch (Exception e)
        {
            Logger.logError(String.format("Failed to associate Account %s with User %s %s",account.getAccountName(),newUser.getUsername(),e));
        }
        return  false;
    }

    public void deleteAccount(Account account)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("delete from userAccountDatabase where accountIdentifier = ? ");
            statement.setString(1, account.getAccountIdentifier());
            statement.execute();

            statement = conn.prepareStatement("delete from bankTransactionDatabase where accountIdentifier = ? ");
            statement.setString(1, account.getAccountIdentifier());
            statement.execute();

            statement = conn.prepareStatement("delete from accountDatabase where accountIdentifier = ? ");
            statement.setString(1, account.getAccountIdentifier());
            statement.execute();
        }
        catch (SQLException e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }
    }

    private ResultSet getAccountResultSet(String identify)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement(String.format("select * from accountDatabase where accountIdentifier = '%s'", identify));
            return statement.executeQuery();
        }
        catch (Exception e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }
        return null;
    }

    public void updateTransaction(Account account, double value)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("update accountDatabase set balance = ? where accountIdentifier = ?;");
            statement.setDouble(1, value + account.getBalance());
            statement.setString(2, account.getAccountIdentifier());
            statement.execute();
            createTransactionLog(account.getAccountIdentifier(), value);
            account.sumBalance(value);
        }
        catch (SQLException e)
        {
            Logger.logFatal(String.format("Failed to adjust balance by $%f for account %s " + e, value, account.getAccountIdentifier()));
            e.printStackTrace();
        }
    }

    public void getTransactionHistory(Account account)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("select * from bankTransactionDatabase where accountIdentifier = ?;");
            statement.setString(1, account.getAccountIdentifier());
            ResultSet results = statement.executeQuery();
            int i = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            GregorianCalendar calendar = new GregorianCalendar();
            StringBuilder sep= new StringBuilder();
            for (int j = 0; j < 5; j++)
            {
                sep.append("**********");
            }
            System.out.println(sep);
            while (results.next())
            {
                double value = results.getDouble("transactionValue");
                calendar.setTimeInMillis(results.getLong("transactionDate"));

                System.out.printf("%d: \t%s$%.2f | %5s\n", i++, value < 0 ? "-" : '+', Math.abs(value), sdf.format(calendar.getTime()));
                System.out.println(sep);
            }
        }
        catch (SQLException e)
        {
            Logger.logFatal("Failed to get transaction log");
            e.printStackTrace();
        }
    }

    public String[] getAccountTypes()
    {
        String[] accounts = null;
        try
        {
            PreparedStatement statement = conn.prepareStatement("select * from accountType", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet accountTypes = statement.executeQuery();
            accountTypes.last();
            accounts = new String[accountTypes.getRow()];
            accountTypes.beforeFirst();
            for (int i = 0; i < accounts.length; i++)
            {
                accountTypes.next();
                accounts[i] = accountTypes.getString("accountTypeName");
            }
        }
        catch (Exception e)
        {
            System.err.println("An Error has occurred");
            e.printStackTrace();
        }

        return accounts;

    }

    private void createTransactionLog(String identity, double value)
    {
        try
        {
            String transactionIdentity;
            PreparedStatement statement;
            do
            {
                transactionIdentity = Logger.generateIdentifier();
                statement = conn.prepareStatement("select * from bankTransactionDatabase where accountIdentifier = ?;");
                statement.setString(1, transactionIdentity);

            } while (statement.executeQuery().next());
            statement = conn.prepareStatement("insert into bankTransactionDatabase values (?,?,?,?)");
            statement.setString(1, transactionIdentity);
            statement.setString(2, identity);
            statement.setDouble(3, value);
            statement.setLong(4, System.currentTimeMillis());
            statement.execute();
        }
        catch (SQLException e)
        {
            Logger.logFatal(String.format("Failed to document transaction of %s$%.2f for account %s " + e, value >= 0 ? '+' : "", value, identity));
            e.printStackTrace();
        }
    }
}
