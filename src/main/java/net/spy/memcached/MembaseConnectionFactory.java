package net.spy.memcached;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.vbucket.ConfigurationException;
import net.spy.memcached.vbucket.ConfigurationProvider;
import net.spy.memcached.vbucket.ConfigurationProviderHTTP;
import net.spy.memcached.vbucket.VBucketNodeLocator;
import net.spy.memcached.vbucket.config.Bucket;
import net.spy.memcached.vbucket.config.Config;
import net.spy.memcached.vbucket.config.ConfigType;

/**
 * Membase implementation of ConnectionFactory.
 *
 * <p>
 * This implementation creates connections where the operation queue is an
 * ArrayBlockingQueue and the read and write queues are unbounded
 * LinkedBlockingQueues. The <code>Retry</code> FailureMode and <code>
 * KetamaHash</code> VBucket hashing mechanism are always used. If other
 * configurations are needed, look at the ConnectionFactoryBuilder.
 *
 * </p>
 */
public class MembaseConnectionFactory extends BinaryConnectionFactory {

	/**
	 * Default failure mode.
	 */
	public static final FailureMode DEFAULT_FAILURE_MODE =
		FailureMode.Retry;

	/**
	 * Default hash algorithm.
	 */
	public static final HashAlgorithm DEFAULT_HASH = HashAlgorithm.KETAMA_HASH;

	/**
	 * Maximum length of the operation queue returned by this connection
	 * factory.
	 */
	public static final int DEFAULT_OP_QUEUE_LEN=16384;

	private final Locator locator;
	private final AuthDescriptor ad;
	private final ConfigurationProvider configurationProvider;
	private final Config vbConfig;

	public MembaseConnectionFactory(final List<URI> baseList,
			final String bucketName, final String usr, final String pwd) throws IOException {
		//ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder(cf);
		for (URI bu : baseList) {
			if (!bu.isAbsolute()) {
				throw new IllegalArgumentException("The base URI must be absolute");
			}
		}
		this.configurationProvider = new ConfigurationProviderHTTP(baseList, usr, pwd);
		Bucket bucket = this.configurationProvider.getBucketConfiguration(bucketName);
		Config config = bucket.getConfig();

		if (config.getConfigType() == ConfigType.MEMBASE) {
			locator = Locator.VBUCKET;
			vbConfig = bucket.getConfig();
		} else if (config.getConfigType() == ConfigType.MEMCACHE) {
			locator = Locator.CONSISTENT;
			vbConfig = null;
		} else {
			locator = null;
			vbConfig = null;
			throw new ConfigurationException("Bucket type not supported or JSON response unexpected");
		}

		if (!this.configurationProvider.getAnonymousAuthBucket().equals(bucketName) && usr != null) {
			ad = new AuthDescriptor(new String[]{"PLAIN"},new PlainCallbackHandler(usr, pwd));
		} else {
			ad = null;
		}
	}

	@Override
	public NodeLocator createLocator(List<MemcachedNode> nodes) {
		switch(locator) {
			case CONSISTENT:
				return new KetamaNodeLocator(nodes, getHashAlg());
            case VBUCKET:
                return new VBucketNodeLocator(nodes, getVBucketConfig());
			default: throw new IllegalStateException(
					"Unhandled locator type: " + locator);
		}
	}

	public AuthDescriptor getAuthDescriptor() {
		return ad;
	}

	public Config getVBucketConfig() {
		return vbConfig;
	}

	public ConfigurationProvider getConfigurationProvider() {
		return configurationProvider;
	}

	public Locator getLocator() {
		return locator;
	}
}
