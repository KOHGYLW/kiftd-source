package kohgylw.kiftd.server.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * 
 * <h2>随机验证码生成工厂</h2>
 * <p>
 * 该工厂能够生成以指定字符随机生成的、带简单干扰线条的验证码图片。
 * 您需要先将本工厂实例化，之后调用next方法来获得一个随机验证码图片的封装对象。该对象包括了一个BufferedImage实例和
 * 一个代表验证码正确内容字符串。创建方法及各项参数意义请见构造器注释。
 * </p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.verification_code.factory.VerificationCode
 */
public class VerificationCodeFactory {

	private char[] alternative;
	private static final Random RANDOM = new Random();
	private int width;
	private int height;
	private int maxLine;
	private int maxOval;
	private int charSize;
	
	/**
	 * 
	 * <h2>验证码生成工厂构造器</h2>
	 * <p>
	 * 按照传入参数创建指定验证码风格生成工厂，该工厂负责生成带干扰内容的随机验证码图片并封装。
	 * 请在创建本工厂实例后调用next方法获得一个随机验证码封装对象，详见next方法注释。
	 * </p>
	 * @author 青阳龙野(kohgylw)
	 * @param charSize int 设定验证码中的字体大小，图片体积将随之改变以确保显示完全
	 * @param maxLine int 设定干扰线的数量，如果设定为0则不产生干扰线
	 * @param maxOval int 设定干扰椭圆数量，效果同干扰线
	 * @param alternative char... 设定待选字符，这些字符将随机出现在验证码中
	 */
	public VerificationCodeFactory(int charSize,int maxLine,int maxOval,char... alternative) {
		// TODO 自动生成的构造函数存根
		if (alternative == null || alternative.length == 0 || charSize <=0 || maxLine <0 || maxOval <0) {
			throw new IllegalArgumentException("验证码工厂：参数有误，字体大小必须大于0，最大行数和最大椭圆数必须大于等于0，至少提供一个候选字符。");
		} else {
			this.alternative = alternative;
			this.charSize=charSize;
			this.maxLine=maxLine;
			this.maxOval=maxOval;
			this.height=charSize+10;
		}
	}
	
	/**
	 * 
	 * <h2>生成一个新的随机验证码对象</h2>
	 * <p>根据指定验证码长度创建一个新的验证码对象并返回。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param length int 验证码长度
	 * @return kohgylw.verification_code.factory.VerificationCode 验证码封装对象，使用方法详见注释
	 */
	public VerificationCode next(int length) {
		if(length<=0) {
			throw new IllegalArgumentException("验证码工厂：length必须大于0，但是传入length="+length);
		}
		StringBuffer codeBuffer = new StringBuffer();
		VerificationCode result = new VerificationCode();
		//确保能装下，设定长度
		width=(length+1)*charSize;
		for (int i = 0; i < length; i++) {
			codeBuffer.append(alternative[RANDOM.nextInt(alternative.length)]);
		}
		result.setCode(codeBuffer.toString());
		//准备一张图片并获取画布
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();
		//先绘制底色
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, width, height);
		//再画个边框
		graphics.setColor(Color.BLACK);
		graphics.drawRect(0, 0, width-1, height-1);
		//接下来该画点乱七八糟的背景了
		for(int i=0;i<maxLine;i++) {
			//为每条线随机生成一个颜色，不能太浅了
			graphics.setColor(getRandomColor());
			//随机一个线条粗细，防止脚本根据线条粗细轻易识别出哪些是干扰线
			graphics.setStroke(new BasicStroke(RANDOM.nextInt(charSize)/2+1));
			//随机绘图位置
			int start_x=RANDOM.nextInt(width);
			int start_y=RANDOM.nextInt(height);
			int end_x=RANDOM.nextInt(width);
			int end_y=RANDOM.nextInt(height);
			graphics.drawLine(start_x, start_y, end_x, end_y);
		}
		for(int i=0;i<maxOval;i++) {
			//为每条线随机生成一个颜色，不能太浅了
			graphics.setColor(getRandomColor());
			graphics.setStroke(new BasicStroke(RANDOM.nextInt(charSize)/2+1));
			int start_x=RANDOM.nextInt(width);
			int start_y=RANDOM.nextInt(height);
			int end_x=RANDOM.nextInt(width);
			int end_y=RANDOM.nextInt(height);
			graphics.drawOval(start_x, start_y, end_x, end_y);
		}
		//最后，把验证码内容加上去
		//设置统一字体
		Font font = new Font("songti", Font.BOLD, charSize);
		//生成若干个字
		for(int i=0;i<codeBuffer.length();i++) {
			//再给每个字也生成一个颜色
			graphics.setColor(getRandomColor());
			//稍微转一个小角度
			graphics.setFont(font.deriveFont(AffineTransform.getRotateInstance(Math.toRadians(RANDOM.nextInt(90)),0,-charSize/2)));
			graphics.drawString(String.valueOf(codeBuffer.charAt(i)), (i+1)*charSize, charSize);
		}
		//存入
		result.setImage(image);
		return result;
	}
	
	//生成一个比较好辨识的随机颜色
	private static Color getRandomColor() {
		int r=RANDOM.nextInt(255);
		int g=RANDOM.nextInt(255);
		int b=RANDOM.nextInt(255);
		//避免颜色太浅
		while(r>200&&g>200&&b>200) {
			r=RANDOM.nextInt(255);
			g=RANDOM.nextInt(255);
			b=RANDOM.nextInt(255);
		}
		return new Color(r, g, b);
	}

}
