package com.amexio;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;

/**
 * Import files from the file system using JDOM and XPATH Loop Thru Exported Folders Traverse thru Main xml file whose
 * name same as the folder from each folder using XPATH Traverse thru each child xml returned by the main xml Import
 * each exported document Dump Properties on each exported document by looking at the child xml
 *
 * @author zkaiserm
 * @author afarina
 *
 */
public class IManager {
	// identify Rep Credentials
	String iRep = null;
	String iUName = null;
	String iUPass = null;
	// identify Rep Folder Path
	String iRepFldrPath = null;
	// identify File System Folder Path
	String eFsFldrPath = null;
	// identify import folders
	String iFldrs = null;
	// identify export folders
	String eFldrs = null;
	String iProp = null;
	// identify import obj properties
	String iObjProp = null;
	// identify export obj properties
	String eObjProp = null;
	// identify import obj properties
	String iObjPropArr[] = null;
	// identify export obj properties
	String eObjPropArr[] = null;
	// identify export folders
	String eFldrsArr[] = null;
	// identify import folders
	String iFldrsArr[] = null;
	// identify object type
	String iType = null;
	// identify element in the xml file
	String eiXmlDataEle = null;
	// identify element path in the xml file
	String iXmlTraversePath = null;

	// Liste des attributs qui constituent la clé d'unicité
	String iUnicityArr[] = null;

	IDfSessionManager sMgr = null;
	IDfSession dfSession = null;

	// identify Map to map WLOA folder names to NPR folder names
	Map<String, String> ieFldrMap = null;
	private int compteur=0;

	/**
	 * initialize Rep Credentials Global Var
	 * @throws DfException 
	 */
	public IManager() throws DfException {
		this.iRep = EIUtils.getConfigVal(EIConstants.IREP, EIConstants.PROPFILE);
		this.iUName = EIUtils.getConfigVal(EIConstants.IUNAME, EIConstants.PROPFILE);
		this.iUPass = EIUtils.getConfigVal(EIConstants.IUPASS, EIConstants.PROPFILE);
		this.iRepFldrPath = EIUtils.getConfigVal(EIConstants.IREPFLDRPATH, EIConstants.PROPFILE);
		this.iType = EIUtils.getConfigVal(EIConstants.ITYPE, EIConstants.PROPFILE);
		this.eFsFldrPath = EIUtils.getConfigVal(EIConstants.EFSFLDRPATH, EIConstants.PROPFILE);
		this.iFldrs = EIUtils.getConfigVal(EIConstants.IFLDRS, EIConstants.PROPFILE);
		this.eFldrs = EIUtils.getConfigVal(EIConstants.EFLDRS, EIConstants.PROPFILE);
		this.eObjProp = EIUtils.getConfigVal(EIConstants.EOBJPROP, EIConstants.PROPFILE);
		this.iObjProp = EIUtils.getConfigVal(EIConstants.IOBJPROP, EIConstants.PROPFILE);
		this.eiXmlDataEle = EIUtils.getConfigVal(EIConstants.EIXMLDATAELE, EIConstants.PROPFILE);
		this.iXmlTraversePath = EIUtils.getConfigVal(EIConstants.IXMLTRAVERSEPATH, EIConstants.PROPFILE);
		this.eObjPropArr = this.eObjProp.split(EIConstants.COMMDELIMITER);
		this.iObjPropArr = this.iObjProp.split(EIConstants.COMMDELIMITER);
		this.eFldrsArr = this.eFldrs.split(EIConstants.PIPEDELIMITER);
		this.iFldrsArr = this.iFldrs.split(EIConstants.PIPEDELIMITER);
		this.ieFldrMap = EIUtils.buildColMapFromStrArr(this.eFldrsArr, this.iFldrsArr);
		this.iUnicityArr = EIUtils.getConfigVal(EIConstants.IOBJUNICITY, EIConstants.PROPFILE).split(EIConstants.PIPEDELIMITER);
		initialize();
	}

	/**
	 * Get Documentum Session Get ACL Object
	 * @return 
	 * @throws DfException 
	 */
	private String initialize() throws DfException {
		this.sMgr = EIUtils.createSessionManager(this.iRep, this.iUName, this.iUPass);
		this.dfSession = this.sMgr.getSession(this.iRep);
		return this.dfSession.getDBMSName();
	}

	/**
	 * Loop Thru Exported Folders Traverse Main XML File Traverse Child XML File Retrieve exported PDF File from the
	 * file system Retrieve exported object properties from child xmls
	 * @return 
	 *
	 * @throws Exception
	 */
	public int runImport() throws Exception {
		final String[] eFldrsList = this.eFldrs.split(EIConstants.PIPEDELIMITER);
		for (final String folderCible : eFldrsList) {
			EIUtils.debug(this,"Chemin des données à importer : " +this.iRepFldrPath + this.ieFldrMap.get(folderCible));
			String eFilePath = null;
			eFilePath = retFldrMainXmlFile(folderCible);
			EIUtils.debug(this, "Lecture du fichier principal : " + eFilePath);
			final List<?> parentFileChildList = IManager.getChildNodes(IManager.getJdomDoc(eFilePath),
					this.iXmlTraversePath);
			boolean abortTransaction=false;
			this.dfSession.beginTrans();
			EIUtils.debug(this,"Transaction de session démarrée");
			for (int j = 0; j < parentFileChildList.size(); j++) {
				final Element parentNode = (Element) parentFileChildList.get(j);
				EIUtils.debug(this, "Traitement du fichier " + parentNode.getValue());
				final List<?> childFileChildList = IManager.getChildNodes(IManager.getJdomDoc(parentNode.getValue()),
						this.iXmlTraversePath);
				for (int k = 0; k < childFileChildList.size(); k++) {
					final Element childNode = (Element) childFileChildList.get(k);
					final Map<String, String> objMap = new HashMap<String, String>();
					final List<?> children = childNode.getChildren();
					for (int l = 0; l < children.size(); l++) {
						final Element child = (Element) children.get(l);
						objMap.put(child.getName(), child.getValue());
					}
					try {
						if (checkUnicity(objMap,folderCible)) {
							doImport(objMap, folderCible);
						}
						else {
							throw new Exception(objMap.get("r_object_id")+" ("+objMap.get(this.eiXmlDataEle)+") : Un objet ayant la même clé d'unicité existe déjà --> la transaction sera annulée");
						}
					}
					catch(Exception e) {
						if (!e.getMessage().contains("DM_SESSION_E_TRANSACTION_ERROR")) {
							EIUtils.debug(this,"\tErreur --> La transaction de session sera abandonnée");
							abortTransaction=true;
						}
					}
				}
			}
			if (abortTransaction) {
				this.dfSession.abortTrans();
				throw new DfException("La transaction de session a été abandonnée en raison des erreurs précédentes");
			}
			this.dfSession.commitTrans();
			EIUtils.debug(this,"--> Transaction de session commitée");
		} 
		EIUtils.closeSess(this.sMgr, this.dfSession);
		return compteur;
	}

	private boolean checkUnicity(Map<String, String> objMap, String folderCible) throws DfException {
		if (iUnicityArr!=null && iUnicityArr.length>0) {
			StringBuffer whereBuf=new StringBuffer();
			for (int i = 0; i < iUnicityArr.length; i++) {
				if (i!=0) {
					whereBuf.append(" AND ");
				}
				if (iUnicityArr[i].equals("same_folder")){
					whereBuf.append("FOLDER('")
					.append(this.iRepFldrPath + this.ieFldrMap.get(folderCible))
					.append("')");
				} else {
					whereBuf.append(iUnicityArr[i])
					.append("='")
					.append(objMap.get(iUnicityArr[i]))
					.append("'");
				}
			}
			try {
				IDfSysObject sysObj= (IDfSysObject) this.dfSession.getObjectByQualification("dm_sysobject where "+whereBuf.toString());
				if (sysObj!=null) {
					EIUtils.error(this, "\tUn objet existe déjà avec la clé : "+whereBuf.toString());
					return false;
				}
			} catch (DfException e) {
				EIUtils.error(this, "\tErreur technique lors du contrôle d'unicité de l'objet à importer");
				throw e;
			}
		}
		return true;
	}

	/**
	 * Build Main XML File Path based on exported folder name
	 *
	 * @param fldrName
	 * @return
	 */
	private String retFldrMainXmlFile(final String fldrName) throws Exception {
		String nEsFldrPath = null;
		nEsFldrPath = this.eFsFldrPath.replace("\\", "\\\\");
		return nEsFldrPath + fldrName + "\\\\" + fldrName + ".xml";
	}

	/**
	 * Create New Documentum Object, Set Properties & Import
	 *
	 * @param objMap
	 * @param fldrName
	 * @throws Exception
	 */
	private void doImport(final Map<String, String> objMap, final String fldrName) throws Exception {
		final IDfSysObject newNPRObj = (IDfSysObject) this.dfSession.newObject(this.iType);
		Set<String> keys = objMap.keySet();
		boolean aclTraite=false;
		boolean throwError=false;
		for (Iterator<String> keysIt = keys.iterator(); keysIt.hasNext();) {
			String attributName = (String) keysIt.next();
			String attributValeur =objMap.get(attributName);
			if (!aclTraite && (attributName.equals("acl_name") || attributName.equals("acl_domain"))) {
				IDfACL aclObj=  (IDfACL) this.dfSession.getObjectByQualification("dm_acl where object_name ='"+objMap.get("acl_name")+"' and owner_name='"+objMap.get("acl_domain")+"'");
				if (aclObj!=null) {
					newNPRObj.setACLName(objMap.get("acl_name"));
					newNPRObj.setACLDomain(objMap.get("acl_domain"));
				}
				else {
					EIUtils.error(this,"\tErreur "+objMap.get("r_object_id")+" : l'ACL '"+objMap.get("acl_name")+"' ("+objMap.get("acl_domain")+") n'existe pas dans la docbase cible");
					throwError=true;
				}
				aclTraite=true;
			} else if (attributName.equals("owner_name")) {
				IDfUser usrObj=  (IDfUser) this.dfSession.getObjectByQualification("dm_user where user_name ='"+objMap.get("owner_name")+"'");
				if (usrObj!=null) {
					newNPRObj.setOwnerName(attributValeur);
				}
				else {
					EIUtils.error(this,"\tErreur "+objMap.get("r_object_id")+" : l'utilisateur '"+objMap.get("owner_name")+"' n'existe pas dans la docbase cible");
					throwError=true;
				}
			} else if (attributName.equals("a_content_type")) {
				if (objMap.get("a_content_type") != null && objMap.get(this.eiXmlDataEle) != null
						&& !objMap.get(this.eiXmlDataEle).equals("")) {
					newNPRObj.setContentType(objMap.get("a_content_type"));
					newNPRObj.setFile(objMap.get(this.eiXmlDataEle));
				}
			} else if (attributName.equals(this.eiXmlDataEle) || attributName.equals("r_object_id") || attributName.equals("r_object_type") || attributName.equals("r_creation_date")|| attributName.equals("r_modify_date")) {
				//ignorer : deja traiter
			} else {
				if (!attributValeur.contains(EIConstants.SEPARATEUR)) {
					newNPRObj.setString(attributName,attributValeur);
				} else {
					String[] valeurs = attributValeur.split(EIConstants.SEPARATEUR);
					for (int i = 0; i < valeurs.length; i++) {
						newNPRObj.appendString(attributName,valeurs[i]);
					}
				}
			}
		}
		if (throwError) {
			throw new Exception("\tDes erreurs ont été rencontrées lors de l'import de l'objet :"+objMap.get("r_object_id"));
		}
		try {
			newNPRObj.appendString("keywords","Migration 2017");
			newNPRObj.link(this.iRepFldrPath + this.ieFldrMap.get(fldrName));
			newNPRObj.save();
		}
		catch (DfException dfe) {
			EIUtils.error(this,"\tErreur "+objMap.get("r_object_id")+" : erreur lors de la sauvegarde." ,dfe);
			throw dfe;
		}
		EIUtils.debug(this,"\tOK "+objMap.get("r_object_id")+" importé");
		compteur++;
	}

	/**
	 * initialize SAX Parser to create a JDOM doc from xml file on the file system
	 *
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	private static Document getJdomDoc(final String fileName) throws Exception {
		final SAXBuilder saxBuilder = new SAXBuilder(false);
		final Document jdomDoc = saxBuilder.build(fileName);
		return jdomDoc;
	}

	/**
	 * Traverse thru JDOM Document to get Child Nodes
	 *
	 * @param jdomDoc
	 * @param elePath
	 * @return
	 * @throws Exception
	 */
	private static List<?> getChildNodes(final Document jdomDoc, final String elePath) throws Exception {
		final List<?> nodeList = XPath.selectNodes(jdomDoc, elePath);
		return nodeList;
	}

}
