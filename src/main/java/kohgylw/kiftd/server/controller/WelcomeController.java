package kohgylw.kiftd.server.controller;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
public class WelcomeController {
	@RequestMapping({ "/" })
	public String home() {
		return "redirect:/home.html";
	}

}
