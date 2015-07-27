package com.pas.chat;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.pas.chat.iq.CloseChatIQ;
import com.pas.chat.iq.RequestChatIQ;
import com.pas.chat.messages.ChatMessage;
import com.pas.chat.messages.MessageAdapter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private ListView _chatView;
    private EditText _chatMessage;
    private Button _chatButton;
    private ArrayList<ChatMessage> _messages;
    private MessageAdapter _messageAdapter;

    private ChatApplication _application;
    private Chat _chat;
    private ActionBar _actionBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        _application = (ChatApplication) getApplicationContext();

        initView();

        setButtonListener();
        setMessageListener();

        setPresence();
        setActionTitles();

        createDisconnectListener();
    }

    private void setPresence() {
        try {
            _application.getClient().setPresenceMode(Presence.Mode.available);
        } catch (Exception e) {
            Log.d("ChatActivity.java", e.getMessage());
        }
    }

    private void setActionTitles() {
        _actionBar.setTitle("Chat");
        _actionBar.setSubtitle("Connected to " + _chat.getParticipant());
    }

    private void createDisconnectListener() {
        _application.getClient().getConnection().addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                Log.d("[INFO]", "Here we call disconnect()");
                disconnect();
            }
        }, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                return (stanza instanceof CloseChatIQ);
            }
        });
    }


    private void setButtonListener() {
        _chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = _chatMessage.getText().toString();
                try {
                    //@TODO REPLACE WITH ASYNCTASK FOR NETWORKING DELAY ETC NOT ON MAIN THREAD
                    new SendMessage().execute(message);
                    _chatMessage.setText("");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void disconnect() {
        if (_application.getChat() == null) {
            Log.d("[INFO]", "We can't disconnect because we don't have a chat (null).");
            return;
        }
        _application.getChat().close();
        _application.setChat(null);

        Intent intent = new Intent(_application.getApplicationContext(), StartChatActivity.class);
        startActivity(intent);
        finish();
    }

    private void setMessageListener() {
        _chat = _application.getChat();
        _chat.addMessageListener(new ChatMessageListener() {

            @Override
            public void processMessage(Chat chat, Message message) {
                final Message runnableMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMessage(new ChatMessage(runnableMessage, false));
                    }
                });
            }
        });
    }

    private void initView() {
        _chatView = (ListView) findViewById(R.id.chat_view);

        _actionBar = getSupportActionBar();

        _chatMessage = (EditText) findViewById(R.id.chat_message);
        _chatButton = (Button) findViewById(R.id.send_chat_message);


        _messages = new ArrayList<>();
        _messageAdapter = new MessageAdapter(this, _messages);
        _chatView.setAdapter(_messageAdapter);
    }

    private class SendMessage extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params) {
            try {
                _chat.sendMessage(params[0]);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(String text) {
            addMessage(new ChatMessage(text, true));
        }
    }

    private void addMessage(ChatMessage chatMessage)
    {
        _messages.add(chatMessage);
        _messageAdapter.notifyDataSetChanged();
        _chatView.setSelection(_messages.size() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave) {
            // Leave chat
            try {
                CloseChatIQ closeChat = new CloseChatIQ(_chat.getParticipant() + "/Smack");
                _application.getClient().getConnection().sendStanza(closeChat);

                Log.d("[INFO]", "Send our closeChat stanza");
            } catch (Exception e) {
                Log.d("ChatActivity.java", e.getMessage());
            }

            disconnect();
        }

        return super.onOptionsItemSelected(item);
    }
}
