package net.spy.memcached.couch;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.spy.memcached.TestConfig;
import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.internal.ViewFuture;
import net.spy.memcached.protocol.couchdb.RowWithDocs;
import net.spy.memcached.protocol.couchdb.ViewResponseNoDocs;
import net.spy.memcached.protocol.couchdb.Query;
import net.spy.memcached.protocol.couchdb.ViewResponseReduced;
import net.spy.memcached.protocol.couchdb.RowReduced;
import net.spy.memcached.protocol.couchdb.RowNoDocs;
import net.spy.memcached.protocol.couchdb.View;
import net.spy.memcached.protocol.couchdb.ViewResponseWithDocs;

public class CouchbaseClientTest {
	protected TestingClient client = null;
	private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
			+ ":8091/pools";
	private static final Map<String, Object> items;
	public static final String DESIGN_DOC_W_REDUCE = "doc_with_view";
	public static final String DESIGN_DOC_WO_REDUCE = "doc_without_view";
	public static final String VIEW_NAME_W_REDUCE = "view_with_reduce";
	public static final String VIEW_NAME_WO_REDUCE = "view_without_reduce";

	static {
		items = new HashMap<String, Object>();
		int d = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = 0; k < 2; k++, d++) {
					String type = new String(new char[] { 'a' + 26 });
					String small = (new Integer(j)).toString();
					String large = (new Integer(k)).toString();
					String doc = generateDoc(type, small, large);
					items.put("key" + d, doc);
				}
			}
		}
	}

	protected void initClient() throws Exception {
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(SERVER_URI));
		client = new TestingClient(uris, "default", "");
	}

	@BeforeClass
	public static void before() throws Exception {
		// Create some design documents
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(SERVER_URI));
		TestingClient c = new TestingClient(uris, "default", "");
		String docUri = "/default/_design/" + DESIGN_DOC_W_REDUCE;
		String view = "{\"language\":\"javascript\",\"views\":{\""
				+ VIEW_NAME_W_REDUCE + "\":{\"map\":\"function (doc) {  "
				+ "emit(doc._id, 1)}\",\"reduce\":\"_sum\" }}}";
		c.asyncHttpPut(docUri, view);

		docUri = "/default/_design/" + DESIGN_DOC_WO_REDUCE;
		view = "{\"language\":\"javascript\",\"views\":{\""
				+ VIEW_NAME_WO_REDUCE + "\":{\"map\":\"function (doc) {  "
				+ "emit(doc._id, 1)}\"}}}";
		for (Entry<String, Object> item : items.entrySet()) {
			c.set(item.getKey(), 0, (String) item.getValue()).get();
		}
		c.asyncHttpPut(docUri, view);
		c.shutdown();
		Thread.sleep(15000);
	}

	@Before
	public void beforeTest() throws Exception {
		initClient();
	}

	@After
	public void afterTest() throws Exception {
		// Shut down, start up, flush, and shut down again. Error tests have
		// unpredictable timing issues.
		client.shutdown();
		client = null;
		initClient();
		assert client.flush().get();
		client.shutdown();
		client = null;
	}

	@AfterClass
	public static void after() throws Exception {
		// Delete all design documents I created
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(SERVER_URI));
		TestingClient c = new TestingClient(uris, "default", "");
		String json = c.asyncHttpGet("/default/_design/" + DESIGN_DOC_W_REDUCE).get();
		String rev = (new JSONObject(json)).getString("_rev");
		c.asyncHttpDelete("/default/_design/" + DESIGN_DOC_W_REDUCE + "?rev=" + rev).get();

		json = c.asyncHttpGet("/default/_design/" + DESIGN_DOC_WO_REDUCE).get();
		System.out.println(json);
		rev = (new JSONObject(json)).getString("_rev");
		c.asyncHttpDelete("/default/_design/" + DESIGN_DOC_WO_REDUCE + "?rev=" + rev).get();
	}

	private static String generateDoc(String type, String small, String large) {
		return "{\"type\":\"" + type + "\"" + "\"small range\":\"" + small
				+ "\"" + "\"large range\":\"" + large + "\"}";
	}

	@Test
	public void testQueryWithDocs() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.query(view, query);
		ViewResponseWithDocs response = future.get();

		Iterator<RowWithDocs> itr = response.iterator();
		while (itr.hasNext()) {
			RowWithDocs row = itr.next();
			if (items.containsKey(row.getId())) {
				assert items.get(row.getId()).equals(row.getDoc());
			}
		}
		assert items.size() == response.size() :
			future.getStatus().getMessage();
	}

	@Test
	public void testViewNoDocs() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		HttpFuture<ViewResponseNoDocs> future = client.queryAndExcludeDocs(
				view, query);
		ViewResponseNoDocs response = future.get();

		Iterator<RowNoDocs> itr = response.iterator();
		while (itr.hasNext()) {
			RowNoDocs row = itr.next();
			if (!items.containsKey(row.getId())) {
				assert false : ("Got an item that I shouldn't have gotten.");
			}
		}
		assert response.size() == items.size() :
				future.getStatus().getMessage();
	}

	@Test
	public void testReduce() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		HttpFuture<ViewResponseReduced> future = client.queryAndReduce(view,
				query);
		ViewResponseReduced reduce = future.get();

		Iterator<RowReduced> itr = reduce.iterator();
		while (itr.hasNext()) {
			RowReduced row = itr.next();
			assert row.getKey() == null;
			assert Integer.valueOf(row.getValue()) == items.size() :
					future.getStatus().getMessage();
		}
	}

	@Test
	public void testReduceWhenNoneExists() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_WO_REDUCE, VIEW_NAME_WO_REDUCE);
		try {
			client.queryAndReduce(view, query);
		} catch (RuntimeException e) {
			return; // Pass, no reduce exists.
		}
		assert false : ("No view exists and this query still happened");
	}

}
