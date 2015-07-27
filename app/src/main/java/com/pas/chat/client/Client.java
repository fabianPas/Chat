package com.pas.chat.client;

import android.util.Log;

import com.pas.chat.client.exceptions.InvalidGroupException;
import com.pas.chat.client.exceptions.NotPermittedException;
import com.pas.chat.iq.CloseChatProvider;
import com.pas.chat.iq.RequestChatProvider;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.login.LoginException;

public class Client {
    public static final String HOST = "10.0.3.2"; // genymotion: 10.0.3.2, android adm: 10.0.2.2
    public static final String DOMAIN = "fabian-pc";
    public final String GROUP_NAME = "Chat";

    public String _searchService;
    private String _username;
    private AbstractXMPPConnection _connection;
    private Roster _roster;
    private Random _randomInstance = new Random();

    public Client()
    {
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(Client.DOMAIN)
                .setHost(Client.HOST)
                .setPort(5222)
                .setDebuggerEnabled(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled) // We want to get back to SSL later
                .build();

        _connection = new XMPPTCPConnection(configuration);
        setSearchService();
    }

    public Client(String username, String password)
    {
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username, password)
                .setServiceName(Client.DOMAIN)
                .setHost(Client.HOST)
                .setPort(5222)
                .setDebuggerEnabled(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled) // We want to get back to SSL later
                .build();

        _connection = new XMPPTCPConnection(configuration);
        _username = username;

        setSearchService();

        ProviderManager.addIQProvider("request", "chat:iq:request", new RequestChatProvider());
        ProviderManager.addIQProvider("close", "chat:iq:request", new CloseChatProvider());

        Log.d("Client.java", "Created XMPP TCP object");
    }

    public void setSearchService() {
        _searchService = "search." + this.getConnection().getServiceName();
    }

    public void createAccount(String username, String password, Map<String, String> attributes)
    {
        AccountManager accountManager = AccountManager.getInstance(this.getConnection());
        accountManager.sensitiveOperationOverInsecureConnection(true);

        try {
            accountManager.createAccount(username, password, attributes);
        } catch (Exception e) {
            Log.d("Client.java", e.getMessage());
        }
    }

    public boolean propertyExists(String property, String value) {

        try {
            UserSearchManager search = new UserSearchManager(getConnection());
            Form searchForm = search.getSearchForm(_searchService);
            Form answerForm = searchForm.createAnswerForm();
            answerForm.setAnswer(property, true);
            answerForm.setAnswer("search", value);
            ReportedData data = search.getSearchResults(answerForm, _searchService);

            if (data.getRows() != null) {
                List<ReportedData.Row> rows = data.getRows();
                if (rows.size() != 0) {
                    Log.d("RegisterActivity.java", "EMAIL EXISTS ALREADY");
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.d("Client.java", "Error when checking if property exists: " + e.getMessage());
        }

        return false;
    }

    public void login() throws LoginException
    {
        try {
            _connection.login();
            _roster = Roster.getInstanceFor(_connection);
        }
        catch (Exception e)
        {
            // @TODO instanceof SASLEXception to clarify login/connection exceptions
            Log.d("Client.java", e.getMessage());
            throw new LoginException();
        }
    }


    public void setPresenceMode(Presence.Mode mode) throws NotPermittedException {
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(mode);

        try {
            _connection.sendStanza(presence);
        } catch (Exception e) {
            throw new NotPermittedException(e.getMessage());
        }
    }

    public String getAvailableUser() throws InvalidGroupException, NotPermittedException {
        ArrayList<RosterEntry> available = new ArrayList<>();

        try {
            if (!_roster.isLoaded())
                _roster.reloadAndWait();

        } catch (Exception e) {
            throw new NotPermittedException(e.getMessage());
        }

        if (_roster.getGroup(GROUP_NAME) == null) {
            throw new InvalidGroupException("Group not found. (Is it a shared group?)");
        }

        Collection<RosterEntry> entries = _roster.getGroup(GROUP_NAME).getEntries();

        for (RosterEntry entry : entries) {
            if(_roster.getPresence(entry.getUser()).getMode() == Presence.Mode.chat  && !entry.getName().equals(_username)) {
                available.add(entry);
            }
        }

        if (available.size() == 0)
            return null;

        RosterEntry availableEntry = available.get(_randomInstance.nextInt(available.size()));
        return availableEntry.getUser();
    }

    public int getOnlineUsers() throws InvalidGroupException, NotPermittedException
    {
        try {
            if (!_roster.isLoaded())
                _roster.reloadAndWait();

        } catch (Exception e) {
            throw new NotPermittedException(e.getMessage());
        }

        if (_roster.getGroup(GROUP_NAME) == null) {
            throw new InvalidGroupException("Group not found. (Is it a shared group?)");
        }

        Collection<RosterEntry> entries = _roster.getGroup(GROUP_NAME).getEntries();

        int onlineCount = 0;
        for (RosterEntry entry : entries) {
            if(_roster.getPresence(entry.getUser()).getMode() == Presence.Mode.chat  && !entry.getName().equals(_username)) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    public void connect()
    {
        try {
            _connection.connect();
            Log.d("Client.java", "connect() connected");
        }
        catch (Exception e)
        {
            Log.d("Client.java", e.getMessage());
        }
    }

    public AbstractXMPPConnection getConnection()
    {
        return _connection;
    }
}
