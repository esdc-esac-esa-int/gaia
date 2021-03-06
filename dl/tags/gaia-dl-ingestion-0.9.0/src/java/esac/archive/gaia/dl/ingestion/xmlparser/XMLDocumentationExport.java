/*******************************************************************************
 * Copyright (C) 2017 European Space Agency
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package esac.archive.gaia.dl.ingestion.xmlparser;

import java.util.List;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="documentation-export")
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLDocumentationExport {

	@XmlElement(name = "table")
	public List<XMLTable> getTables() {
		return tables;
	}

	public void setTables(List<XMLTable> tables) {
		this.tables = tables;
	}

	@XmlTransient
	List<XMLTable> tables;
}
