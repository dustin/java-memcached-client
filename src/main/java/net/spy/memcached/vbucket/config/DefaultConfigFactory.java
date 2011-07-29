package net.spy.memcached.vbucket.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.HashAlgorithmRegistry;

public class DefaultConfigFactory implements ConfigFactory {

    @Override
    public Config create(File filename) {
        if (filename == null || "".equals(filename.getName())) {
            throw new IllegalArgumentException("Filename is empty.");
        }
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    fis));
            String str;
            while ((str = reader.readLine()) != null) {
                sb.append(str);
            }
        } catch (IOException e) {
            throw new ConfigParsingException("Exception reading input file: "
                    + filename, e);
        }
        return create(sb.toString());
    }

    @Override
    public Config create(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            return parseJSON(jsonObject);
        } catch (JSONException e) {
            throw new ConfigParsingException("Exception parsing JSON data: "
                    + data, e);
        }
    }

    @Override
    public Config create(JSONObject jsonObject) {
        try {
            return parseJSON(jsonObject);
        } catch (JSONException e) {
            throw new ConfigParsingException("Exception parsing JSON data: "
                    + jsonObject, e);
        }
    }

 
    private Config parseJSON(JSONObject jsonObject) throws JSONException {
	// the incoming config could be cache or EP object types, JSON envelope picked apart
	if (!jsonObject.has("vBucketServerMap" )) {
	    return parseCacheJSON(jsonObject);
	}
	return parseEpJSON(jsonObject.getJSONObject("vBucketServerMap"));
    }

    private Config parseCacheJSON(JSONObject jsonObject) throws JSONException {

	JSONArray nodes = jsonObject.getJSONArray("nodes");
        if (nodes.length() <= 0) {
            throw new ConfigParsingException("Empty nodes list.");
        }
        int serversCount = nodes.length();

	CacheConfig config = new CacheConfig(serversCount);
        populateServers(config, nodes);

	return config;
    }

	/* ep is for ep-engine, a.k.a. membase */
    private Config parseEpJSON(JSONObject jsonObject) throws JSONException {
        String algorithm = jsonObject.getString("hashAlgorithm");
        HashAlgorithm hashAlgorithm = 
          HashAlgorithmRegistry.lookupHashAlgorithm(algorithm);
        if (hashAlgorithm == null){
			throw new IllegalArgumentException(
					"Unhandled algorithm type: " + algorithm);
        }
        int replicasCount = jsonObject.getInt("numReplicas");
        if (replicasCount > VBucket.MAX_REPLICAS) {
            throw new ConfigParsingException("Expected number <= "
                    + VBucket.MAX_REPLICAS + " for replicas.");
        }
        JSONArray servers = jsonObject.getJSONArray("serverList");
        if (servers.length() <= 0) {
            throw new ConfigParsingException("Empty servers list.");
        }
        int serversCount = servers.length();
        JSONArray vbuckets = jsonObject.getJSONArray("vBucketMap");
        int vbucketsCount = vbuckets.length();
        if (vbucketsCount == 0 || (vbucketsCount & (vbucketsCount - 1)) != 0) {
            throw new ConfigParsingException(
                    "Number of buckets must be a power of two, > 0 and <= "
                            + VBucket.MAX_BUCKETS);
        }
	List<String> populateServers = populateServers(servers);
	List<VBucket> populateVbuckets = populateVbuckets(vbuckets);

        DefaultConfig config = new DefaultConfig(hashAlgorithm, serversCount, replicasCount, vbucketsCount, populateServers, populateVbuckets);

        return config;
    }

    private List<String> populateServers(JSONArray servers) throws JSONException {
        List<String> serverNames = new ArrayList<String>();
        for (int i = 0; i < servers.length(); i++) {
            String server = servers.getString(i);
            serverNames.add(server);
        }
        return serverNames;
    }


    private void populateServers(CacheConfig config, JSONArray nodes) throws JSONException {
	List<String> serverNames = new ArrayList<String>();
	for (int i = 0; i < nodes.length(); i++) {
	    JSONObject node = nodes.getJSONObject(i);
	    String webHostPort = node.getString("hostname");
	    String[] splitHostPort = webHostPort.split(":");
	    JSONObject portsList = node.getJSONObject("ports");
	    int port = portsList.getInt("direct");
	    serverNames.add(splitHostPort[0] + ":" + port);
	}
	config.setServers(serverNames);
    }

    private List<VBucket> populateVbuckets(JSONArray jsonVbuckets) throws JSONException {
        List<VBucket> vBuckets = new ArrayList<VBucket>();
        for (int i = 0; i < jsonVbuckets.length(); i++) {
            JSONArray rows = jsonVbuckets.getJSONArray(i);
            int master = rows.getInt(0);
            int replicas[] = new int[VBucket.MAX_REPLICAS];
            for (int j = 1; j < rows.length(); j++) {
                replicas[j-1] = rows.getInt(j);
            }
            vBuckets.add(new VBucket(master, replicas));
        }
        return vBuckets;
    }

}
