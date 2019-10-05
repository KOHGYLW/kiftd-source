package kohgylw.kiftd.server.util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * 
 * <h2>AES加密器</h2>
 * <p>该加密器负责对字符串进行加密、解密和随机密码生产操作，详见其中的各个方法注释。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class AESCipher {

	private static final String CIPHER_TYPE = "AES";//所用到的加密算法类型
	private Base64.Encoder encoder;
	private Base64.Decoder decoder;
	
	public AESCipher() {
		encoder = Base64.getEncoder();
		decoder = Base64.getDecoder();
	}
	
	/**
	 * 
	 * <h2>生成随机AES密钥</h2>
	 * <p>该方法将生成一个随机的AES密钥，并转化为Base64字符串返回。</p>
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang.String 随机生成的密钥，以Base64字符串输出
	 */
	public String generateRandomKey() throws NoSuchAlgorithmException {
		KeyGenerator kg = KeyGenerator.getInstance(CIPHER_TYPE);
		kg.init(128);
		return encoder.encodeToString(kg.generateKey().getEncoded());
	}
	
	/**
	 * 
	 * <h2>对字符串进行AES加密</h2>
	 * <p>该方法用于对传入的字符串进行加密，并以Base64的形式返回加密后的密文。加密用到的密钥也需以Base64的形式传入。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param base64Key java.lang.String 加密密钥，必须以Base64形式传入
	 * @param content java.lang.String 需要加密的内容
	 * @return java.lang.String 加密后的密文，以Base64的形式返回
	 */
	public String encrypt(String base64Key, String content) throws Exception {
		SecretKey key = new SecretKeySpec(decoder.decode(base64Key), CIPHER_TYPE);
		Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return encoder.encodeToString(cipher.doFinal(content.getBytes(StandardCharsets.UTF_8)));
	}
	
	/**
	 * 
	 * <h2>对密文进行AES解密</h2>
	 * <p>该方法用于对传入的Base64形式的密文进行解密。解密用到的密钥也需以Base64的形式传入。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param base64Key java.lang.String 解密密钥，必须以Base64形式传入
	 * @param ciphertext java.lang.String 需要解密的密文，必须以Base64形式传入
	 * @return java.lang.String 解密后的内容
	 */
	public String decrypt(String base64Key, String ciphertext) throws Exception {
		SecretKey key = new SecretKeySpec(decoder.decode(base64Key), CIPHER_TYPE);
		Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return new String(cipher.doFinal(decoder.decode(ciphertext)),StandardCharsets.UTF_8);
	}

}
