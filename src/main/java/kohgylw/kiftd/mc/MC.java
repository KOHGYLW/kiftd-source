package kohgylw.kiftd.mc;

/**
 * 
 * <h2>kiftd主类</h2>
 * <p>该类为程序主类，其main方法为程序入口。您可以从这里开始逐步了解整个kiftd的运作过程。如有任何问题，欢迎
 * 随时与作者联系：kohgylw@163.com。</p>
 * <h4>许可声明：</h4>
 * <p>使用该源代码（包括但不限于：分发、修改、编译）代表您接受以下条款：</p>
 * <ul>
 * <li>任何人均可以免费获取kiftd源代码的原版拷贝，并进行分发或修改。</li>
 * <li>该源代码及其修改版本的编译结果可以运用于任何用途，无论是商业的还是非商业的。</li>
 * <li>该源代码的作者无需为使用该源代码所产生的任何后果负责。</li>
 * <li>使用者需承认作者对该源代码具有版权。</li>
 * </ul>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */

public class MC {
	/**
	 * 
	 * <h2>主方法</h2>
	 * <p>该方法为kiftd入口位置，负责接收命令传参并以不同模式启动kiftd。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param args String[] 控制台传入参数
	 * @return void
	 */
	public static void main(final String[] args) {
		if (args == null || args.length == 0) {
			try {
				UIRunner.build();//以界面模式启动kiftd。
			} catch (Exception e) {
				System.out.println("错误！无法以图形界面模式启动kiftd，您的操作系统可能不支持图形界面。您可以尝试使用命令模式“-console”来启动kiftd并开始使用。");
			}
		} else {
			ConsoleRunner.build(args);//以控制台模式启动kiftd。
		}
	}
}
