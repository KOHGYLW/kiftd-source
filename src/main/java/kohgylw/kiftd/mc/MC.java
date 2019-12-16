package kohgylw.kiftd.mc;

/**
 * 
 * <h2>kiftd主类（启动类）</h2>
 * <p>
 * 该类为程序主类，内部的main方法为kiftd程序的唯一入口。您可以从这里开始逐步深入到kiftd的各个运作过程中。目前注释可能还不是很完善，
 * 未来将会逐步完善代码注释，对此带来的不便深表歉意。如有任何问题，欢迎随时与作者联系：kohgylw@163.com。
 * </p>
 * <h4>许可声明：</h4>
 * <p>
 * 使用该源代码（包括但不限于：分发、修改、编译）代表您接受以下条款：
 * </p>
 * <ul>
 * <li>任何人均可以免费获取kiftd源代码的原版拷贝，并进行分发或修改。</li>
 * <li>该源代码及其修改版本的编译结果可以运用于任何用途，无论是商业的还是非商业的。</li>
 * <li>该源代码的作者无需为使用该源代码所产生的任何后果负责。</li>
 * <li>作者对该源代码的原始版本及其编译生成的程序拥有版权。</li>
 * </ul>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */

public class MC {
	/**
	 * 
	 * <h2>主方法</h2>
	 * <p>
	 * 该方法为kiftd入口位置，即程序启动后进入的主方法，它会负责接收系统的启动传参并以不同模式启动kiftd。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param args
	 *            String[] 控制台传入参数
	 * @return void
	 */
	public static void main(final String[] args) {
		if (args == null || args.length == 0) {
			try {
				UIRunner.build();// 以界面模式启动kiftd。
			} catch (Exception e) {
				// 如果无法以图形界面启动，可能是由于开发环境差异或资源引用失败导致的，请根据此处捕获的异常进行调试。
				System.out.println(new String(
						"错误！无法以图形界面模式启动kiftd，您的操作系统可能不支持图形界面。您可以尝试使用命令模式参数“-console”来启动并开始使用kiftd。".getBytes()));
			}
		} else {
			ConsoleRunner.build(args);// 以控制台模式启动kiftd。
		}
	}
}
