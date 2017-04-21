package com.amexio;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLogger;
import com.documentum.fc.common.IDfLoginInfo;

/**
 * Util Method For "EManager" and "IManager"
 *
 * @author zkaiserm
 * @author afarina
 *
 */
public class EIUtils {
	/**
	 * Read properties and get values for each key
	 *
	 * @param configKey
	 * @param propFileName
	 * @return
	 */
	public static String getConfigVal(final String configKey, final String propFileName) {
		ResourceBundle appConfig = null;
		appConfig = ResourceBundle.getBundle(propFileName);
		return appConfig.getString(configKey).trim();
	}

	/**
	 * Create Session Manager
	 *
	 * @param docbase
	 * @param user
	 * @param pass
	 * @return
	 * @throws DfException
	 */
	public static IDfSessionManager createSessionManager(final String docbase, final String user, final String pass)
			throws DfException {
		debug(null, "Création d'un session manager - docbase : "+docbase+", User : "+user+", pass : ******");
		final IDfClientX clientx = new DfClientX();
		final IDfClient client = clientx.getLocalClient();
		final IDfSessionManager sMgr = client.newSessionManager();
		final IDfLoginInfo loginInfoObj = clientx.getLoginInfo();
		loginInfoObj.setUser(user);
		loginInfoObj.setPassword(pass);
		loginInfoObj.setDomain(null);
		sMgr.setIdentity(docbase, loginInfoObj);
		return sMgr;
	}

	/**
	 * Build DQL String
	 *
	 * @param prop
	 * @param fldrPath
	 * @return
	 */
	public static String retDqlString(final String prop, final String fldrPath) throws Exception {
		final StringBuffer sBuf = new StringBuffer();
		sBuf.append("SELECT ");
		sBuf.append(prop);
		sBuf.append(" FROM ");
		sBuf.append(EIUtils.getConfigVal(EIConstants.ETYPE, EIConstants.PROPFILE));
		sBuf.append(" WHERE");
		sBuf.append(" Folder");
		sBuf.append("('");
		sBuf.append(fldrPath);
		sBuf.append("',DESCEND)");
		return sBuf.toString();
	}

	/**
	 * Create Directory on the file system
	 *
	 * @param root
	 * @param fldrName
	 * @return
	 * @throws Exception
	 */
	public static String makeDir(final String root, final String fldrName) throws Exception {
		String dirPath = null;
		dirPath = root + fldrName;
		if (!new File(dirPath).exists()) {
			final boolean success = (new File(dirPath)).mkdir();
			if (success) {
				return dirPath;
			} else {
				return null;
			}
		} else {
			return dirPath;
		}
	}

	/**
	 * Run DQL and return Collection object
	 *
	 * @param dqlQuery
	 * @param dfSession
	 * @return
	 * @throws DfException
	 */
	public static IDfCollection executeQuery(final String dqlQuery, final IDfSession dfSession) throws DfException {
		IDfCollection coll = null;
		final IDfQuery qry = new DfQuery();
		qry.setDQL(dqlQuery);
		coll = qry.execute(dfSession, IDfQuery.DF_READ_QUERY);
		return coll;
	}

	/**
	 * Generate xml for each exported objects
	 *
	 * @param dirPath
	 * @param attrMap
	 * @param xmlFileName
	 * @return
	 * @throws Exception
	 */
	public static String createObjectPropXml(final IDfSysObject sysObj, final String dirPath,
			final Map<String, String> attrMap, final String xmlFileName) throws Exception {
		FileOutputStream xmlPropFile = null;
		final boolean indent = true;
		xmlPropFile = new FileOutputStream(dirPath + xmlFileName + ".xml");
		byte[] strBytes = null;
		final StringBuffer sBuf = new StringBuffer();
		sBuf.append("<?xml version='1.0' ?>");
		if (indent) {
			sBuf.append("\n\t");
		}
		sBuf.append("<object>");

		for (final Map.Entry<String, String> entry : attrMap.entrySet()) {
			if (indent) {
				sBuf.append("\n\t\t");
			}
			final String key = entry.getKey();
			final String value = entry.getValue();
			sBuf.append("<");
			sBuf.append(key);
			if (!key.equals("filePath")) {
				final int dataT = sysObj.getAttrDataType(key);
				String dataTStr = "";
				switch (dataT) {
					case 1:
						dataTStr = "int";
						break;
					case 2:
						dataTStr = "string";
						break;
					case 3:
						dataTStr = "id";
						break;
					case 4:
						dataTStr = "date";
						break;
					case 5:
						dataTStr = "double";
						break;
					default:
						break;
				}

				final boolean repeat = sysObj.isAttrRepeating(key);
				sBuf.append(" datatype='");
				sBuf.append(dataTStr);
				sBuf.append("'");
				if (repeat) {
					sBuf.append(" repeating='true'");
				}
			}
			sBuf.append(">");
			sBuf.append(value);
			sBuf.append("</");
			sBuf.append(key);
			sBuf.append(">");
		}
		if (indent) {
			sBuf.append("\n\t");
		}
		sBuf.append("</object>");
		strBytes = sBuf.toString().getBytes();
		xmlPropFile.write(strBytes);
		xmlPropFile.close();
		return dirPath + xmlFileName + ".xml";
	}

	/**
	 * Generate Main XML file for each exported folder
	 *
	 * @param fldrName
	 * @param xmlFilePath
	 * @param mainFldrXml
	 * @throws Exception
	 */
	public static void createMainPropXml(final String fldrName, final String xmlFilePath,
			final FileOutputStream mainFldrXml) throws Exception {
		byte[] strBytes = null;
		final StringBuffer sBuf = new StringBuffer();
		sBuf.append("<");
		sBuf.append("object");
		sBuf.append(">");
		sBuf.append(xmlFilePath);
		sBuf.append("</");
		sBuf.append("object");
		sBuf.append(">");
		strBytes = sBuf.toString().getBytes();
		mainFldrXml.write(strBytes);
	}

	/**
	 * Close DCTM Session
	 *
	 * @param sMgr
	 * @param dfSession
	 */
	public static void closeSess(final IDfSessionManager sMgr, final IDfSession dfSession) {
		if (dfSession != null) {
			sMgr.release(dfSession);
		}
	}

	/**
	 * Build Map from "2" arrays
	 *
	 * @param dataStrArr
	 * @param dbdataStrArr
	 * @return
	 */
	public static Map<String, String> buildColMapFromStrArr(final String dataStrArr[], final String dbdataStrArr[]) {
		final Map<String, String> dataMap = new HashMap<String, String>();
		for (int index = 0; index < dataStrArr.length; index++) {
			dataMap.put(dataStrArr[index].trim(), dbdataStrArr[index].trim());
		}
		return dataMap;
	}
	public static void error(Object cllog,String message,Exception t) {
		System.out.println(message);
		if (cllog==null) {
			DfLogger.error(EIUtils.class , message, null, t);
		} else {
			DfLogger.error(cllog.getClass(), message, null, t);
		}
	}
	public static void debug(Object cllog,String message,Exception t) {
		if (cllog==null) {
			DfLogger.debug(EIUtils.class , message, null, t);
		} else {
			DfLogger.debug(cllog.getClass(), message, null, t);
		}
	}
	public static void info(Object cllog,String message,Exception t) {
		System.out.println(message);
		if (cllog==null) {
			DfLogger.info(EIUtils.class , message, null, t);
		} else {
			DfLogger.info(cllog.getClass(), message, null, t);
		}
	}
	public static void error(Object cllog,String message) {
		error(cllog,message,null);
	}	
	public static void debug(Object cllog,String message) {
		debug(cllog,message,null);
	}
	public static void info(Object cllog,String message) {
		info(cllog,message,null);
	}
}
