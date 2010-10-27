package net.spy.memcached.vbucket;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

import java.util.Collection;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.net.SocketAddress;
import java.io.IOException;

/**
 * @author alexander.sokolovsky.a@gmail.com
 */
public class MemcachedNodeMockImpl implements MemcachedNode {
    private SocketAddress socketAddress;
    public void addOp(Operation op) {
    }

    public void authComplete() {
    }

    public void connected() {
    }

    public void copyInputQueue() {
    }

    public Collection<Operation> destroyInputQueue() {
        return null;
    }

    public void fillWriteBuffer(boolean optimizeGets) {
    }

    public void fixupOps() {
    }

    public int getBytesRemainingToWrite() {
        return 0;
    }

    public SocketChannel getChannel() {
        return null;
    }

    public int getContinuousTimeout() {
        return 0;
    }

    public Operation getCurrentReadOp() {
        return null;
    }

    public Operation getCurrentWriteOp() {
        return null;
    }

    public ByteBuffer getRbuf() {
        return null;
    }

    public int getReconnectCount() {
        return 0;
    }

    public int getSelectionOps() {
        return 0;
    }

    public SelectionKey getSk() {
        return null;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public ByteBuffer getWbuf() {
        return null;
    }

    public boolean hasReadOp() {
        return true;
    }

    public boolean hasWriteOp() {
        return true;
    }

    public void insertOp(Operation o) {
    }

    public boolean isActive() {
        return true;
    }

    public void reconnecting() {
    }

    public void registerChannel(SocketChannel ch, SelectionKey selectionKey) {
    }

    public Operation removeCurrentReadOp() {
        return null;
    }

    public Operation removeCurrentWriteOp() {
        return null;
    }

    public void setChannel(SocketChannel to) {
    }

    public void setContinuousTimeout(boolean timedOut) {
    }

    public void setSk(SelectionKey to) {
    }

    public void setupForAuth() {
    }

    public void setupResend() {
    }

    public void transitionWriteItem() {
    }

    public int writeSome() throws IOException {
        return 0;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }
}
