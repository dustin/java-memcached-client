package net.spy.memcached.protocol.binary;

import java.util.Collection;

import net.spy.memcached.OperationFactory;
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
import net.spy.memcached.ops.GetOperation.Callback;

/**
 * Factory for binary operations.
 */
public class BinaryOperationFactory implements OperationFactory {

	public DeleteOperation delete(String key, int when,
			OperationCallback operationCallback) {
		return new DeleteOperationImpl(key, when, operationCallback);
	}

	public FlushOperation flush(int delay, OperationCallback cb) {
		return new FlushOperationImpl(cb);
	}

	public GetOperation get(String key, Callback callback) {
		return new GetOperationImpl(key, callback);
	}

	public GetOperation get(Collection<String> value, Callback cb) {
		return new MultiGetOperationImpl(value, cb);
	}

	public GetsOperation gets(String key, GetsOperation.Callback cb) {
		return new GetOperationImpl(key, cb);
	}

	public MutatatorOperation mutate(Mutator m, String key, int by,
			long def, int exp, OperationCallback cb) {
		return new MutatorOperationImpl(m, key, by, def, exp, cb);
	}

	public StatsOperation stats(String arg,
			net.spy.memcached.ops.StatsOperation.Callback cb) {
		throw new UnsupportedOperationException();
	}

	public StoreOperation store(StoreType storeType, String key, int flags,
			int exp, byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(storeType, key, flags, exp, data, 0, cb);
	}

	public VersionOperation version(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public NoopOperation noop(OperationCallback cb) {
		return new NoopOperationImpl(cb);
	}

	public CASOperation cas(String key, long casId, int flags,
			byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(StoreType.set, key, flags, 0, data,
				casId, cb);
	}

	public ConcatenationOperation cat(ConcatenationType catType, long casId,
			String key, byte[] data, OperationCallback cb) {
		return new ConcatenationOperationImpl(catType, key, data, casId, cb);
	}

}
