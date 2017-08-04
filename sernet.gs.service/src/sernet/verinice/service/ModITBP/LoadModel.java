/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service.ModITBP;

import java.util.List;

import sernet.gs.service.RetrieveInfo;
import sernet.verinice.interfaces.GenericCommand;
import sernet.verinice.interfaces.INoAccessControl;
import sernet.verinice.model.moditbp.elements.ModITBPModel;

@SuppressWarnings("serial")
public class LoadModel extends GenericCommand implements INoAccessControl {

	private ModITBPModel model;

	public LoadModel() {
	}
	
	public void execute() {
		RetrieveInfo ri = new RetrieveInfo();
		ri.setChildren(true);
		List<ModITBPModel> modelList = getDaoFactory().getDAO(ModITBPModel.class).findAll(ri);
		if(modelList != null) {
			if(modelList.size()>1) {
				throw new RuntimeException("More than one ModITBPRoot found.");
			} else if(modelList.size()==1) {			
				model = modelList.get(0);
			}
		}
	}


	public ModITBPModel getModel() {
		return model;
	}
	
	

}
