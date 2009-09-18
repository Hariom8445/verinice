/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak@sernet.de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 *     Robert Schuster <r.schuster@tarent.de> - use HibernateCallback
 ******************************************************************************/
package sernet.gs.ui.rcp.main.service.crudcommands;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import sernet.gs.ui.rcp.main.bsi.model.BSIModel;
import sernet.gs.ui.rcp.main.common.model.CnATreeElement;
import sernet.gs.ui.rcp.main.common.model.HydratorUtil;
import sernet.gs.ui.rcp.main.connect.IBaseDao;
import sernet.gs.ui.rcp.main.service.commands.GenericCommand;

@SuppressWarnings("serial")
public class LoadPolymorphicCnAElementById extends GenericCommand {
	
	public LoadPolymorphicCnAElementById(Integer[] ds) {
		ids = ds;
	}

	private Integer[] ids;

	private List<CnATreeElement> list = new ArrayList<CnATreeElement>();
	
	@SuppressWarnings("unchecked")
	public void execute() {
		IBaseDao<? extends CnATreeElement, Serializable> dao = getDaoFactory().getDAO(BSIModel.class);
		
		list = (List<CnATreeElement>) dao.findByCallback(new Callback(ids));
		HydratorUtil.hydrateElements(dao, list, false);
	}

	public List<CnATreeElement> getElements() {
		return list;
	}

	private static class Callback implements HibernateCallback, Serializable {
		
		Integer[] ids;
		
		Callback(Integer[] ids)
		{
			this.ids = ids;
		}

		public Object doInHibernate(Session session) throws HibernateException,
				SQLException {
			
			Query query = session.createQuery(
					"from CnATreeElement cte "
					+ "where cte.dbId in (:ids)")
					.setParameterList("ids", ids);
			
			return query.list();
		}

	}
}
