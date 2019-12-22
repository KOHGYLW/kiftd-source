package kohgylw.kiftd.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

import kohgylw.kiftd.printer.Printer;
import ws.schild.jave.FFMPEGLocator;

@Component
public class KiftdFFMPEGLocator extends FFMPEGLocator {
	
	/**
	 * 内置的ffmpeg引擎的版本号，应该与jave整合资源本身自带的ffmpeg引擎版本对应
	 */
	private static final String MY_EXE_VERSION = "2.5.0";

	/**
	 * ffmpeg引擎的路径
	 */
	private String path;

	/**
	 * 
	 * <h2>实例化ffmpeg引擎构造器</h2>
	 * <p>
	 * 在实例化过程中，将根据操作系统来判断使用哪一种ffmpeg引擎可执行文件来进行引用，并将其拷贝到临时目录中以便转码使用。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public KiftdFFMPEGLocator() {
		// 首先检查是否启用了在线解码功能，如果没有启用则无需初始化ffmpeg引擎
		if (!ConfigureReader.instance().isEnableFFMPEG()) {
			return;
		}
		// 下面的变量用于判断操作系统，主要判断是Windows还是Mac，都不是的话就一律视作是各种Linux的发行版
		String os = System.getProperty("os.name").toLowerCase();
		boolean isWindows = os.contains("windows");
		boolean isMac = os.contains("mac");

		// 为了保证运行过程中ffmpeg可执行文件能被单独使用，首先要构造一个临时目录，用于存放运行中引用的ffmpeg可执行文件
		File dirFolder = new File(System.getProperty("java.io.tmpdir"), "jave/");
		if (!dirFolder.exists()) {
			dirFolder.mkdirs();
		}

		// 判断正确的后缀名，如果是windows，那么则使用“.exe”作为后缀，然后再判断是不是Mac，如果是则查找“-osx”后缀的ffmpeg，
		// 其余的不需要特定后缀。
		String suffix = isWindows ? ".exe" : (isMac ? "-osx" : "");
		String arch = System.getProperty("os.arch");

		// 是否在程序主目录下放置了自定义的ffmpeg可执行文件“ffmpeg.exe”/“ffmpeg”？
		File ffmpegFile;
		File customFFMPEGexef = new File(ConfigureReader.instance().getPath(), isWindows ? "ffmpeg.exe" : "ffmpeg");
		// 如果有，那么优先使用自定义的ffmpeg可执行文件。
		if (customFFMPEGexef.isFile() && customFFMPEGexef.canRead()) {
			ffmpegFile = new File(dirFolder, customFFMPEGexef.getName());
			// 临时文件中是否已经拷贝好了ffmpeg可执行文件了？
			if (!ffmpegFile.exists()) {
				// 没有？那将自定义的ffmpeg文件拷贝到临时目录中。
				try {
					Files.copy(customFFMPEGexef.toPath(), ffmpegFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Printer.instance.print("警告：自定义的ffmpeg引擎可执行文件无法读取，视频播放的在线解码功能将不可用。");
					return;
				}
				// 已经有了？那么它应该准备好了
			}
		} else {
			// 否则，使用内置的ffmpeg文件。
			// 临时文件中是否已经拷贝好了ffmpeg可执行文件了？
			ffmpegFile = new File(dirFolder, "ffmpeg-" + arch + "-" + MY_EXE_VERSION + suffix);
			if (!ffmpegFile.exists()) {
				// 没有？那将自带的、对应操作系统的ffmpeg文件拷贝到临时目录中，如果没有对应自带的ffmpeg，那么会抛出异常
				// 如果抛出异常，那么直接结束构造
				try {
					copyFile("ffmpeg-" + arch + suffix, ffmpegFile);
				} catch (NullPointerException e) {
					Printer.instance.print("警告：未能找到适合此操作系统的ffmpeg引擎可执行文件，视频播放的在线解码功能将不可用。");
					return;
				}
				// 已经有了？那么它应该准备好了
			}
		}

		// 对于类Unix系统而言，还要确保临时目录授予可运行权限，以便jave运行时调用ffmpeg
		if (!isWindows) {
			if (!ffmpegFile.canExecute()) {
				try {
					Runtime.getRuntime().exec(new String[] { "/bin/chmod", "755", ffmpegFile.getAbsolutePath() });
				} catch (IOException e) {
					// 授予权限失败的话……好像也没啥好办法，直接结束构造就行了
					return;
				}
			}
		}

		// 上述工作都做好了，就可以将ffmpeg的路径返回给jave调用了。
		// 如果到不了这里，说明构造失败，path字段会是null，那么应该禁用jave的在线转码功能
		path = ffmpegFile.getAbsolutePath();
	}

	@Override
	public String getFFMPEGExecutablePath() {
		return path;
	}

	// 把文件从自带的jar包中拷贝出来，移入指定文件夹
	private void copyFile(String path, File dest) {
		String resourceName = "/ws/schild/jave/native/" + path;
		try {
			if (!copy(getClass().getResourceAsStream(resourceName), dest.getAbsolutePath())) {
				throw new NullPointerException();
			}
		} catch (NullPointerException ex) {
			throw ex;
		}
	}

	// 以文件的形式把流存入指定文件夹内
	private boolean copy(InputStream source, String destination) {
		boolean success = true;
		try {
			Files.copy(source, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			success = false;
		}
		return success;
	}
	
}
