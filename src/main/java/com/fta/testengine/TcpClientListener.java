package com.fta.testengine;

public interface TcpClientListener {
    void received(byte[] msg);

    void sent(byte[] sentData);

    void connected();

    void disConnected(int code, String msg);
}
