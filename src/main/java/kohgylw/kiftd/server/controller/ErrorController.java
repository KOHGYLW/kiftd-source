package kohgylw.kiftd.server.controller;

import javax.annotation.*;
import kohgylw.kiftd.server.util.*;
import javax.servlet.http.*;
import kohgylw.kiftd.printer.*;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
@RequestMapping({ "/errorController" })
public class ErrorController {
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;

	@RequestMapping({ "/pageNotFound.do" })
	public String handleError(final HttpServletRequest request, final HttpServletResponse response) {
		return response.encodeURL("/prv/error.html");
	}

	@ExceptionHandler({ Exception.class })
	public void handleException(final Exception e) {
		this.lu.writeException(e);
		e.printStackTrace();
		this.fbu.checkFileBlocks();
		Printer.instance
				.print("\u5904\u7406\u8bf7\u6c42\u65f6\u53d1\u751f\u9519\u8bef\uff1a\n\r------\u4fe1\u606f------\n\r"
						+ e.getMessage() + "\n\r------\u4fe1\u606f------");
	}
}
