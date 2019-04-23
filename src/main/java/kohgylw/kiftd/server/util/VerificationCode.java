package kohgylw.kiftd.server.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

/**
 * 
 * <h2>随机验证码封装类</h2>
 * <p>
 * 该类用于封装一个随机验证码图片及其内容。要获得验证码图片，请使用saveTo方法将其存入本地路径或是写入输出流。
 * 要获得验证码图片代表的正确内容，请使用getCode方法获得内容字符串。要获得图片对象本身，请调用getImage方法。
 * </p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class VerificationCode {

	private String code;
	private BufferedImage image;

	public String getCode() {
		return code.toLowerCase();
	}

	public void setCode(String code) {
		this.code = code;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	public void saveTo(String path) throws IOException {
		File f = new File(path);
		ImageIO.write(image, "jpeg", new FileOutputStream(f));
	}
	
	public void saveTo(OutputStream out) throws IOException {
		ImageIO.write(image, "jpeg", out);
	}

}
