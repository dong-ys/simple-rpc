package com.dys.rpc.message;

public class PingMessage extends com.dys.rpc.message.Message {
    @Override
    public int getMessageType() {
        return PingMessage;
    }
}
