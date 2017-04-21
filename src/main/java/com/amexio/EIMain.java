package com.amexio;

import com.documentum.fc.common.DfException;

public class EIMain {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		boolean exportB=false;
		boolean importB=false;
		
		if (args.length<=0) {
			error("Paramètre manquant.");
			usage();
			System.exit(1);
		} else if (args.length>2) {
			error("Trop de paramètres.");
			usage();
			System.exit(1);			
		}
		boolean errorB=false;
		for (int i = 0; i < args.length; i++) {
			String argu=args[i];
			if (argu.equalsIgnoreCase("-E")) {
				exportB=true;
			} else if (argu.equalsIgnoreCase("-I")) {
				importB=true;
			} else if (argu.equalsIgnoreCase("-EI") || argu.equalsIgnoreCase("-IE")) {
				exportB=true;
				importB=true;
			} else {
				error("paramètre incorrect : "+argu);
				errorB=true;
			}
		}
		if (errorB) {
			usage();
			System.exit(1);
		}
		if (exportB) {
		EManager eManObj = null;
		try {
			eManObj = new EManager();
		} catch (DfException e) {
			error("Impossible d'ouvrir une session Documentum pour l'export",e);
			System.exit(1);
		}
		info("START EXPORT");
		try {
			eManObj.runExport();
		} catch (Exception e) {
			error("Erreur lors de l'export",e);
			System.exit(1);
		}
		info("END EXPORT");
		}		
		if (importB) {
			info("START IMPORT");
			IManager iManObj = null;
			try {
				iManObj = new IManager();
			} catch (DfException e) {
				error("Impossible d'ouvrir une session Documentum pour l'import",e);
				System.exit(1);
			}
			try {
				int compteur=iManObj.runImport();
				if (compteur==0){
					info("--> Aucun objet importé");
				}
				else {
					info("--> Nombre d'objet(s) importé(s) : "+compteur);
				}
			} catch (Exception e) {
				error("Erreur lors de l'import",e);
				info("--> Aucun objet importé");
				System.exit(1);
			}
			info("END IMPORT");
		}
	}

	private static void error(String message,Exception t) {
		EIUtils.error(null, message, t);
	}

	private static void info(String message,Exception t) {
		EIUtils.info(null, message, t);
	}
	private static void error(String message) {
		error(message,null);
	}	
	private static void info(String message) {
		info(message,null);
	}
	private static void usage() {
		System.out.println("Utilisation : ");
		System.out.println("\t expimp.bat {options}");
		System.out.println("\t\t Valeurs possibles pour {options} : ");
		System.out.println("\t\t\t -E : export seul");
		System.out.println("\t\t\t -I : import seul");
		System.out.println("\t\t\t -EI : export et import");
	}
}
