package classes;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;


public class User
{
    private String username;
    private String password;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    private String firstName;
    private String lastName;

    public String getFirstName()
    {
        return firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public User(String user, String pass, String first, String last)
    {
        username = user;
        password = pass;
        firstName = first;
        lastName = last;
    }

    void prepareStatement(PreparedStatement statement) throws SQLException
    {
        statement.setString(1,this.username.toLowerCase(Locale.ROOT));
        statement.setString(2,this.password);
        statement.setString(3,this.firstName);
        statement.setString(4,this.lastName);
    }

    @Override
    public String toString()
    {
        return username+" ("+lastName+")";
    }
}