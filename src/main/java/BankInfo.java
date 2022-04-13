import classes.Account;
import classes.Connections;
import classes.Logger;
import classes.User;

import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

//Front-End Class
public class BankInfo
{
    Scanner consoleInput;
    Connections conns;

    public static void main(String[] args)
    {
        new BankInfo(args[0], args[1], args[2], args[3]);
    }

    ///Attempt to connect to database if not display error and terminate.
    private BankInfo(String server, String database, String username, String password)
    {
        conns = new Connections();
        consoleInput = new Scanner(System.in);
        print(String.format("%s/%s?user=%s&password=%s", server, database, username, password));
        if (conns.connect(server, database, username, password))
        {
            while (this.startPage()) ;
            return;
        }
        print("Failed to connect to server.");
        enterToContinue();
    }

    ///Welcome user to app
    ///prompt to login or create account
    private boolean startPage()
    {
        clearSpace(20);
        String action = validateString("Welcome to Sterling Bank!\nHow can we help you today?\n1: Access account\n2: Make a New Account\n0: Quit", 1, 10);
        switch (action)
        {
            case "1":
                while (login()) ;
                break;
            case "2":
                while (makeNewUserAccount()) ;
                break;
            case "0":
                print("Thank you for visiting Sterling Bank.");
                enterToContinue();
                return false;
            default:
                break;
        }
        return true;
    }

    ///Loop login (from startPage()) until user is successfully logged in or prompted to quit
    private boolean login()
    {
        clearSpace(20);
        String username = validateString("Please enter your username.", 5, 20);
        String password = validateString("Please enter your password.", 8, 20);

        User user = conns.login(username, password);
        if (user != null)
        {
            clearSpace(20);
            while (userMenu(user)) ;
            return false;
        }
        print("Failed to login\nTry Again? Y for Yes, anything else to quit");
        return consoleInput.next().trim().toLowerCase(Locale.ROOT).charAt(0) == 'y';
    }

    ///looped user menu to prompt them on action
    private boolean userMenu(User user)
    {
        clearSpace(20);
        print(String.format("Welcome %s %s.\nWhat would you like to do today?\n1:View Accounts\n2:Open a new account\n0:Exit", user.getFirstName(), user.getLastName()));
        switch (consoleInput.next())
        {
            case "1":
                while (displayAllAccounts(user)) ;
                return true;
            case "2":
                openNewBankAccount(user);
                return true;
            case "0":
                return false;
            default:
                return true;
        }
    }

    ///Prints all accounts aside from an optional skip value
    private void printAccounts(Account[] accounts, int skip)
    {
        String sep = "________________________________________________________________________________";
        print("Available Accounts:\n" + sep);
        for (int i = 0; i < accounts.length; i++)
        {
            if (i == skip) continue;
            Account account = accounts[i];
            print(String.format("%d\t:\t%s", i + 1, account.toString()));
        }
        print(sep);
        print("");
    }

    ///Displays all accounts associated with user
    ///prompts to select and account or to initiate transfers between accounts
    private boolean displayAllAccounts(User user)
    {
        clearSpace(20);
        Account[] accounts = conns.getAccounts(user);

        printAccounts(accounts, -1);

        if (accounts.length > 0)
        {
            String action = validateString("Please select an account\nor input T to transfer between accounts\nor input No to quit", 1, 25).trim().toLowerCase(Locale.ROOT);
            if (!Pattern.compile("\\D").matcher(action).find())
            {
                try
                {
                    int parse = Integer.parseInt(action) - 1;
                    if (parse >= 0 && parse < accounts.length)
                    {
                        while (openAccount(accounts[parse])) ;
                    }
                }
                catch (Exception e)
                {
                    Logger.logError("Failed to parse string " + action + " " + e);
                    e.printStackTrace();
                }
            }
            else if (action.charAt(0) == 't')
            {
                while (transfer(accounts)) ;
            }
            else return action.charAt(0) != 'n';
            return true;
        }
        print("No accounts associated with this account");
        enterToContinue();
        return false;
    }

    ///transfer money between accounts that belong to user
    ///prints accounts to select from
    private boolean transfer(Account[] accounts)
    {
        if (accounts.length < 2)
        {
            print("Unable to transfer between accounts with less than two accounts associated with user");
            enterToContinue();
            return false;
        }
        int fromAccount;
        int toAccount;
        do
        {
            clearSpace(20);
            printAccounts(accounts, -1);
            fromAccount = validateInteger("Please select an account to transfer from", true, true) - 1;
        } while (fromAccount >= accounts.length);

        do
        {
            clearSpace(20);
            printAccounts(accounts, fromAccount);
            toAccount = validateInteger("Please select an account to transfer to", true, true) - 1;
        } while (toAccount >= accounts.length);

        char confirmation = validateString(String.format("Transfer from %s account to %s account?\nEnter Y for yes\nEnter N for no\nEnter anything else to quit", accounts[fromAccount].verbalString(), accounts[toAccount].verbalString()), 1, 10).toLowerCase(Locale.ROOT).charAt(0);

        switch (confirmation)
        {
            case 'y':
                break;
            case 'n':
                return true;
            default:
                return false;
        }
        double value;
        do
        {
            value = validateDouble(String.format("Please enter the transfer amount.\nFunds available: $%.2f", accounts[fromAccount].getBalance()), true);
        } while (value > accounts[fromAccount].getBalance());
        conns.updateTransaction(accounts[fromAccount], -value);
        conns.updateTransaction(accounts[toAccount], value);

        return false;
    }

    ///opens menu associated with an account
    ///prompts actions to take
    private boolean openAccount(Account account)
    {
        clearSpace(20);
        print(String.format("%s account\n\t%s\nBalance $%,.2f\n", account.getAccountName(), account.getAccountType(), /*account.getBalance() < 0 ? '-' : '+',*/ account.getBalance()));
        String userChoice = validateString("1: Deposit\n2: Withdrawal\n3: Print Transaction history\n4: Add another user as joint account\n\nPlease select an action or input No to quit", 1, 10);
        if (!Pattern.compile("\\D").matcher(userChoice).find())
        {
            try
            {
                switch (Integer.parseInt(userChoice))
                {
                    case 1:
                        deposit(account);
                        break;
                    case 2:
                        withdraw(account);
                        break;
                    case 3:
                        conns.getTransactionHistory(account);
                        enterToContinue();
                        break;
                    case 4:
                        addJoint(account);
                        break;
                    default:
                        break;
                }
            }
            catch (Exception e)
            {
                Logger.logError("Failed to parse string " + userChoice + " " + e);
                e.printStackTrace();
            }
            return true;
        }
        return userChoice.trim().toLowerCase(Locale.ROOT).charAt(0) != 'n';
    }

    ///Displays associated users
    ///prompts to add an additional user
    private void addJoint(Account account)
    {
        clearSpace(20);
        User[] jointUsers = conns.getAccountUsers(account);
        print(String.format("*****\t%s[%s]\t*****\n***********************\nApproved Users:", account.getAccountName(), account.getAccountType()));
        for (User user : jointUsers)
        {
            print(String.format("Username: %s\n\tFirst Name:\t%s\n\tLast Name:\t%s\n", user.getUsername(), user.getFirstName(), user.getLastName()));
        }
        String userConfirmation = validateString("\nAdd another existing user to account? Y for yes", 1, 10);
        if (userConfirmation.toLowerCase(Locale.ROOT).charAt(0) != 'y') return;
        User newJointUser;
        do
        {
            String username = validateString("Please enter the new user's username.", 5, 20);
            String password = validateString("Please enter the new user's password.", 8, 20);

            newJointUser = conns.login(username, password);
            if (newJointUser == null)
            {
                print("Failed to confirm user\nTry Again? Y for Yes, anything else to quit");
                if (consoleInput.next().trim().toLowerCase(Locale.ROOT).charAt(0) != 'y') return;
            }
        } while (newJointUser == null);

        if (conns.associateAccount(account, newJointUser))
        {
            print("Successfully added " + newJointUser.getUsername() + " as a Joint User");
            enterToContinue();
        }
    }

    ///deposit funds into account
    private void deposit(Account account)
    {
        double value = 0d;
        do
        {
            clearSpace(20);
            if (value < 0d) Logger.logWarning("Please enter a positive value to deposit into the account");
            value = validateDouble(String.format("Currently available funds $%.2f\nPlease input how much you wish to deposit.", account.getBalance()), false);
        } while (value < 0d);
        conns.updateTransaction(account, value);
    }

    ///Remove funds from account
    private void withdraw(Account account)
    {
        double value = 0d;
        do
        {
            clearSpace(20);
            if (value < 0d) print("Please enter a positive value to withdrawal from the account.");
            if (account.getBalance() - value < 0d) print("Unable to withdraw more than available funds.");
            value = validateDouble(String.format("Currently available funds $%.2f\nPlease input how much money you wish to withdraw.", account.getBalance()), false);
        } while (account.getBalance() - value < 0d || value < 0d);
        conns.updateTransaction(account, -value);
    }

    ///creates anew new account associated with user
    private void openNewBankAccount(User user)
    {
        clearSpace(20);
        String[] accountTypes = conns.getAccountTypes();
        int action;
        do
        {
            for (int i = 0; i < accountTypes.length; i++)
            {
                print(String.format("%d: %s", i + 1, accountTypes[i]));
            }

            action = validateInteger("Please choose an Account type.\nPlease input a value between 1 and " + (accountTypes.length), true, true);
        } while (action < 1 || action > accountTypes.length);

        String accountName = validateString("Please enter an Account name.\nSingle Character will default to Type name.", 1, 20);
        if (accountName.length() == 1)
            accountName = accountTypes[action - 1].substring(0, Math.min(20, accountTypes[action - 1].length()));
        conns.openNewAccount(accountName, action, accountTypes[action - 1], user);
    }

    ///check for valid characters
    public static boolean validCharacters(String comparison)
    {
        return !Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]$").matcher(comparison).find();
    }

    public static boolean validPassword(String comparison)
    {

        if (comparison.length() < 8 || comparison.length() > 20) return false;
        return validCharacters(comparison) && trueRegex("[A-Z]", comparison) && trueRegex("[a-z]", comparison) && trueRegex("[\\d]", comparison) && trueRegex("[@$!%*#?&]", comparison);
    }

    private static boolean trueRegex(String regex, String comparison)
    {
        return Pattern.compile(regex).matcher(comparison).find();
    }

    ///create a new user account
    private boolean makeNewUserAccount()
    {
        clearSpace(20);
        String firstName = validateString("Please enter your first name.", 2, 255);
        String lastName = validateString("Please enter your last name.", 2, 255);
        String username;
        boolean invalidUsername;
        do
        {
            username = validateString("Please enter your username.", 5, 20);
            invalidUsername = !conns.validUsername(username);
            if (invalidUsername) print("Username unavailable");
        } while (invalidUsername);

        String password;
        do
        {
            password = validateString("Please enter your password.\n1: Password must contain a number 0-9\n2: Lower and Uppercase letters\n3: A Special Character: @$!%*#?& and be at least 8 characters in length.", 8, 20);
        } while (!validPassword(password));

        switch (validateString(String.format("Please confirm the following info:\nFirst Name:\t%s\nLast Name:\t%s\nUsername:\t%s\nPassword:\t%s\nConfirm? Y for Yes, N for No and anything else to quit", firstName, lastName, username, password), 1, 10).trim().toLowerCase(Locale.ROOT).charAt(0))
        {
            case 'y':
                User newUser = new User(username, password, firstName, lastName);
                if (conns.createNewUser(newUser))
                {
                    Logger.logDebug("Successfully created account for " + newUser);
                    print("Successfully created account, Please log in.");
                    enterToContinue();
                    consoleInput.next();
                    return false;
                }
                Logger.logError("Failed to create account.\nPlease try again.");
                return true;
            case 'n':
                return true;
            default:
                return false;
        }
    }

    ///confirm integer input
    private int validateInteger(String Prompt, boolean positive, boolean aboveZero)
    {
        while (true)
        {
            String validatedString = validateString(Prompt, 1, 255);
            try
            {
                int result = Integer.parseInt(validatedString);
                if (!positive || positive && result >= (aboveZero ? 1 : 0)) return result;
            }
            catch (Exception e)
            {
                Logger.logError("Failed to parse string " + validatedString + " " + e);
                e.printStackTrace();
            }
        }
    }

    ///confirm double input
    private double validateDouble(String Prompt, boolean positive)
    {
        while (true)
        {
            String validatedString = validateString(Prompt, 1, 255);
            try
            {
                double result = Double.parseDouble(validatedString);
                if (!positive || positive && result >= 0d) return result;
            }
            catch (Exception e)
            {
                Logger.logError("Failed to parse string " + validatedString + " " + e);
                e.printStackTrace();
            }
        }
    }

    ///confirm valid string
    private String validateString(String Prompt, int minLength, int max)
    {
        boolean check;
        String result;
        do
        {
            print(Prompt);
            result = consoleInput.next().trim();
            check = result.toLowerCase(Locale.ROOT).equals("null") || !validCharacters(result) || result.length() < minLength || result.length() >= max;
        } while (check);
        return result;
    }

    private void enterToContinue()
    {
        print("Press Enter key to continue...");
        try
        {
            System.in.read();
        }
        catch (Exception e)
        {
        }
    }

    private void clearSpace(int lineCount)
    {
        for (int i = 0; i < lineCount; i++)
        {
            print("");
        }
    }

    private static void print(String value)
    {
        System.out.println(value);
    }
}
