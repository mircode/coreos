package os.admin.base;

import os.core.api.CoreOS;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;
@RequireWebServerExtender
public abstract class BaseCtrl {
	public abstract CoreOS getCoreOS();
}
