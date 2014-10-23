package net.spy.memcached;

import java.net.InetSocketAddress;

/**
 * Container for the hostname and port of a Node, and resolve them into an
 * InetSocketAddress.
 */
public class HostPort {

  private final String host;
  private final int port;

  private volatile InetSocketAddress address;

  public HostPort(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHostName() {
    return host;
  }

  public int getPort() {
    return port;
  }

  /**
   * Get the InetSocketAddress. If it has not already been resolved then
   * resolve it.
   */
  public InetSocketAddress getAddress() {
    if (address == null) {
      resolveAddress();
    }
    return address;
  }

  /**
   * Resolve and return the InetSocketAddress. If it has already been resolved,
   * it will be re-resolved.
   */
  public InetSocketAddress resolveAddress() {
    address = new InetSocketAddress(host, port);
    return address;
  }

  @Override
  public String toString() {
    if (address == null) {
      return InetSocketAddress.createUnresolved(host, port).toString();
    }
    else {
      return address.toString();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostPort hostPort = (HostPort) o;

    if (port != hostPort.port) return false;
    if (host != null ? !host.equals(hostPort.host) : hostPort.host != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = host != null ? host.hashCode() : 0;
    result = 31 * result + port;
    return result;
  }

}
