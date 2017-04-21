package com.amexio;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * Export Files From Document Repository. Generate individual xml's for each file exported Generate main xml which lists
 * all individual xml's
 *
 * @author zkaiserm
 * @author afarina
 *
 */
public class EManager {

	// identify Rep Credentials
	String eRep = null;
	String eUName = null;
	String eUPass = null;
	// identify rep folder path
	String eRepFldrPath = null;
	// identify file system path
	String eFsFldrPath = null;
	// identify folder that need to be exported
	String eFldrs = null;
	// identify object file properties
	String eObjProp = null;
	String eObjPropArr[] = null;

	String eiXmlDataEle = null;
	IDfSessionManager sMgr = null;
	IDfSession dfSession = null;

	/**
	 * initialize Rep Credentials Global Var
	 * @throws DfException 
	 */
	public EManager() throws DfException {
		this.eRepFldrPath = EIUtils.getConfigVal(EIConstants.EREPFLDRPATH, EIConstants.PROPFILE);
		this.eFsFldrPath = EIUtils.getConfigVal(EIConstants.EFSFLDRPATH, EIConstants.PROPFILE);
		this.eFldrs = EIUtils.getConfigVal(EIConstants.EFLDRS, EIConstants.PROPFILE);
		this.eRep = EIUtils.getConfigVal(EIConstants.EREP, EIConstants.PROPFILE);
		this.eUName = EIUtils.getConfigVal(EIConstants.EUNAME, EIConstants.PROPFILE);
		this.eUPass = EIUtils.getConfigVal(EIConstants.EUPASS, EIConstants.PROPFILE);
		this.eObjProp = EIUtils.getConfigVal(EIConstants.EOBJPROP, EIConstants.PROPFILE);
		this.eiXmlDataEle = EIUtils.getConfigVal(EIConstants.EIXMLDATAELE, EIConstants.PROPFILE);
		this.eObjPropArr = this.eObjProp.split(EIConstants.COMMDELIMITER);
		initialize();
	}

	/**
	 * Get DCTM session
	 * @throws DfException 
	 */
	private void initialize() throws DfException {
			this.sMgr = EIUtils.createSessionManager(this.eRep, this.eUName, this.eUPass);
			this.dfSession = this.sMgr.getSession(this.eRep);
	}

	/**
	 * Create Export Directories based on "properties" file
	 *
	 * @throws Exception
	 */
	public void runExport() throws Exception  {
		String dqlStr = null;
		String fldrPath = null;
		String dirPath = null;
		// Get Export Folders
		final String[] eFldrsList = this.eFldrs.split(EIConstants.PIPEDELIMITER);
		// Loop Thru Folders
		for (final String element : eFldrsList) {
			fldrPath = this.eRepFldrPath + element;
			// Make FS Dir
			try {
				dirPath = EIUtils.makeDir(this.eFsFldrPath, element);
			} catch (Exception e) {
				EIUtils.error(this, "Erreur lors de la création du dossier d'export",e );
				throw e;
			}
			dirPath = dirPath + "\\";
			EIUtils.info(this,"Chemin cible : "+dirPath);
			try {
				dqlStr = EIUtils.retDqlString(this.eObjProp, fldrPath);
			} catch (Exception e) {
				EIUtils.error(this, "Erreur lors de la récupération de la requête DQL d'export",e );
				throw e;
			}
			try {
				doExport(element, dqlStr, dirPath);
			} catch (Exception e) {
				EIUtils.error(this, "\tErreur lors de l'export de l'objet : ");
				throw e;
			}
		}
		EIUtils.closeSess(this.sMgr, this.dfSession);
	}

	/**
	 * Export file object from DCTM Generate xml file for each file object Dump object properties into each xml file
	 * Generate a Main XML for each exported Folder
	 *
	 * @param fldrName
	 * @param dqlStr
	 * @param dirPath
	 * @throws IOException 
	 * @throws DfException 
	 */
	private void doExport(final String fldrName, final String dqlStr, final String dirPath) throws Exception {
		IDfCollection coll = null;
		final Map<String, String> attrMap = new HashMap<String, String>();
		FileOutputStream mainFldrXml = null;
		try {
			mainFldrXml = new FileOutputStream(dirPath + fldrName + ".xml");
		} catch (FileNotFoundException e1) {
			EIUtils.error(this, "Erreur lors la création du fichier XML d'export : " +dirPath + fldrName + ".xml",e1);
			throw e1;
		}
		byte[] strBytes = null;
		try {
			final StringBuffer sBuf1 = new StringBuffer();
			sBuf1.append("<?xml version='1.0' ?>");
			sBuf1.append("<");
			sBuf1.append("objects");
			sBuf1.append(">");
			strBytes = sBuf1.toString().getBytes();
			try {
				mainFldrXml.write(strBytes);
			} catch (IOException e) {
				EIUtils.error(this,"Impossible d'écrire dans le fichier XML d'export",e);
				throw e;
			}
			coll = EIUtils.executeQuery(dqlStr, this.dfSession);

			int fileCnt = 0;
			// Loop Thru Files
			try {
				while (coll.next()) {
					fileCnt++;
					for (final String attrName : this.eObjPropArr) {
						String attrVal = "";
						if (coll.isAttrRepeating(attrName)) {
							attrVal = coll.getAllRepeatingStrings(attrName, EIConstants.SEPARATEUR);
						} else {
							attrVal = coll.getString(attrName);
						}
						attrMap.put(attrName, attrVal);
					}
					EIUtils.debug(this,"\tExport objId:" + attrMap.get("r_object_id"));
					// Get File
					final IDfSysObject sysObj = (IDfSysObject) this.dfSession.getObject(new DfId(attrMap
							.get(this.eObjPropArr[0])));
					final String contType = sysObj.getContentType();
					if (contType != null && !contType.equals("")) {
						final IDfFormat forma = this.dfSession.getFormat(contType);
						final String extension = forma.getDOSExtension();
						sysObj.getFile(dirPath + attrMap.get(this.eObjPropArr[0]) + "_content." + extension);
						// add File Path to the Map
						attrMap.put(this.eiXmlDataEle, dirPath + attrMap.get(this.eObjPropArr[0]) + "_content." + extension);
					}
					// Create Prop Xml
					final String xmlFilePath = EIUtils.createObjectPropXml(sysObj, dirPath, attrMap,
							attrMap.get(this.eObjPropArr[0]) + "_prop");
					EIUtils.createMainPropXml(fldrName, xmlFilePath, mainFldrXml);
				}
			} catch (Exception e) {
				EIUtils.error(this,"Erreur lors de l'export des objets",e);
				throw e;
			}

			final StringBuffer sBuf2 = new StringBuffer();
			sBuf2.append("<");
			sBuf2.append("count");
			sBuf2.append(">");
			sBuf2.append(fileCnt);
			sBuf2.append("</");
			sBuf2.append("count");
			sBuf2.append(">");

			sBuf2.append("<");
			sBuf2.append("validate");
			sBuf2.append(">");
			sBuf2.append(dqlStr);
			sBuf2.append("</");
			sBuf2.append("validate");
			sBuf2.append(">");

			sBuf2.append("</");
			sBuf2.append("objects");
			sBuf2.append(">");
			strBytes = sBuf2.toString().getBytes();
			try {
				mainFldrXml.write(strBytes);
			} catch (IOException e) {
				EIUtils.error(this,"Impossible d'écrire dans le fichier XML d'export",e);
				throw e;
			}
		} finally {
			mainFldrXml.close();
			try {
				if (coll != null) {
					coll.close();
				}
			} catch (final DfException de) {
				de.printStackTrace();
			}
		}
	}

}
