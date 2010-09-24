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

    private HashAlgorithm lookupHashAlgorithm(String algorithm) {
        HashAlgorithm ha = HashAlgorithm.NATIVE_HASH;
        if ("crc".equalsIgnoreCase(algorithm)) {
            ha = HashAlgorithm.CRC32_HASH;
        } else if ("fnv1_32".equalsIgnoreCase(algorithm)) {
            ha = HashAlgorithm.FNV1_32_HASH;
        } else if ("fnv1_64".equalsIgnoreCase(algorithm)) {
            ha = HashAlgorithm.FNV1_64_HASH;
        } else if ("fnv1a_32".equalsIgnoreCase(algorithm)) {
            ha = HashAlgorithm.FNV1A_32_HASH;
        } else if ("fnv1a_64".equalsIgnoreCase(algorithm)) {
            ha = HashAlgorithm.FNV1A_64_HASH;
        } else if ("md5".equalsIgnoreCase(algorithm)) {
            ha = HashAlgorithm.KETAMA_HASH;
        } else {
            throw new IllegalArgumentException("Unhandled algorithm type: "
                    + algorithm);
        }
        return ha;
    }

    private Config parseJSON(JSONObject jsonObject) throws JSONException {
        // Allows clients to have a JSON envelope.
        if (jsonObject.has("vBucketServerMap")) {
            return parseJSON(jsonObject.getJSONObject("vBucketServerMap"));
        }
        HashAlgorithm hashAlgorithm = lookupHashAlgorithm(jsonObject
                .getString("hashAlgorithm"));
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
        Config config = new DefaultConfig(hashAlgorithm, serversCount,
                replicasCount, vbucketsCount, populateServers,
                populateVbuckets);

        return config;
    }

    private List<String> populateServers(JSONArray servers)
            throws JSONException {
        List<String> serverNames = new ArrayList<String>();
        for (int i = 0; i < servers.length(); i++) {
            String server = servers.getString(i);
            serverNames.add(server);
        }
        return serverNames;
    }

    private List<VBucket> populateVbuckets(JSONArray jsonVbuckets)
            throws JSONException {
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
