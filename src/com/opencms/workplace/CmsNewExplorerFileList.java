package com.opencms.workplace;

/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsNewExplorerFileList.java,v $
 * Date   : $Date: 2000/12/07 08:12:36 $
 * Version: $Revision: 1.4 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.*;
import com.opencms.launcher.*;
import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.template.*;
import com.opencms.util.*;

import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Template class for dumping files to the output without further 
 * interpreting or processing.
 * This can be used for plain text files or files containing graphics.
 * 
 * @author Alexander Lucas
 * @version $Revision: 1.4 $ $Date: 2000/12/07 08:12:36 $
 */
public class CmsNewExplorerFileList implements I_CmsDumpTemplate, I_CmsLogChannels, I_CmsConstants, I_CmsWpConstants {
	
	/** 
	 * Template cache is not used here since we don't include
	 * any subtemplates.
	 */
	private static I_CmsTemplateCache m_cache = null;

	/** Boolean for additional debug output control */
	private static final boolean C_DEBUG = false;
	public CmsNewExplorerFileList() {
	}
/**
 * Insert the method's description here.
 * Creation date: (29.11.00 14:05:21)
 * @return boolean
 * @param cms com.opencms.file.CmsObject
 * @param path java.lang.String
 */
private boolean folderExists(CmsObject cms, String path) {
	try{
		cms.readFolder(path);
	}catch(CmsException e){
		return false;
	}
	return true;
}
/**
 * Gets the content of a given template file.
 * 
 * @param cms CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file 
 * @param elementName <em>not used here</em>.
 * @param parameters <em>not used here</em>.
 * @return Unprocessed content of the given template file.
 * @exception CmsException 
 */
public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters) throws CmsException {
	if (C_DEBUG && A_OpenCms.isLogging()) {
		A_OpenCms.log(C_OPENCMS_DEBUG, "[CmsDumpTemplate] Now dumping contents of file " + templateFile);
	}
	/*		try {
	s = cms.readFile(templateFile).getContents();
	} catch(Exception e) {
	String errorMessage = "Error while reading file " + templateFile + ": " + e;
	if(A_OpenCms.isLogging()) {
	A_OpenCms.log(C_OPENCMS_CRITICAL, "[CmsDumpTemplate] " + errorMessage);
	e.printStackTrace();
	}
	if(e instanceof CmsException) {
	throw (CmsException)e;
	} else {
	throw new CmsException(errorMessage, CmsException.C_UNKNOWN_EXCEPTION);
	}
	}
	*/
	I_CmsSession session = cms.getRequestContext().getSession(true);
	CmsXmlWpTemplateFile templateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
	CmsXmlLanguageFile lang = templateDocument.getLanguageFile();

	// get the right folder
	String currentFolder = (String) parameters.get("folder");
	if ((currentFolder != null) && (!"".equals(currentFolder)) && folderExists(cms, currentFolder)) {
		session.putValue(C_PARA_FILELIST, currentFolder);
	} else {
		currentFolder = (String) session.getValue(C_PARA_FILELIST);
		if (currentFolder == null) {
			currentFolder = cms.rootFolder().getAbsolutePath();
		}
	}
	// get the checksum
	String checksum = (String) parameters.get("check");
	boolean newTreePlease = true;
	long check = -1;
	try {
		check = Long.parseLong(checksum);
		if(check == cms.getFileSystemFolderChanges()){
			newTreePlease = false;
		} 
	} catch (Exception e) {
		//TODO:			e.printStackTrace();
	}
System.err.println("mgm-- checksum="+checksum+"  Kernzahl="+cms.getFileSystemFolderChanges()+"  newTreePlease="+newTreePlease+"\n");
	// get the currentFolder Id
	int currentFolderId = (cms.readFolder(currentFolder)).getResourceId();

	// start creating content
	StringBuffer content = new StringBuffer();
	content.append("<html> \n<head> \n<script language=JavaScript> \n<!-- \nfunction show_help() \n{ return '2_1_2_2.html'; } \n//--> \n</script>\n\n<script language=JavaScript>\n");
	content.append("function initialize() {\n");
	// the project
	content.append(" top.setProject(" + cms.getRequestContext().currentProject().getId() + ");\n");
	// the onlineProject
	content.append(" top.setOnlineProject(" + cms.onlineProject().getId() + ");\n");
	// set the checksum for the tree
	content.append(" top.setChecksum(\"" + check + "\");\n");
	// the folder
	content.append(" top.setDirectory(" + currentFolderId + ",\"" + currentFolder + "\");\n");
	content.append(" top.rD();\n\n");
	// now the entries for the filelist
	Vector resources = cms.getResourcesInFolder(currentFolder);
	for (int i = 0; i < resources.size(); i++) {
		CmsResource res = (CmsResource) resources.elementAt(i);
		content.append(" top.aF(");
		// the name
		content.append("\"" + res.getName() + "\", ");
		// the path
		content.append("\"" + res.getPath() + "\", ");
		// the title
		String title = "";
		try {
			title = cms.readProperty(res.getAbsolutePath(), C_PROPERTY_TITLE);
		} catch (CmsException e) {
		}
		if (title == null) {
			title = "";
		}
		content.append("\"" + title + "\", ");
		// the type
		content.append("\"" + res.getType() + "\", ");
		// date of last change
		content.append("\"" + Utils.getNiceDate(res.getDateLastModified()) + "\", ");
		// TODO:user who changed it
		content.append("\"" + "TODO" + "\", ");
		// date
		content.append("\"" + Utils.getNiceDate(res.getDateCreated()) + "\", ");
		// size
		if (res.isFolder()) {
			content.append("\"" + "" + "\", ");
		} else {
			content.append("\"" + res.getLength() + "\", ");
		}
		// state
		content.append("" + res.getState() + ", ");
		// project
		content.append("\"" + res.getProjectId() + "\", ");
		// owner
		content.append("\"" + cms.readUser(res.getOwnerId()).getName() + "\", ");
		// group
		content.append("\"" + cms.readGroup(res).getName() + "\", ");
		// accessFlags
		content.append("\"" + res.getAccessFlags() + "\", ");
		// locked by
		if (res.isLockedBy() == C_UNKNOWN_ID) {
			content.append("\"" + "" + "\");\n");
		} else {
			content.append("\"" + cms.lockedBy(res).getName() + "\");\n");
		}
	}
	// TODO: now the tree, only if changed
	if (true) {
		content.append("\n top.rT();\n");
		Vector tree = cms.getFolderTree();
		int startAt = 1;
		int parentId;
		boolean grey = false;
		int onlineProjectId = cms.onlineProject().getId();
		if (onlineProjectId == cms.getRequestContext().currentProject().getId()) {
			// all easy: we are in the onlineProject
			CmsFolder rootFolder = (CmsFolder) tree.elementAt(0);
			content.append("top.aC(");
			content.append(rootFolder.getResourceId() + ", ");
			content.append("\"" + lang.getDataValue("title.rootfolder") + "\", ");
			content.append(rootFolder.getParentId() + ", false);\n");
			for (int i = startAt; i < tree.size(); i++) {
				CmsFolder folder = (CmsFolder) tree.elementAt(i);
				content.append("top.aC(");
				// id
				content.append(folder.getResourceId() + ", ");
				// name
				content.append("\"" + folder.getName() + "\", ");
				// parentId
				content.append(folder.getParentId() + ", false);\n");
			}
		} else {
			// offline Project
			Hashtable idMixer = new Hashtable();
			CmsFolder rootFolder = (CmsFolder) tree.elementAt(0);
			if (rootFolder.getProjectId() != onlineProjectId) {
				startAt = 2;
				grey = false;
				idMixer.put(new Integer(((CmsFolder) tree.elementAt(1)).getResourceId()), new Integer(rootFolder.getResourceId()));
			} else {
				grey = true;
			}
			content.append("top.aC(");
			content.append(rootFolder.getResourceId() + ", ");
			content.append("\"" + lang.getDataValue("title.rootfolder") + "\", ");
			content.append(rootFolder.getParentId() + ", " + grey + ");\n");
			for (int i = startAt; i < tree.size(); i++) {
				CmsFolder folder = (CmsFolder) tree.elementAt(i);
				if (folder.getProjectId() != onlineProjectId) {
					grey = false;
					parentId = folder.getParentId();
					if(!(folder.getState() == 2)){
						i++;
						idMixer.put(new Integer(((CmsFolder)tree.elementAt(i)).getResourceId()), new Integer(folder.getResourceId()));
					}
				} else {
					grey = true;
					parentId = folder.getParentId();
					if (idMixer.containsKey(new Integer(parentId))) {
						parentId = ((Integer) idMixer.get(new Integer(parentId))).intValue();
					}
				}
				content.append("top.aC(");
				// id
				content.append(folder.getResourceId() + ", ");
				// name
				content.append("\"" + folder.getName() + "\", ");
				// parentId
				content.append(parentId + ", " + grey + ");\n");
			}
		}
	}
	content.append(" top.dU(document); \n");
	content.append("}\n");
	content.append("</script>\n</head> \n<BODY onLoad=\"initialize()\"></BODY> \n</html>\n");
	
	return (content.toString()).getBytes();
}
	/**
	 * Gets the content of a given template file.
	 * 
	 * @param cms CmsObject Object for accessing system resources
	 * @param templateFile Filename of the template file 
	 * @param elementName <em>not used here</em>.
	 * @param parameters <em>not used here</em>.
	 * @param templateSelector <em>not used here</em>.
	 * @return Unprocessed content of the given template file.
	 * @exception CmsException 
	 */
	public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
		// ignore the templateSelector since we only dump the template
		return getContent(cms, templateFile, elementName, parameters);
	}
	/**
	 * Gets the key that should be used to cache the results of
	 * this template class. 
	 * <P>
	 * Since this class is quite simple it's okay to return
	 * just the name of the template file here.
	 * 
	 * @param cms CmsObject Object for accessing system resources
	 * @param templateFile Filename of the template file 
	 * @param parameters Hashtable with all template class parameters.
	 * @param templateSelector template section that should be processed.
	 * @return key that can be used for caching
	 */
	public Object getKey(CmsObject cms, String templateFile, Hashtable parameter, String templateSelector) {
		//return templateFile.getAbsolutePath();
		//Vector v = new Vector();
		CmsRequestContext reqContext = cms.getRequestContext();
		
		//v.addElement(reqContext.currentProject().getName());
		//v.addElement(templateFile);
		//return v;
		return "" + reqContext.currentProject().getId() + ":" + templateFile;
	}
	/** 
	 * Template cache is not used here since we don't include
	 * any subtemplates. So we can always return <code>true</code> here.
	 * @return <code>true</code>
	 */
	public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
		return false;
	}
	/** 
	 * Any results of this class are cacheable since we don't include
	 * any subtemplates. So we can always return <code>true</code> here.
	 * @return <code>true</code>
	 */
	public boolean isTemplateCacheSet() {
		return true;
	}
	/** 
	 * Template cache is not used here since we don't include
	 * any subtemplates <em>(not implemented)</em>.
	 */
	public void setTemplateCache(I_CmsTemplateCache c) {
		// do nothing.
	}
	/** 
	 * Template cache is not used here since we don't include
	 * any subtemplates. So we can always return <code>false</code> here.
	 * @return <code>false</code>
	 */
	public boolean shouldReload(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
		return false;
	}
}
