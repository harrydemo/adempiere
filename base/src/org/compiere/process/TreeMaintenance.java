/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTree_Node;
import org.compiere.model.MTree_NodeBP;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.MTree_NodePR;
import org.compiere.model.MTree_NodeU1;
import org.compiere.model.MTree_NodeU2;
import org.compiere.model.MTree_NodeU3;
import org.compiere.model.MTree_NodeU4;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Tree Maintenance	
 *	
 *  @author Jorg Janke
 *  @version $Id: TreeMaintenance.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *  @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 2015-09-09
 *  	<li>FR [ 9223372036854775807 ] Add Support to Dynamic Tree
 *  @see https://adempiere.atlassian.net/browse/ADEMPIERE-442
 *  @author Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 *  		<a href="https://github.com/adempiere/adempiere/issues/729">
 *			@see FR [ 729 ] Add Support to Parent Column And Search Column for Tree </a>
 */
public class TreeMaintenance extends SvrProcess
{
	/**	Tree				*/
	private int		m_AD_Tree_ID;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
		m_AD_Tree_ID = getRecord_ID();		//	from Window
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		log.info("AD_Tree_ID=" + m_AD_Tree_ID);
		if (m_AD_Tree_ID == 0)
			throw new IllegalArgumentException("Tree_ID = 0");
		MTree tree = new MTree (getCtx(), m_AD_Tree_ID, get_TrxName());	
		if (tree == null || tree.getAD_Tree_ID() == 0)
			throw new IllegalArgumentException("No Tree -" + tree);
		//
		if (MTree.TREETYPE_BoM.equals(tree.getTreeType()))
			return "BOM Trees not implemented";
		return verifyTree(tree);
	}	//	doIt

	/**
	 *  Verify Tree
	 * 	@param tree tree
	 */
	private String verifyTree (MTree tree)
	{
		String nodeTableName = tree.getNodeTableName();
		String sourceTableName = tree.getSourceTableName();
		String sourceTableKey = sourceTableName + "_ID";
		int AD_Client_ID = tree.getAD_Client_ID();
		//int C_Element_ID = 0;
		
		List<Integer> treeElements = new ArrayList<Integer>();
		
		
		if (MTree.TREETYPE_ElementValue.equals(tree.getTreeType())
		||	MTree.TREETYPE_User1.equals(tree.getTreeType())
		||	MTree.TREETYPE_User2.equals(tree.getTreeType())
		||	MTree.TREETYPE_User3.equals(tree.getTreeType())
		||	MTree.TREETYPE_User4.equals(tree.getTreeType()))
		{
			String sql = "SELECT C_Element_ID FROM C_Element "
				+ "WHERE AD_Tree_ID= ?"  ;
			
			int[] elements = DB.getIDsEx(null, sql, tree.getAD_Tree_ID());
			for (int i : elements) 
				treeElements.add(i);
			
			if (elements.length <= 0)
				throw new IllegalStateException("No Account Element found");
		}else
			treeElements.add(0);
		
		AtomicReference<Boolean> ok = new AtomicReference<>();
		ok.set(true);
		
		treeElements.forEach(treeElement ->{
			MColumn parentColumn = null;
			MTable sourceTable	 = null;
			String[] keyColumns = null;
			if (tree.getParent_Column_ID() > 0) {
				parentColumn = MColumn.get(Env.getCtx(), tree.getParent_Column_ID());
				sourceTable = MTable.get(Env.getCtx(),tree.getAD_Table_ID());
				keyColumns = sourceTable.getKeyColumns();
			}
			int C_Element_ID = treeElement;
			//	Delete unused
			StringBuffer sql = new StringBuffer();
			sql.append("DELETE ").append(nodeTableName)
				.append(" WHERE AD_Tree_ID=").append(tree.getAD_Tree_ID())
				.append(" AND Node_ID NOT IN (SELECT ").append(sourceTableKey)
				.append(" FROM ").append(sourceTableName)
				.append(" st WHERE st.AD_Client_ID=").append(AD_Client_ID);
			if (C_Element_ID > 0)
				sql.append(" AND EXISTS (SELECT 1 FROM C_Element WHERE ")
					.append(" C_Element_ID=").append(C_Element_ID)
					.append(" AND C_Element.AD_Tree_ID = ").append(nodeTableName).append(".AD_Tree_ID)");
			sql.append(")");
			log.finer(sql.toString());
			//
			int deletes = DB.executeUpdate(sql.toString(), get_TrxName());
			addLog(0,null, new BigDecimal(deletes), tree.getName()+ " Deleted");
			if (tree.isAllNodes()) {
				//	Insert new
				int inserts = 0;
				sql = new StringBuffer();
				sql.append("SELECT ").append(sourceTableKey)
					.append(" FROM ").append(sourceTableName)
					.append(" WHERE AD_Client_ID=").append(AD_Client_ID);
				if (C_Element_ID > 0)
					sql.append(" AND C_Element_ID=").append(C_Element_ID);
				sql.append(" AND ").append(sourceTableKey)
					.append("  NOT IN (SELECT Node_ID FROM ").append(nodeTableName)
					.append(" WHERE AD_Tree_ID=").append(tree.getAD_Tree_ID()).append(")");
				log.finer(sql.toString());
				//
				
				PreparedStatement pstmt = null;
				try
				{
					pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
					ResultSet rs = pstmt.executeQuery();
					while (rs.next())
					{
						int Node_ID = rs.getInt(1);
						PO node = null;
						if (nodeTableName.equals("AD_TreeNode"))
							node = new MTree_Node(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodeBP"))
							node = new MTree_NodeBP(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodePR"))
							node = new MTree_NodePR(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodeMM"))
							node = new MTree_NodeMM(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodeU1"))
							node = new MTree_NodeU1(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodeU2"))
							node = new MTree_NodeU2(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodeU3"))
							node = new MTree_NodeU3(tree, Node_ID);
						else if (nodeTableName.equals("AD_TreeNodeU4"))
							node = new MTree_NodeU4(tree, Node_ID);
						//				
						if (node == null)
							log.log(Level.SEVERE, "No Model for " + nodeTableName);
						else
						{
							//FR [ 729 ]
							if (keyColumns!=null 
									&& keyColumns.length>0
										&& parentColumn!=null) {
								String whereClause = keyColumns[0] + "=" + node.get_ID();
								PO table = MTable.get(Env.getCtx(), sourceTableName).getPO(whereClause, node.get_TrxName());
								if (table.get_ID() > 0) {
									if (node.get_ID()>0)
										node.set_ValueOfColumn("Parent_ID", table.get_ValueAsInt(parentColumn.getColumnName()));
									else 
										node.set_ValueOfColumn("Parent_ID", null);
								}
							}
							
							if (node.save())
								inserts++;
							else
								log.log(Level.SEVERE, "Could not add to " + tree + " Node_ID=" + Node_ID);
						}
					}
					rs.close();
					
					
					sql = new StringBuffer();
					pstmt.close();
					pstmt = null;
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "verifyTree", e);
					ok.set(false);
				}
				try
				{
					if (pstmt != null)
						pstmt.close();
					pstmt = null;
				}
				catch (Exception e)
				{
					pstmt = null;
				}
				//FR [ 729 ]
				if (keyColumns!=null 
						&& keyColumns.length>0
						 	&& parentColumn!=null) {
					
					String elementFilter = "";
					if (C_Element_ID > 0)
						elementFilter = " AND st.C_Element_ID=" + C_Element_ID;
					
					String whereClause = "NOT EXISTS (SELECT 1 FROM " + sourceTableName + " st "
														+ "WHERE " +nodeTableName + ".Node_ID = st." + keyColumns[0] + " "
														+ "AND " + nodeTableName + ".Parent_ID = COALESCE(st." + parentColumn.getColumnName() + ",0) " 
														+ elementFilter
														+ ") " +
										 "AND EXISTS (SELECT 1 FROM " + sourceTableName + " st "
														+ "WHERE " +nodeTableName + ".Parent_ID = st." + keyColumns[0] + elementFilter +  ") "
										+ "AND AD_Tree_ID = ? ";
					List<PO> parentNodes = new Query(getCtx(), nodeTableName, whereClause, get_TrxName()).setParameters(tree.getAD_Tree_ID()).list();
					int updated = 0;
					for (PO node : parentNodes) {
						whereClause = keyColumns[0] + "=" + node.get_ID();
						PO table = MTable.get(Env.getCtx(), sourceTableName).getPO(whereClause, node.get_TrxName());
						if (table.get_ID() > 0) {
							if (node.get_ID()>0) {
								if (node.get_ValueAsInt("Parent_ID")>0)
									table.set_ValueOfColumn(parentColumn.getColumnName(), node.get_ValueAsInt("Parent_ID"));
								else 
									table.set_ValueOfColumn(parentColumn.getColumnName(), null);
								
								table.saveEx();
							}
						}
						updated +=1;
					}
					addLog(0,null, new BigDecimal(updated), tree.getName()+ " Updated");
				}
				addLog(0,null, new BigDecimal(inserts), tree.getName()+ " Inserted");
			}
		});
		return tree.getName() + (ok.get() ? " OK" : " Error");
	}	//	verifyTree

}	//	TreeMaintenence
