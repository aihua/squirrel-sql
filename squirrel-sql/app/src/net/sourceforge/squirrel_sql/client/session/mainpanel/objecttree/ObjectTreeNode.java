package net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree;
/*
 * Copyright (C) 2002 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;

import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * This is a node in the object tree.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class ObjectTreeNode extends DefaultMutableTreeNode
{
	/** Current session. */
	private ISession _session;

	/** Describes the database object represented by this node. */
	private IDatabaseObjectInfo _dbinfo;

	/** Collection of <TT>INodeExpander</TT> objects for this node. */
	private List _expanders = new ArrayList();

	/**
	 * Ctor that assumes node cannot have children.
	 * 
	 * @param	session	Current session.
	 * @param	dbinfo	Describes this object in the database.
	 * 
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>ISession</TT> or
	 *			<TT>IDatabaseObjectInfo</TT> passed.
	 */
	public ObjectTreeNode(ISession session, IDatabaseObjectInfo dbinfo)
	{
		super(getNodeTitle(dbinfo));
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}
		_session = session;
		_dbinfo = dbinfo;
	}

	/**
	 * Return the current session.
	 * 
	 * @return	the current session.
	 */
	public ISession getSession()
	{
		return _session;
	}

	/**
	 * Return the <TT>IDatabaseObjectInfo</TT> object that describes the
	 * database object represented by this node.
	 */
	public IDatabaseObjectInfo getDatabaseObjectInfo()
	{
		return _dbinfo;
	}

	/**
	 * Returns <TT>true</TT> if this node can have children. If there are
	 * expanders for this node then <TT>true</TT> is returned.
	 * 
	 * @return	<TT>true</TT> if this node can have children.
	 */
	public boolean getAllowsChildren()
	{
		return true;
//		return _expanders.size() != 0;
	}

	/**
	 * Return the expanders for this node. Remember that these are in addition
	 * to the standard expanders stored in the object tree model. Normally
	 * this would be empty.
	 * 
	 * @return	The <TT>INodeExpander</TT> objects for this node.
	 */
	public INodeExpander[] getExpanders()
	{
		return (INodeExpander[])_expanders.toArray(new INodeExpander[_expanders.size()]);
	}

	/**
	 * Adds an expander to this node.
	 * 
	 * @param	value	New <TT>INodeExpander</TT> for this node.
	 * 
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT>INodeExpander</TT> passed.
	 */
	public void addExpander(INodeExpander value)
	{
		if (value == null)
		{
			throw new IllegalArgumentException("INodeExpander == null");
		}

		_expanders.add(value);
	}

	private static String getNodeTitle(IDatabaseObjectInfo dbinfo)
	{
		if (dbinfo == null)
		{
			throw new IllegalArgumentException("IDatabaseObjectInfo == null");
		}
		return dbinfo.toString();
	}
}