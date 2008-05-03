package net.spy.memcached;

import java.util.Collection;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.VersionOperation;

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
	 * @param when the locking duration
	 * @param operationCallback the status callback
	 * @return the new DeleteOperation
	 */
	DeleteOperation delete(String key, int when,
			OperationCallback operationCallback);

	/**
	 * Create a flush operation.
	 *
	 * @param delay delay until flush.
	 * @param operationCallback the status callback
	 * @return the new FlushOperation
	 */
	FlushOperation flush(int delay, OperationCallback operationCallback);

	/**
	 * Create a get operation.
	 *
	 * @param key the key to get
	 * @param callback the callback that will contain the results
	 * @return a new GetOperation
	 */
	GetOperation get(String key, GetOperation.Callback callback);

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
	 * @param key the collection of keys to get
	 * @param callback the callback that will contain the results
	 * @return a new GetOperation
	 */
	GetOperation get(Collection<String> value, GetOperation.Callback cb);

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
	MutatatorOperation mutate(Mutator m, String key, int by,
			long def, int exp, OperationCallback cb);

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
			byte[] data, OperationCallback cb);

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
	ConcatenationOperation cat(ConcatenationType catType, long casId,
			String key, byte[] data, OperationCallback cb);

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
	CASOperation cas(String key, long casId, int flags, byte[] data,
			OperationCallback cb);

	/**
	 * Create a new version operation.
	 */
	VersionOperation version(OperationCallback cb);

}
