package com.pas.chat.iq;


import org.jivesoftware.smack.packet.IQ;

public class RequestChatIQ extends IQ {

    private String _sender;

    public RequestChatIQ(String to) {
        super("request", "chat:iq:request");

        setType(Type.set);
        setTo(to + "/Smack");
    }

    public void setSender(String sender) {
        _sender = sender;
    }


    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.openElement("sender");
        xml.append(_sender);
        xml.closeElement("sender");
        return xml;
    }
}
