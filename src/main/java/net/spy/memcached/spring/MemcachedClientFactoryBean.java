package net.spy.memcached.spring;

import java.util.Collection;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.ops.OperationQueueFactory;
import net.spy.memcached.transcoders.Transcoder;

import org.springframework.beans.factory.FactoryBean;

/**
 * A Spring {@link FactoryBean} creating {@link MemcachedClient} instances.
 * <p>
 * Usage example:
 * <pre>
 * {@code
 * <bean id="memcachedClient" class="net.spy.memcached.utils.MemcachedClientFactoryBean">
 *   <property name="servers" value="${pajamas.remoteHosts}"/>
 *   <property name="protocol" value="${pajamas.client.protocol}"/>
 *   <property name="transcoder">
 *     <bean class="net.rubyeye.xmemcached.transcoders.SerializingTranscoder"/>
 *   </property>
 *   <property name="hashAlg" value="${pajamas.client.hashAlg}"/>
 *   <property name="locatorType" value="${pajamas.client.locatorType}"/>
 * </bean>
 * }
 * </pre>
 * </p>
 * @author Eran Harel
 */
public class MemcachedClientFactoryBean implements FactoryBean {
  private final ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder();
  private String servers;

  @Override
  public Object getObject() throws Exception {
    return new MemcachedClient(connectionFactoryBuilder.build(), AddrUtil.getAddresses(servers));
  }

  @Override
  public Class getObjectType() {
    return MemcachedClient.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setServers(final String servers) {
    this.servers = servers;
  }

  public void setAuthDescriptor(final AuthDescriptor to) {
    connectionFactoryBuilder.setAuthDescriptor(to);
  }

  public void setDaemon(final boolean d) {
    connectionFactoryBuilder.setDaemon(d);
  }

  public void setFailureMode(final FailureMode fm) {
    connectionFactoryBuilder.setFailureMode(fm);
  }

  public void setHashAlg(final HashAlgorithm to) {
    connectionFactoryBuilder.setHashAlg(to);
  }

  public void setInitialObservers(final Collection<ConnectionObserver> obs) {
    connectionFactoryBuilder.setInitialObservers(obs);
  }

  public void setLocatorType(final Locator l) {
    connectionFactoryBuilder.setLocatorType(l);
  }

  public void setMaxReconnectDelay(final long to) {
    connectionFactoryBuilder.setMaxReconnectDelay(to);
  }

  public void setOpFact(final OperationFactory f) {
    connectionFactoryBuilder.setOpFact(f);
  }

  public void setOpQueueFactory(final OperationQueueFactory q) {
    connectionFactoryBuilder.setOpQueueFactory(q);
  }

  public void setOpQueueMaxBlockTime(final long t) {
    connectionFactoryBuilder.setOpQueueMaxBlockTime(t);
  }

  public void setOpTimeout(final long t) {
    connectionFactoryBuilder.setOpTimeout(t);
  }

  public void setProtocol(final Protocol prot) {
    connectionFactoryBuilder.setProtocol(prot);
  }

  public void setReadBufferSize(final int to) {
    connectionFactoryBuilder.setReadBufferSize(to);
  }

  public void setReadOpQueueFactory(final OperationQueueFactory q) {
    connectionFactoryBuilder.setReadOpQueueFactory(q);
  }

  public void setShouldOptimize(final boolean o) {
    connectionFactoryBuilder.setShouldOptimize(o);
  }

  public void setTimeoutExceptionThreshold(final int to) {
    connectionFactoryBuilder.setTimeoutExceptionThreshold(to);
  }

  public void setTranscoder(final Transcoder<Object> t) {
    connectionFactoryBuilder.setTranscoder(t);
  }

  public void setUseNagleAlgorithm(final boolean to) {
    connectionFactoryBuilder.setUseNagleAlgorithm(to);
  }

  public void setWriteOpQueueFactory(final OperationQueueFactory q) {
    connectionFactoryBuilder.setWriteOpQueueFactory(q);
  }

}
