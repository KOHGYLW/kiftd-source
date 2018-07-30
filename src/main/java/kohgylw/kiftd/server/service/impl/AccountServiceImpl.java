package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;

import javax.annotation.Resource;
import javax.servlet.http.*;
import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.pojo.*;

@Service
public class AccountServiceImpl implements AccountService {
	@Resource
	private KeyUtil ku;

	private static final long TIME_OUT = 30000L;
	@Resource
	private Gson gson;

	public String checkLoginRequest(final HttpServletRequest request, final HttpSession session) {
		final String encrypted = request.getParameter("encrypted");
		final String loginInfoStr = DecryptionUtil.dncryption(encrypted, ku.getPrivateKey());
		try {
			final LoginInfoPojo info = gson.fromJson(loginInfoStr, LoginInfoPojo.class);
			if (System.currentTimeMillis() - Long.parseLong(info.getTime()) > TIME_OUT) {
				return "error";
			}
			final ConfigureReader cr = ConfigureReader.instance();
			final String accountId = info.getAccountId();
			if (!cr.foundAccount(accountId)) {
				return "accountnotfound";
			}
			if (cr.checkAccountPwd(accountId, info.getAccountPwd())) {
				session.setAttribute("ACCOUNT", (Object) accountId);
				return "permitlogin";
			}
			return "accountpwderror";
		} catch (Exception e) {
			return "error";
		}
	}

	public void logout(final HttpSession session) {
		session.invalidate();
	}

	public String getPublicKey() {
		PublicKeyInfo pki = new PublicKeyInfo();
		pki.setPublicKey(ku.getPublicKey());
		pki.setTime(System.currentTimeMillis());
		return gson.toJson(pki);
	}
}
