package kohgylw.kiftd.server.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ResourceService {
	
	public void getResource(HttpServletRequest request,HttpServletResponse response);
	
	public void getWordView(String fileId,HttpServletRequest request,HttpServletResponse response);

}
