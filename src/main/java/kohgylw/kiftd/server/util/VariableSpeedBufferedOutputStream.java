package kohgylw.kiftd.server.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpSession;

/**
 * 
 * <h2>带限流作用的缓存输出流</h2>
 * <p>
 * 该工具是对普通缓存输出流BufferedOutputStream的升级，能够将输出速度限制在指定速率之内，便于系统管理输出带宽。
 * </p>
 * <p>
 * 特别提示：该类中仅有write(byte[] b, int off, int len)方法具备限速功能，其余方法则不具备。因此，如果您希望控制输出速率，
 * 请使用（且仅使用）该方法实现输出操作。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class VariableSpeedBufferedOutputStream extends BufferedOutputStream {

	private long maxRate;// 该实例的最大输出限速，以KB/s为单位。
	private HttpSession session;// 该实例所用的用户会话对象，用于线程锁。

	/**
	 * 
	 * <h2>创建一个限速缓存输出流实例</h2>
	 * <p>
	 * 请使用该方法构造一个实例，然后开始使用。必须按照参数说明给定正确的参数以确保该实例能够正常发挥作用。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param out
	 *            java.io.OutputStream 原始输出流，传入方法与普通的BufferedOutputStream构造器相同
	 * @param maxRate
	 *            long 每秒最大可输出的数据数量，以KB为单位。例如：传入100就代表该实例的最大输出限速为100 KB/s
	 * @param session
	 *            javax.servlet.http.HttpSession 传入用户会话。该对象用于锁定输出操作，从而确保当用户开启多个
	 *            下载任务时，总的最大下载速率仍不会超过限速值
	 */
	public VariableSpeedBufferedOutputStream(OutputStream out, long maxRate, HttpSession session) {
		super(out);
		this.maxRate = maxRate;
		this.session = session;
	}

	/**
	 * 
	 * <h2>升级的write(byte[] b, int off, int len)方法</h2>
	 * <p>
	 * 该方法将按照 <strong>构造器中传入的最大输出速率限制</strong>
	 * 输出数据。使用方法与原BufferedOutputStream类中定义的方法完全相同。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param b
	 *            byte[] 数据数组
	 * @param off
	 *            int 数据的起始下标
	 * @param len
	 *            int 数据在数组中的长度
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		if (maxRate > 0) {
			// 如果限速值为正数，那么进行限速输出，输出时要求独占Session对象以确保和其它下载任务共享最大限速。
			synchronized (session) {
				// 记录写出前的纳秒值
				long startNano = System.nanoTime();
				super.write(b, off, len);// 执行写出……
				// 记录写出后的纳秒值
				long endNano = System.nanoTime();
				int n = len - off;// 计算本次写出的数据长度（byte数）
				if (n > 0) {// 确实写出了数据？
					long consumeNano = endNano - startNano;// 记录写出的耗时（纳秒）
					long shouldNano = 976562L * (long)((double) n / maxRate);// 计算最大应耗时（纳秒）
					if (consumeNano < shouldNano) {// 如果实际耗时小于应耗时，则代表写出过快，应进行延迟
						// 计算还需要再延迟的毫秒数
						long shouldSleep = (shouldNano - consumeNano) / 1000000;
						if (shouldSleep > 0) {// 延迟毫秒数大于0？
							try {
								Thread.sleep(shouldSleep);// 执行延迟
							} catch (InterruptedException e) {
								// 如果收到中断指令，那么就响应中断
								Thread.currentThread().interrupt();
							}
						}
					}
				}
			}
		} else if (maxRate < 0) {// 如果限速值为负数，则不限速输出
			super.write(b, off, len);
		} else {
			// 如果限速值为0，那肯定是限速设置有误造成的。
			throw new IllegalArgumentException("Error:invalid maximum download rate value.");
		}
	}

}
