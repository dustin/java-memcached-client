package net.spy.memcached.protocol.binary;

import java.util.Collection;

import net.spy.memcached.OperationFactory;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.GetOperation;
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

	public MutatatorOperation mutate(Mutator m, String key, int by,
			long def, int exp, OperationCallback cb) {
		// TODO Auto-generated method stub
		return new MutatorOperationImpl(m, key, by, def, exp, cb);

	}

	public StatsOperation stats(String arg,
			net.spy.memcached.ops.StatsOperation.Callback cb) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	public StoreOperation store(StoreType storeType, String key, int flags,
			int exp, byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(storeType, key, flags, exp, data, cb);
	}

	public VersionOperation version(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public NoopOperation noop(OperationCallback cb) {
		return new NoopOperationImpl(cb);
	}

}
