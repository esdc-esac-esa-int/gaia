package esac.archive.gacs.sl.services.publicgroup.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import esac.archive.gacs.sl.services.publicgroup.PublicGroupHandler;
import esac.archive.gacs.sl.services.publicgroup.PublicGroupItem;
import esac.archive.gacs.sl.services.publicgroup.PublicGroupUtils;
import esac.archive.gacs.sl.tap.actions.JDBCPooledFunctions;
import esavo.tap.TAPService;
import esavo.uws.event.UwsEventType;
import esavo.uws.output.UwsOutputResponseHandler;
import esavo.uws.owner.UwsJobOwner;

/**
 * Removes access to one or more public group tables.<br/>
 * User is extracted from the security context.<br/>
 * <p>Example
 * <pre><tt>
 * server/PublicGroup?ACTION=remove&TABLES=schema1.table1,schema2.table2
 * </tt></pre>
 * 
 * @author juan.carlos.segovia@sciops.esa.int
 *
 */
public class RemoveTableFromUserHandler implements PublicGroupHandler {
	
	public static final String ACTION = "remove";

	@Override
	public String getAction(){
		return ACTION;
	}

	@Override
	public boolean canHandle(String action) {
		return ACTION.equalsIgnoreCase(action);
	}

	@Override
	public void handle(UwsJobOwner user, HttpServletResponse response, TAPService service, Map<String,String> parameters) throws IOException {
		String tablesParam = parameters.get(PublicGroupHandler.PARAM_TABLES);
		List<String> tables = PublicGroupUtils.getTables(tablesParam);
		
		//Will raise an exception
		PublicGroupUtils.checkValidTables(tables);
		
		List<PublicGroupItem> items = PublicGroupUtils.getTableItemsForUser(tables, user.getId());
		
		try {
			JDBCPooledFunctions dbConn = (JDBCPooledFunctions)service.getFactory().createDBConnection(JDBC_PUBLIC_GROUP_CONNECTION);
			dbConn.removeAccessToPublicGroupTables(items);
			service.getFactory().getEventsManager().setEventTime(user, UwsEventType.PUBLIC_GROUP_HIDE_TABLE);
			PublicGroupUtils.writeMsg(response, UwsOutputResponseHandler.OK, UwsOutputResponseHandler.CONTENT_TYPE_TEXT_PLAIN, "OK");
		} catch (Exception e) {
			PublicGroupUtils.writeError(response, UwsOutputResponseHandler.INTERNAL_SERVER_ERROR, 
					"Cannot remove access to group tables for user '"+user.getId()+"'", e.getMessage());
		}
	}

}