package esac.archive.gacs.sl.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;
import esac.archive.gacs.sl.test.data.ReadData;
import esac.archive.gacs.sl.test.http.DummyHttpRequest;
import esac.archive.gacs.sl.test.http.DummyHttpResponse;
import esac.archive.gacs.sl.test.http.DummyServletInputStream;
import esac.archive.gacs.sl.test.tap.DummyTapDatabaseConnection;
import esavo.uws.jobs.parameters.UwsJobOwnerParameters;
import esavo.uws.test.DatabaseUtils;
import esavo.uws.test.database.DummyData;
import esavo.uws.test.database.DummyDatabaseConnection;

/**
 * In order to create requests, you have to specify 'subcontext' and 'servletName'<br/>
 * 'subcontext' is added to the base url. e.g.:
 * <pre><tt>
 * String base = "http://localhost:8080/tap-test"
 * 
 * //We want to construct the following URL:
 * //     http://localhost:8080/tap-test/path/to/servlet/servletName/info/other?param=value 
 * 
 * String subcontext = "path/to/servlet/servletName/info/other";
 * String servletName = "servletName";
 * Map<String,String> params = new HashMap<String,String>();
 * params.put("param","value");
 * 
 * DummyHttpRequest request = TestUtils.createSimpleHttpGetRequest(subcontext, servletName, params);
 * 
 * //Another url: http://localhost:8080/tap-test/Upload?param=value
 * String subcontext = "Upload";
 * String servletName = "Upload"
 * Map<String,String> params = new HashMap<String,String>();
 * params.put("param","value");
 * 
 * DummyHttpRequest request = TestUtils.createSimpleHttpGetRequest(subcontext, servletName, params);
 * 
 * 
 * </tt></pre>
 */
public class TestUtils {
	
	public static final String DATA_DIR = "/esac/archive/gacs/sl/test/data/";
	public static final String STATGRAPHS_DIR = DATA_DIR+"statgraphs/";
	public static final String HTTP_TAP_REQUEST_HOST_PORT = "localhost:8080";
	public static final String HTTP_TAP_REQUEST_BASE = "http://"+HTTP_TAP_REQUEST_HOST_PORT+"/tap-test/";
	
	public static void clearDirectory(File f) {
		if (!f.exists()) {
			return;
		}
		if (f.isDirectory()) {
			File fChild[] = f.listFiles();
			if (fChild != null) {
				for (int i = 0; i < fChild.length; i++) {
					clearDirectory(fChild[i]);
				}
			}
		}

		f.delete();
	}

	public static Map<String,String> createStandardHttpResponseHeaders(){
		return createStandardHttpResponseHeaders("200", "OK", "text/html; charset=UTF-8");
	}
	
	public static Map<String,String> createStandardHttpResponseHeaders(String code, String status, String contentType){
		Map<String,String> headers = new HashMap<String, String>();
		headers.put("HTTP/1.x",code + " " + status);
		headers.put("Content-Type", contentType);
		return headers;
	}
	
	public static DummyHttpResponse createSimpleHttpResponse(){
		DummyHttpResponse response = new DummyHttpResponse();
		response.setHeaders(createStandardHttpResponseHeaders());
		return response;
	}
	
	public static Map<String,String> createStandardHttpRequestHeaders(){
		Map<String,String> headers = new HashMap<String, String>();
		//headers.put("Content-type", "")
		headers.put(DummyHttpResponse.HTTP_HEADER_USER_AGENT, "test-harness framework");
		headers.put(DummyHttpResponse.HTTP_HEADER_HOST, HTTP_TAP_REQUEST_HOST_PORT);
		headers.put(DummyHttpResponse.HTTP_HEADER_ACCEPT, "*/*");
		return headers;
	}
	
	public static DummyHttpRequest createMultipartUploadHttpRequest(Class<?> loader, String resource) throws IOException{
		Map<String,String> headers = createStandardHttpRequestHeaders();
		headers.put("MIME-version", "1.0");
		
		//boundary
		String boundary = ReadData.readFirstLine(loader, resource);
		String realBoundary = boundary.substring(2);
		headers.put(DummyHttpRequest.HTTP_HEADER_CONTENT_TYPE, "multipart/form-data; boundary="+realBoundary);
		
		//resource size
		long contentLength = ReadData.getContentLength(loader, resource);
		headers.put(DummyHttpRequest.HTTP_HEADER_CONTENT_LENGTH, "" + contentLength);
		
		//Create request
		DummyHttpRequest request = createHttpRequest("Upload", "Upload", headers, "POST");
		request.setCharacterEncoding("UTF-8");
		
		//Set input stream
		//InputStream is = TestUtils.class.getClass().getResourceAsStream(resource);
		InputStream is = ReadData.findResource(loader, resource);
		DummyServletInputStream sii = new DummyServletInputStream(is);
		request.setInputStream(sii);
		
		return request;
	}
	
	public static DummyHttpRequest createMultipartUploadHttpRequest(Class<?> loader, String resource, String parameterToRemove) throws IOException{
		String file = ReadData.readFileAndRemoveParameter(loader, resource, parameterToRemove);
		
		Map<String,String> headers = createStandardHttpRequestHeaders();
		headers.put("MIME-version", "1.0");
		
		//boundary
		String boundary = ReadData.readFirstLine(loader, resource);
		String realBoundary = boundary.substring(2);
		headers.put(DummyHttpRequest.HTTP_HEADER_CONTENT_TYPE, "multipart/form-data; boundary="+realBoundary);
		
		//resource size
		long contentLength = file.length();
		headers.put(DummyHttpRequest.HTTP_HEADER_CONTENT_LENGTH, "" + contentLength);
		
		//Create request
		DummyHttpRequest request = createHttpRequest("Upload", "Upload", headers, "POST");
		request.setCharacterEncoding("UTF-8");
		
		//Set input stream
		ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());
		DummyServletInputStream sii = new DummyServletInputStream(bais);
		request.setInputStream(sii);
		
		return request;
	}
	
	public static DummyHttpRequest createSimpleHttpPostRequest(String subcontext, String servletName){
		return createSimpleHttpPostRequest(subcontext, servletName, null);
	}
	
	/**
	 * Creates a Get http request composed of: {@link #HTTP_TAP_REQUEST_BASE} + subcontext <br/>
	 * @param subcontext
	 * @param reqParams
	 * @return
	 */
	public static DummyHttpRequest createSimpleHttpPostRequest(String subcontext, String servletName, Map<String,String> reqParams){
		Map<String,String> headers = createStandardHttpRequestHeaders();
		
		DummyHttpRequest request = createHttpRequest(subcontext, servletName, headers, "POST");
		
		if(reqParams != null && reqParams.size() > 0){
			StringBuilder sb = new StringBuilder();
			boolean firstTime = true;
			for(Entry<String, String> e: reqParams.entrySet()){
				if(firstTime){
					firstTime = false;
				}else{
					sb.append("&");
				}
				sb.append(e.getKey()).append('=').append(e.getValue());
				request.setParameter(e.getKey(), e.getValue());
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
			DummyServletInputStream sii = new DummyServletInputStream(bais);
			request.setInputStream(sii);
		}

		return request;
	}
	


	public static DummyHttpRequest createSimpleHttpUploadGetRequest(){
		return createSimpleHttpUploadGetRequest(null);
	}
	
	/**
	 * Creates an 'StatGraphs' request.
	 * @param reqParams
	 * @return
	 */
	public static DummyHttpRequest createSimpleHttpStatGraphsGetRequest(Map<String,String> reqParams){
		return createSimpleHttpGetRequest("StatGraph", "StatGraph", reqParams);
	}
	/**
	 * Creates an 'Upload' request.
	 * @param reqParams
	 * @return
	 */
	public static DummyHttpRequest createSimpleHttpUploadGetRequest(Map<String,String> reqParams){
		return createSimpleHttpGetRequest("Upload", "Upload", reqParams);
	}
	
	public static DummyHttpRequest createSimpleHttpGetRequest(String subcontext, String servletName){
		return createSimpleHttpGetRequest(subcontext, servletName, null);
	}
	
	/**
	 * Creates a Get http request composed of: {@link #HTTP_TAP_REQUEST_BASE} + subcontext <br/>
	 * @param subcontext
	 * @param reqParams
	 * @return
	 */
	public static DummyHttpRequest createSimpleHttpGetRequest(String subcontext, String servletName, Map<String,String> reqParams){
		Map<String,String> headers = createStandardHttpRequestHeaders();
		if(reqParams != null && reqParams.size() > 0){
			StringBuilder sb = new StringBuilder();
			boolean firstTime = true;
			for(Entry<String, String> e: reqParams.entrySet()){
				if(firstTime){
					firstTime = false;
				}else{
					sb.append("&");
				}
				sb.append(e.getKey()).append('=').append(e.getValue());
			}
			subcontext += "?" + sb.toString();
		}
		
		DummyHttpRequest request = createHttpRequest(subcontext, servletName, headers, "GET");
		
		return request;
	}
	
//	public static DummyHttpRequest createStandardTapHttpRequest(String tapMode, String queryString, String method){
//		String subcontext = tapMode + "?" + queryString;
//		Map<String,String> headers = createStandardHttpRequestHeaders();
//		return createHttpRequest(subcontext, subcontext, headers, method);
//	}
	
	/**
	 * Returns an HTTP request composed of: {@link #HTTP_TAP_REQUEST_BASE} + subcontext <br/>
	 * Using the provided headers and method
	 * @param subcontext
	 * @param headers
	 * @param method
	 * @return
	 */
	public static DummyHttpRequest createHttpRequest(String subcontext, String servletName, Map<String,String> headers, String method){
		String url = HTTP_TAP_REQUEST_BASE + subcontext;
		DummyHttpRequest request = new DummyHttpRequest(url, servletName, headers);
		request.setMethod(method);
		return request;
	}
	
	public static boolean findErrorInHtml(String html) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(html.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		try{
			while((line = br.readLine()) != null){
				if(line.indexOf("<title>SERVICE ERROR</title>") >= 0){
					return true;
				}
			}
		} finally{
			br.close();
			is.close();
		}
		return false;
	}
	
	public static boolean findErrorInJSon(String json) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		try{
			while((line = br.readLine()) != null){
				if(line.indexOf("\"failed\":") >= 0){
					return true;
				}
				if(line.indexOf("'failed':") >= 0){
					return true;
				}
			}
		} finally{
			br.close();
			is.close();
		}
		return false;
	}
	

	public static boolean findValueInJSon(String json, String value) throws IOException {
		String realValue = value;
		if(value == null){
			realValue = "";
		}
		ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		try{
			while((line = br.readLine()) != null){
				if(line.indexOf(",\"value\":\""+realValue+"\"") >= 0){
					return true;
				}
			}
		} finally{
			br.close();
			is.close();
		}
		return false;
	}
	
	public static void removeDirectory(File f){
		if(f == null){
			return;
		}
		if(f.isDirectory()){
			File[] fContent = f.listFiles();
			if(fContent != null){
				for(File fTmp: fContent){
					removeDirectory(fTmp);
				}
			}
		}
		f.delete();
	}
	

	
	public static void checkDbAction(DummyTapDatabaseConnection db, String action, String msg){
		if(!db.isActionLogged(action, msg)){
			String similarActions = db.findSimilarAction(action);
			if(similarActions == null || "".equals(similarActions)){
				Assert.fail("DB action expected: '" + action + ": "+msg+"'" + "\nRegisterd actions:\n" + db.getRegisteredActions());
			}else{
				Assert.fail("DB action expected: '" + action + ": "+msg+"'" + "\nSimilar actions:\n" + similarActions);
			}
		}
	}
	
	public static void checkDbActions(DummyTapDatabaseConnection db, String[] actions, String[] msgs){
		for(int i = 0; i < actions.length; i++){
			checkDbAction(db, actions[i], msgs[i]);
		}
	}
	
	public static void checkDbQuery(DummyDatabaseConnection db, String query){
		if(!db.isExecutedQuery(query)){
			Assert.fail("DB query expected: '" + query + "'.\nRegistered queries:\n" + db.getExecutedQueries());
		}
	}
	
	public static void checkDbQueries(DummyDatabaseConnection db, String[] queries){
		for(String q: queries){
			checkDbQuery(db, q);
		}
	}
	
	public static void setUserDataComplete(DummyDatabaseConnection dbConn, String userid, String role){
		String queryUser = "SELECT auth_name, pseudo, roles FROM uws2_schema.owners WHERE owner_id = '"+userid+"'";
		DummyData data = DatabaseUtils.createDummyData(DatabaseUtils.getColumnNamesFromSelectQuery(queryUser), (String[][])null);
		dbConn.setDataForQuery(queryUser, data);

		data = DatabaseUtils.createDummyData(DatabaseUtils.getColumnNamesFromSelectQuery(queryUser), new String[][]{{userid,userid,role}});
		dbConn.setDataForQuery(queryUser, data);
		
		TestUtils.setUserData(dbConn, userid, role);
	}
	
	public static final int DEFAULT_DB_CURRENT_SIZE = 100;
	public static final int DEFAULT_DB_QUOTA = 1000;
	public static final int DEFAULT_FILES_CURRENT_SIZE = 200;
	public static final int DEFAULT_FILES_QUOTA = 2000;
	
	public static void setUserData(DummyDatabaseConnection dbConn, String userid, String role){
		String queryExecuted2 = "SELECT parameter_id, data_type, string_representation FROM uws2_schema.owner_parameters WHERE owner_id = '"+userid+"'";
		DummyData data2 = DatabaseUtils.createDummyData(DatabaseUtils.getColumnNamesFromSelectQuery(queryExecuted2), 
				new String[][]{
			{"db_quota","Long",""+DEFAULT_DB_QUOTA},
			{"db_current_size","Long",""+DEFAULT_DB_CURRENT_SIZE},
			{"files_quota","Long",""+DEFAULT_FILES_QUOTA},
			{"files_current_size","Long",""+DEFAULT_FILES_CURRENT_SIZE}
			});
		dbConn.setDataForQuery(queryExecuted2, data2);
		
		String queryExecuted3 = "SELECT tap_schema.db_user_usage('"+userid+"')";
		DummyData data3 = DatabaseUtils.createDummyData(
				new String[]{"db_user_usage"},
				new String[][]{
			{"105"}
			});
		dbConn.setDataForQuery(queryExecuted3, data3);
		
		dbConn.setDataForQuery(
				"UPDATE uws2_schema.owners SET auth_name = '"+userid+"', pseudo = '"+userid+"', roles = "+role+" WHERE owner_id = '"+userid+"'", 
				DatabaseUtils.createInsertOrUpdateData(1));
		
		dbConn.setDataForQuery(
				"UPDATE uws2_schema.owner_parameters SET data_type = 'Long', string_representation = '"+DEFAULT_DB_CURRENT_SIZE+"' WHERE parameter_id = 'db_current_size' AND owner_id = '"+userid+"'", 
				DatabaseUtils.createInsertOrUpdateData(1));
		
		dbConn.setDataForQuery(
				"UPDATE uws2_schema.owner_parameters SET data_type = 'Long', string_representation = '"+DEFAULT_FILES_QUOTA+"' WHERE parameter_id = 'files_quota' AND owner_id = '"+userid+"'",
				DatabaseUtils.createInsertOrUpdateData(1));
		
		dbConn.setDataForQuery(
				"UPDATE uws2_schema.owner_parameters SET data_type = 'Long', string_representation = '"+DEFAULT_DB_QUOTA+"' WHERE parameter_id = 'db_quota' AND owner_id = '"+userid+"'",
				DatabaseUtils.createInsertOrUpdateData(1));

		dbConn.setDataForQuery(
				"UPDATE uws2_schema.owner_parameters SET data_type = 'Long', string_representation = '"+DEFAULT_FILES_CURRENT_SIZE+"' WHERE parameter_id = 'files_current_size' AND owner_id = '"+userid+"'",
				DatabaseUtils.createInsertOrUpdateData(1));

	}
	
	public static UwsJobOwnerParameters createDefaultUserParameters(){
		UwsJobOwnerParameters parameters = new UwsJobOwnerParameters();
		try{
			parameters.setParameter("db_quota", new Long(DEFAULT_DB_QUOTA));
			parameters.setParameter("db_current_size", new Long(DEFAULT_DB_CURRENT_SIZE));
			parameters.setParameter("files_quota", new Long(DEFAULT_FILES_QUOTA));
			parameters.setParameter("files_current_size", new Long(DEFAULT_FILES_CURRENT_SIZE));
		}catch(Exception e){
			
		}
		return parameters;
	}
	
	
	public static void copyFile(File src, File dst) throws IOException {
		FileInputStream fis = new FileInputStream(src);
		copyFile(fis, dst);
		fis.close();
	}

	public static void copyFile(InputStream src, File dst) throws IOException {
		File dstParent = dst.getParentFile();
		if(!dstParent.exists()){
			dstParent.mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(dst);
		copyFile(src, fos);
		fos.close();
	}

	public static void copyFile(InputStream src, OutputStream dst) throws IOException {
		byte[] buffer = new byte[4096];
		int read;
		while((read = src.read(buffer)) != -1){
			dst.write(buffer, 0, read);
		}
	}


}