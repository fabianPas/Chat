package com.pas.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pas.chat.iq.RequestChatIQ;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;

public class StartChatActivity extends Activity {

    private Button _search;
    private boolean _searching = false;
    private ProgressBar _progress;
    private ChatApplication _application;
    private TextView _userCount;

    private ChatManager _chatManager;
    private Thread _pickChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_chat);

        _application = (ChatApplication) getApplicationContext();

        initView();

        createChatListener();
        createChatRequestListener();

        RosterEntryTask createEntry = new RosterEntryTask(_application);
        createEntry.execute((Void) null);

        createSearchListener();

        startCount();
    }

    private void startCount() {
        new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        final int count = _application.getClient().getOnlineUsers();

                        _userCount.post(new Runnable() {
                            public void run() {
                                _userCount.setText(getString(R.string.user_count, count));
                            }
                        });

                        Thread.sleep(1500);
                    } catch (Exception e) {
                        Log.d("StartChatActivity.java", e.getMessage());
                        return;
                    }
                }
            }
        }).start();
    }

    private void createSearchListener() {
        _search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!_searching) {
                    SetPresenceTask setPresence = new SetPresenceTask(_application);
                    setPresence.execute(Presence.Mode.chat);
                    _search.setText("Cancel search..");
                    _progress.setVisibility(View.VISIBLE);

                    if (_pickChat == null)
                        _pickChat = new Thread(createSearchRunnable());

                    _pickChat.start();
                } else {
                    SetPresenceTask setPresence = new SetPresenceTask(_application);
                    setPresence.execute(Presence.Mode.available);
                    _progress.setVisibility(View.INVISIBLE);
                    _search.setText("I want to talk");
                    _pickChat.interrupt();
                    _pickChat = null;
                }

                _searching = !_searching;
            }
        });
    }

    private void initView() {
        _userCount = (TextView) findViewById(R.id.users_available);

        _progress = (ProgressBar) findViewById(R.id.action_searching);
        _progress.setVisibility(View.INVISIBLE);

        _search = (Button) findViewById(R.id.start_chat);
    }

    private void createChatListener() {
        _chatManager = ChatManager.getInstanceFor(_application.getClient().getConnection());
        _chatManager.addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                //@TODO Okay, so without this check it does work, but why doesn't it work when the check is in place
               /* if (_application.getChat() != null) {
                    Log.d("[INFO]", "We already have a chat set, which should not be possible.");
                    return;
                }*/

                try {
                    if (!createdLocally) {
                        Log.d("[INFO]", "Set that chat!");
                        _application.setChat(chat);
                        Intent intent = new Intent(_application.getApplicationContext(), ChatActivity.class);
                        startActivity(intent);
                        finish();
                    }


                } catch (Exception e) {
                    Log.d("StartChatActivity.java", e.getMessage());
                }
            }
        });
    }

    private void createChatRequestListener() {
        _application.getClient().getConnection().addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                try {
                    Chat chat = _chatManager.createChat(packet.getFrom().replace("/Smack", ""));

                    if (chat != null) {
                        _application.setChat(chat);
                        chat.sendMessage("Handshake!");

                        Log.d("[INFO]", "We actually did receive a packet, sent the handshake and set the chat.");

                        Intent intent = new Intent(_application.getApplicationContext(), ChatActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e) {
                    Log.d("StartChatActivity.java", e.getMessage());
                }

            }
        }, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                return (stanza instanceof RequestChatIQ);
            }
        });
    }

    private Runnable createSearchRunnable() {
        return new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (_application.getChat() == null) {
                            String user = _application.getClient().getAvailableUser();

                            if (user != null) {
                                RequestChatIQ requestChat = new RequestChatIQ(user);
                                requestChat.setSender(_application.getClient().getConnection().getUser());
                                _application.getClient().getConnection().sendStanza(requestChat);
                            }
                        }

                        Thread.sleep(1000);
                    } catch (Exception e) {
                        if (!(e instanceof InterruptedException))
                            e.printStackTrace();

                        return;
                    }
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        _pickChat.interrupt();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class RosterEntryTask extends AsyncTask<Void, Void, Integer> {

        private ChatApplication mApplication;

        public RosterEntryTask(ChatApplication application) {
            mApplication = application;
        }


        @Override
        protected Integer doInBackground(Void... params) {
            int count = 0;

            try {
                count = mApplication.getClient().getOnlineUsers();
            } catch (Exception e) {
                Log.d("StartChatActivity.java", e.getMessage());
            }

            return count;
        }

        @Override
        protected void onPostExecute(Integer count) {
            _userCount.setText(getString(R.string.user_count, count));
        }
    }

    public class SetPresenceTask extends AsyncTask<Presence.Mode, Void, Void> {

        private ChatApplication mApplication;

        public SetPresenceTask(ChatApplication application) {
            mApplication = application;
        }

        @Override
        protected Void doInBackground(Presence.Mode... params) {
            try {
                mApplication.getClient().setPresenceMode(params[0]);
            } catch (Exception e) {
                Log.d("StartChatActivity.java", "Unable to set user presence: " + e.getMessage());
            }

            return null;
        }
    }
}
