package com.pas.chat.iq;


import org.jivesoftware.smack.packet.IQ;

public class CloseChatIQ extends IQ {

    public CloseChatIQ() {
        super("close", "chat:iq:request");
        setType(Type.set);
    }

    public CloseChatIQ(String to) {
        super("close", "chat:iq:request");

        setType(Type.set);
        setTo(to);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }
}
