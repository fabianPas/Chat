package com.pas.chat.iq;

import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class RequestChatProvider extends IQProvider<RequestChatIQ> {
    @Override
    public RequestChatIQ parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        String receiver = null;

        int type = parser.next();
        switch(type) {
            case XmlPullParser.START_TAG:
                String element = parser.getName();

                switch(element) {
                    case "sender":
                        receiver = parser.nextText();
                        Log.d("RECEIVER", receiver);
                }
        }

        return new RequestChatIQ(receiver);
    }
}
