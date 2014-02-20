/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2012 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.protocol.ascii;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import net.spy.memcached.ops.BaseOperationFactory;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.GetAndTouchOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetlOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.MultiGetOperationCallback;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.ObserveOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.ReplicaGetOperation;
import net.spy.memcached.ops.ReplicaGetsOperation;
import net.spy.memcached.ops.SASLAuthOperation;
import net.spy.memcached.ops.SASLMechsOperation;
import net.spy.memcached.ops.SASLStepOperation;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StatsOperation.Callback;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.ops.UnlockOperation;
import net.spy.memcached.ops.VersionOperation;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapOpcode;

/**
 * Operation factory for the ascii protocol.
 */
public class AsciiOperationFactory extends BaseOperationFactory {

  public DeleteOperation delete(String key, DeleteOperation.Callback cb) {
    return new DeleteOperationImpl(key, cb);
  }

  public DeleteOperation delete(String key, long cas,
    DeleteOperation.Callback cb) {
    throw new UnsupportedOperationException("Delete with CAS is not supported "
        + "for ASCII protocol");
  }

  public FlushOperation flush(int delay, OperationCallback cb) {
    return new FlushOperationImpl(delay, cb);
  }

  public GetAndTouchOperation getAndTouch(String key, int expiration,
      GetAndTouchOperation.Callback cb) {
    throw new UnsupportedOperationException("Get and touch is not supported "
        + "for ASCII protocol");
  }

  public GetOperation get(String key, GetOperation.Callback cb) {
    return new GetOperationImpl(key, cb);
  }

  public GetOperation get(Collection<String> keys, GetOperation.Callback cb) {
    return new GetOperationImpl(keys, cb);
  }

  public GetlOperation getl(String key, int exp, GetlOperation.Callback cb) {
    return new GetlOperationImpl(key, exp, cb);
  }

  public ObserveOperation observe(String key, long casId, int index,
      ObserveOperation.Callback cb) {
    throw new UnsupportedOperationException("Observe is not supported "
        + "for ASCII protocol");
  }

  public UnlockOperation unlock(String key, long casId,
          OperationCallback cb) {
    return new UnlockOperationImpl(key, casId, cb);
  }

  public GetsOperation gets(String key, GetsOperation.Callback cb) {
    return new GetsOperationImpl(key, cb);
  }

  public StatsOperation keyStats(String key, Callback cb) {
    throw new UnsupportedOperationException("Key stats are not supported "
        + "for ASCII protocol");
  }

  public MutatorOperation mutate(Mutator m, String key, long by, long exp,
      int def, OperationCallback cb) {
    return new MutatorOperationImpl(m, key, by, cb);
  }

  public StatsOperation stats(String arg, StatsOperation.Callback cb) {
    return new StatsOperationImpl(arg, cb);
  }

  public StoreOperation store(StoreType storeType, String key, int flags,
      int exp, byte[] data, StoreOperation.Callback cb) {
    return new StoreOperationImpl(storeType, key, flags, exp, data, cb);
  }

  public KeyedOperation touch(String key, int expiration,
      OperationCallback cb) {
    return new TouchOperationImpl(key, expiration, cb);
  }

  public VersionOperation version(OperationCallback cb) {
    return new VersionOperationImpl(cb);
  }

  public NoopOperation noop(OperationCallback cb) {
    return new VersionOperationImpl(cb);
  }

  public CASOperation cas(StoreType type, String key, long casId, int flags,
      int exp, byte[] data, StoreOperation.Callback cb) {
    return new CASOperationImpl(key, casId, flags, exp, data, cb);
  }

  public ConcatenationOperation cat(ConcatenationType catType, long casId,
      String key, byte[] data, OperationCallback cb) {
    return new ConcatenationOperationImpl(catType, key, data, cb);
  }

  @Override
  protected Collection<? extends Operation> cloneGet(KeyedOperation op) {
    Collection<Operation> rv = new ArrayList<Operation>();
    GetOperation.Callback callback =
        new MultiGetOperationCallback(op.getCallback(), op.getKeys().size());
    for (String k : op.getKeys()) {
      rv.add(get(k, callback));
    }
    return rv;
  }

  public SASLMechsOperation saslMechs(OperationCallback cb) {
    throw new UnsupportedOperationException("SASL is not supported for "
        + "ASCII protocol");
  }

  public SASLStepOperation saslStep(String[] mech, byte[] challenge,
      String serverName, Map<String, ?> props, CallbackHandler cbh,
      OperationCallback cb) {
    throw new UnsupportedOperationException("SASL is not supported for "
        + "ASCII protocol");
  }

  public SASLAuthOperation saslAuth(String[] mech, String serverName,
      Map<String, ?> props, CallbackHandler cbh, OperationCallback cb) {
    throw new UnsupportedOperationException("SASL is not supported for "
        + "ASCII protocol");
  }

  @Override
  public TapOperation tapBackfill(String id, long date, OperationCallback cb) {
    throw new UnsupportedOperationException("Tap is not supported for ASCII"
        + " protocol");
  }

  @Override
  public TapOperation tapCustom(String id, RequestMessage message,
      OperationCallback cb) {
    throw new UnsupportedOperationException("Tap is not supported for ASCII"
        + " protocol");
  }

  @Override
  public TapOperation tapAck(TapOpcode opcode, int opaque,
      OperationCallback cb) {
    throw new UnsupportedOperationException("Tap is not supported for ASCII"
        + " protocol");
  }

  @Override
  public TapOperation tapDump(String id, OperationCallback cb) {
    throw new UnsupportedOperationException("Tap is not supported for ASCII"
        + " protocol");
  }

  @Override
  public ReplicaGetOperation replicaGet(String key, int index,
  ReplicaGetOperation.Callback callback) {
    throw new UnsupportedOperationException("Replica get is not supported "
        + "for ASCII protocol");
  }

  @Override
  public ReplicaGetsOperation replicaGets(String key, int index,
    ReplicaGetsOperation.Callback callback) {
    throw new UnsupportedOperationException("Replica gets is not supported "
      + "for ASCII protocol");
  }
}
