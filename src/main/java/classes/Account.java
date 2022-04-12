package classes;

public class Account
{
    private String accountIdentifier;
    private String accountName;

    public String getAccountType()
    {
        return accountType;
    }

    private String accountType;

    private double balance;

    public String getAccountIdentifier()
    {
        return accountIdentifier;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public double getBalance()
    {
        return balance;
    }

    public void sumBalance(double cashFlow)
    {
        Logger.logMessage(String.format("Modifying account %s balance by %.2f from %.2f to %.2f",this.accountIdentifier,cashFlow,this.balance,this.balance+cashFlow));
        this.balance += cashFlow;
    }

    public Account(String identifier, String name, double bal, String type)
    {
        accountIdentifier = identifier;
        accountName = name;
        accountType = type;
        balance = bal;
    }

    @Override
    public String toString()
    {
        return String.format("%10s\t|\t%10s\t|\t%.2f", this.accountName,this.accountType, this.balance);
    }

    public String verbalString()
    {
        return String.format("%s[%s]", this.accountName,this.accountType);
    }
}
