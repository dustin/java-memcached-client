package net.spy.memcached.protocol.ascii;

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

/**
 * Operation factory for the ascii protocol.
 */
public final class AsciiOperationFactory implements OperationFactory {

	public DeleteOperation delete(String key, int when,
			OperationCallback cb) {
		return new DeleteOperationImpl(key, when, cb);
	}

	public FlushOperation flush(int delay, OperationCallback cb) {
		return new FlushOperationImpl(delay, cb);
	}

	public GetOperation get(String key, GetOperation.Callback cb) {
		return new GetOperationImpl(key, cb);
	}

	public GetOperation get(Collection<String> keys, GetOperation.Callback cb) {
		return new GetOperationImpl(keys, cb);
	}

	public GetsOperation gets(String key, GetsOperation.Callback cb) {
		 return new GetsOperationImpl(key, cb);
	}

	public MutatatorOperation mutate(Mutator m, String key, int by,
			long exp, int def, OperationCallback cb) {
		return new MutatorOperationImpl(m, key, by, cb);
	}

	public StatsOperation stats(String arg, StatsOperation.Callback cb) {
		return new StatsOperationImpl(arg, cb);
	}

	public StoreOperation store(StoreType storeType, String key, int flags,
			int exp, byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(storeType, key, flags, exp, data, cb);
	}

	public VersionOperation version(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public NoopOperation noop(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public CASOperation cas(String key, long casId, int flags,
			byte[] data, OperationCallback cb) {
		return new CASOperationImpl(key, casId, flags, data, cb);
	}

	public ConcatenationOperation cat(ConcatenationType catType,
			long casId,
			String key, byte[] data, OperationCallback cb) {
		return new ConcatenationOperationImpl(catType, key, data, cb);
	}

}
