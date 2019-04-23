package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.*;
import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.pojo.*;

@Service
public class AccountServiceImpl implements AccountService {
	@Resource
	private KeyUtil ku;

	// 登录密钥有效期
	private static final long TIME_OUT = 30000L;

	@Resource
	private Gson gson;

	// 验证码生成工厂，包含了一些不太容易误认的字符
	private VerificationCodeFactory vcf = new VerificationCodeFactory(45, 6, 2, 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'w', 'x', 'y', 'z', '2', '3', '4', '5', '6', '7', '8', '9',
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'W', 'X', 'Y', 'Z');

	// 关注账户，当任意一个账户登录失败后将加入至该集合中，登录成功则移除。登录集合中的账户必须进行验证码验证
	private static final Set<String> focusAccount = new HashSet<>();

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
			// 如果该账户已被关注，则要求提供验证码
			synchronized (focusAccount) {
				if (focusAccount.contains(accountId)) {
					String reqVerCode = request.getParameter("vercode");
					String trueVerCode = (String) session.getAttribute("VERCODE");
					session.removeAttribute("VERCODE");//确保一个验证码只会生效一次，无论对错
					if (reqVerCode == null || trueVerCode == null || !trueVerCode.equals(reqVerCode.toLowerCase())) {
						return "needsubmitvercode";
					}
				}
			}
			if (cr.checkAccountPwd(accountId, info.getAccountPwd())) {
				session.setAttribute("ACCOUNT", (Object) accountId);
				// 如果该账户输入正确且是一个被关注的账户，则解除该账户的关注，释放空间
				synchronized (focusAccount) {
					focusAccount.remove(accountId);
				}
				return "permitlogin";
			}
			// 如果账户密码不匹配，则将该账户加入到关注账户集合，避免对方进一步破解
			synchronized (focusAccount) {
				focusAccount.add(accountId);
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

	@Override
	public void getNewLoginVerCode(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		VerificationCode vc = vcf.next(4);
		session.setAttribute("VERCODE", vc.getCode());
		try {
			response.setContentType("image/png");
			OutputStream out = response.getOutputStream();
			vc.saveTo(out);
			out.flush();
			out.close();
		} catch (IOException e) {
			try {
				response.sendError(404);
			} catch (IOException e1) {

			}
		}

	}
}
