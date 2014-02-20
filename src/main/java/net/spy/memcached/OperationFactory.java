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

package net.spy.memcached;

import java.util.Collection;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

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
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.ops.UnlockOperation;
import net.spy.memcached.ops.VersionOperation;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapOpcode;

/**
 * Factory that builds operations for protocol handlers.
 */
public interface OperationFactory {

  /**
   * Create a NOOP operation.
   *
   * @param cb the operation callback
   * @return the new NoopOperation
   */
  NoopOperation noop(OperationCallback cb);

  /**
   * Create a deletion operation.
   *
   * @param key the key to delete
   * @param callback the status callback
   * @return the new DeleteOperation
   */
  DeleteOperation delete(String key, DeleteOperation.Callback callback);

  /**
   * Create a deletion operation with CAS.
   *
   * @param key the key to delete
   * @param cas the CAS value to pass along
   * @param callback the status callback
   * @return the new DeleteOperation
   */
  DeleteOperation delete(String key, long cas,
    DeleteOperation.Callback callback);

  /**
   * Create a Unlock operation.
   *
   * @param key the key to unlock
   * @param casId the value of CAS
   * @param operationCallback the status callback
   * @return the new UnlockOperation
   */
  UnlockOperation unlock(String key, long casId,
          OperationCallback operationCallback);

  /**
   * Create an Observe operation.
   *
   * @param key the key to observe
   * @param casId the value of CAS
   * @param index the VBucket index of key
   * @param operationCallback the status callback
   * @return the new ObserveOperation
   */
  ObserveOperation observe(String key, long casId, int index,
          ObserveOperation.Callback operationCallback);

  /**
   * Create a flush operation.
   *
   * @param delay delay until flush.
   * @param operationCallback the status callback
   * @return the new FlushOperation
   */
  FlushOperation flush(int delay, OperationCallback operationCallback);

  /**
   * Gets the value of a key and resets its timeout.
   *
   * @param key the key to get a value for and reset its timeout
   * @param expiration the new expiration for the key
   * @param cb the callback that will contain the result
   * @return a new GATOperation
   */
  GetAndTouchOperation getAndTouch(String key, int expiration,
      GetAndTouchOperation.Callback cb);

  /**
   * Create a get operation.
   *
   * @param key the key to get
   * @param callback the callback that will contain the results
   * @return a new GetOperation
   */
  GetOperation get(String key, GetOperation.Callback callback);

  /**
   * Create a replica get operation.
   *
   * @param key the key to get
   * @param callback the callback that will contain the results
   * @return a new ReplicaGetOperation
   */
  ReplicaGetOperation replicaGet(String key, int index,
    ReplicaGetOperation.Callback callback);

  /**
   * Create a replica gets operation.
   *
   * @param key the key to get
   * @param callback the callback that will contain the results
   * @return a new ReplicaGetOperation
   */
  ReplicaGetsOperation replicaGets(String key, int index,
    ReplicaGetsOperation.Callback callback);

  /**
   * Create a getl operation. A getl gets the value for a key and then locks the
   * value for a given amount of time. The maximum default lock time is 30
   * seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param callback the callback that will contain the results
   * @return a new GetOperation
   */
  GetlOperation getl(String key, int exp, GetlOperation.Callback callback);

  /**
   * Create a gets operation.
   *
   * @param key the key to get
   * @param callback the callback that will contain the results
   * @return a new GetsOperation
   */
  GetsOperation gets(String key, GetsOperation.Callback callback);

  /**
   * Create a get operation.
   *
   * @param keys the collection of keys to get
   * @param cb the callback that will contain the results
   * @return a new GetOperation
   */
  GetOperation get(Collection<String> keys, GetOperation.Callback cb);

  /**
   * Get a new KeyStatsOperation.
   *
   * @param key the key to get stats for
   * @param cb the stats callback
   * @return the new StatsOperation
   */
  StatsOperation keyStats(String key, StatsOperation.Callback cb);

  /**
   * Create a mutator operation.
   *
   * @param m the mutator type
   * @param key the mutatee key
   * @param by the amount to increment or decrement
   * @param def the default value
   * @param exp expiration in case we need to default (0 if no default)
   * @param cb the status callback
   * @return the new mutator operation
   */
  MutatorOperation mutate(Mutator m, String key, long by, long def, int exp,
      OperationCallback cb);

  /**
   * Get a new StatsOperation.
   *
   * @param arg the stat parameter (see protocol docs)
   * @param cb the stats callback
   * @return the new StatsOperation
   */
  StatsOperation stats(String arg, StatsOperation.Callback cb);

  /**
   * Create a store operation.
   *
   * @param storeType the type of store operation
   * @param key the key to store
   * @param flags the storage flags
   * @param exp the expiration time
   * @param data the data
   * @param cb the status callback
   * @return the new store operation
   */
  StoreOperation store(StoreType storeType, String key, int flags, int exp,
      byte[] data, StoreOperation.Callback cb);

  /**
   * Resets a keys expiration time.
   *
   * @param key The key whose expiration time is to be reset.
   * @param expiration The new expiration time for the key
   * @param cb The status callback
   * @return A touch operation
   */
  KeyedOperation touch(String key, int expiration, OperationCallback cb);

  /**
   * Get a concatenation operation.
   *
   * @param catType the type of concatenation to perform.
   * @param key the key
   * @param casId the CAS value for an atomic compare-and-cat
   * @param data the data to store
   * @param cb a callback for reporting the status
   * @return thew new ConcatenationOperation
   */
  ConcatenationOperation cat(ConcatenationType catType, long casId, String key,
      byte[] data, OperationCallback cb);

  /**
   * Create a CAS operation.
   *
   * @param key the key to store
   * @param casId the CAS identifier value (from a gets operation)
   * @param flags the storage flags
   * @param exp the expiration time
   * @param data the data
   * @param cb the status callback
   * @return the new store operation
   */
  CASOperation cas(StoreType t, String key, long casId, int flags, int exp,
      byte[] data, StoreOperation.Callback cb);

  /**
   * Create a new version operation.
   */
  VersionOperation version(OperationCallback cb);

  /**
   * Create a new SASL mechs operation.
   */
  SASLMechsOperation saslMechs(OperationCallback cb);

  /**
   * Create a new sasl auth operation.
   */
  SASLAuthOperation saslAuth(String[] mech, String serverName,
      Map<String, ?> props, CallbackHandler cbh, OperationCallback cb);

  /**
   * Create a new sasl step operation.
   */
  SASLStepOperation saslStep(String[] mech, byte[] challenge,
      String serverName, Map<String, ?> props, CallbackHandler cbh,
      OperationCallback cb);

  /**
   * Clone an operation.
   *
   * <p>
   * This is used for requeueing operations after a server is found to be down.
   * </p>
   *
   * <p>
   * Note that it returns more than one operation because a multi-get could
   * potentially need to be played against a large number of underlying servers.
   * In this case, there's a separate operation for each, and callback
   * fa\u00E7ade to reassemble them. It is left up to the operation pipeline to
   * perform whatever optimization is required to turn these back into
   * multi-gets.
   * </p>
   *
   * @param op the operation to clone
   * @return a new operation for each key in the original operation
   */
  Collection<Operation> clone(KeyedOperation op);

  /**
   * Creates a tap backfill stream.
   *
   * See <a href="http://www.couchbase.org/wiki/display/membase/TAP+Protocol">
   * http://www.couchbase.org/wiki/display/membase/TAP+Protocol</a> for more
   * details on the tap protocol.
   *
   * TAP connection names are optional, but allow for momentary interruptions in
   * connection to automatically restart. TAP connection names also appear in
   * TAP stats from the given server.
   *
   * Note that according to the protocol, TAP backfill dates are advisory and
   * the protocol guarantees at least data from specified date forward, but
   * earlier mutations may be received.
   *
   * @param id The name for the TAP connection
   * @param date The date to start backfill from.
   * @param cb The status callback.
   * @return The tap operation used to create and handle the stream.
   */
  TapOperation tapBackfill(String id, long date, OperationCallback cb);

  /**
   * Creates a custom tap stream.
   *
   * See <a href="http://www.couchbase.org/wiki/display/membase/TAP+Protocol">
   * http://www.couchbase.org/wiki/display/membase/TAP+Protocol</a> for more
   * details on the tap protocol.
   *
   * TAP connection names are optional, but allow for momentary interruptions in
   * connection to automatically restart. TAP connection names also appear in
   * TAP stats from the given server.
   *
   * @param id The name for the TAP connection
   * @param message The tap message to send.
   * @param cb The status callback.
   * @return The tap operation used to create and handle the stream.
   */
  TapOperation tapCustom(String id, RequestMessage message,
      OperationCallback cb);

  /**
   * Sends a tap ack message to the server.
   *
   * See <a href="http://www.couchbase.org/wiki/display/membase/TAP+Protocol">
   * http://www.couchbase.org/wiki/display/membase/TAP+Protocol</a> for more
   * details on the tap protocol.
   *
   * @param opcode the opcode sent to the client by the server.
   * @param opaque the opaque value sent to the client by the server.
   * @param cb the callback for the tap stream.
   * @return a tap ack operation.
   */
  TapOperation tapAck(TapOpcode opcode, int opaque, OperationCallback cb);

  /**
   * Sends a tap dump message to the server.
   *
   * See <a href="http://www.couchbase.org/wiki/display/membase/TAP+Protocol">
   * http://www.couchbase.org/wiki/display/membase/TAP+Protocol</a> for more
   * details on the tap protocol.
   *
   * @param id the name for the TAP connection
   * @param cb the callback for the tap stream.
   * @return a tap dump operation.
   */
  TapOperation tapDump(String id, OperationCallback cb);
}
