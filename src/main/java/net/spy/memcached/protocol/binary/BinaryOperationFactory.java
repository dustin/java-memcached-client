package net.spy.memcached.protocol.binary;

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
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.MultiGetOperationCallback;
import net.spy.memcached.ops.MultiGetsOperationCallback;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.SASLAuthOperation;
import net.spy.memcached.ops.SASLMechsOperation;
import net.spy.memcached.ops.SASLStepOperation;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.VersionOperation;
import net.spy.memcached.ops.GetOperation.Callback;

/**
 * Factory for binary operations.
 */
public class BinaryOperationFactory extends BaseOperationFactory {

	public DeleteOperation delete(String key,
		OperationCallback operationCallback) {
		return new DeleteOperationImpl(key, operationCallback);
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

	public MutatorOperation mutate(Mutator m, String key, int by,
			long def, int exp, OperationCallback cb) {
		return new MutatorOperationImpl(m, key, by, def, exp, cb);
	}

	public StatsOperation stats(String arg,
			net.spy.memcached.ops.StatsOperation.Callback cb) {
		return new StatsOperationImpl(arg, cb);
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

	public CASOperation cas(StoreType type, String key, long casId, int flags,
			int exp, byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(type, key, flags, exp, data,
				casId, cb);
	}

	public ConcatenationOperation cat(ConcatenationType catType, long casId,
			String key, byte[] data, OperationCallback cb) {
		return new ConcatenationOperationImpl(catType, key, data, casId, cb);
	}

	@Override
	protected Collection<? extends Operation> cloneGet(KeyedOperation op) {
		Collection<Operation> rv=new ArrayList<Operation>();
		GetOperation.Callback getCb = null;
		GetsOperation.Callback getsCb = null;
		if(op.getCallback() instanceof GetOperation.Callback) {
			getCb=new MultiGetOperationCallback(
					op.getCallback(), op.getKeys().size());
		} else {
			getsCb=new MultiGetsOperationCallback(
					op.getCallback(), op.getKeys().size());
		}
		for(String k : op.getKeys()) {
			rv.add(getCb == null ? gets(k, getsCb) : get(k, getCb));
		}
		return rv;
	}

	public SASLAuthOperation saslAuth(String[] mech, String serverName,
			Map<String, ?> props, CallbackHandler cbh, OperationCallback cb) {
		return new SASLAuthOperationImpl(mech, serverName, props, cbh, cb);
	}

	public SASLMechsOperation saslMechs(OperationCallback cb) {
		return new SASLMechsOperationImpl(cb);
	}

	public SASLStepOperation saslStep(String[] mech, byte[] challenge,
			String serverName, Map<String, ?> props, CallbackHandler cbh,
			OperationCallback cb) {
		return new SASLStepOperationImpl(mech, challenge, serverName,
				props, cbh, cb);
	}

}
