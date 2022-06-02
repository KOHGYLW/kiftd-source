package kohgylw.kiftd.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import kohgylw.kiftd.printer.Printer;
import ws.schild.jave.Version;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.FFMPEGProcess;

@Component
public class KiftdFFMPEGLocator implements ProcessLocator {

	@Resource
	private LogUtil lu;

	private boolean enableFFmpeg;

	private String suffix;

	private String arch;

	private File dirFolder;

	private boolean isWindows;

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
		// 实例化过程，初始化一些系统相关的变量
		// 下面的变量用于判断操作系统，主要判断是Windows还是Mac，都不是的话就一律视作是各种Linux的发行版
		String os = System.getProperty("os.name").toLowerCase();
		isWindows = os.contains("windows");
		boolean isMac = os.contains("mac");

		dirFolder = new File(System.getProperty("java.io.tmpdir"), "jave/");
		if (!dirFolder.exists()) {
			dirFolder.mkdirs();
		}

		suffix = isWindows ? ".exe" : (isMac ? "-osx" : "");
		arch = System.getProperty("os.arch");
		// 服务器启动时第一次初始化ffmpeg执行路径
		initFFMPEGExecutablePath();// 这里的目的在于启动服务器时预拷贝ffmpeg，
		// 避免第一次访问文件夹视图时再拷贝造成延迟，同时初始化enableFFmpeg属性。
	}

	@Override
	public String getExecutablePath() {
		// 每次获得路径时再次初始化ffmpeg执行路径
		return initFFMPEGExecutablePath();// 这里的目的在于避免运行中ffmpeg被删掉从而导致程序找不到它，同时更新enableFFmpeg属性。
	}

	// 初始化ffmpeg执行路径并返回，过程包括：
	// 1，判断正确的程序版本；2，根据版本判断程序是否就位；3，如果未就位则将程序拷贝到临时文件夹中；4，返回正确的程序路径或null。
	private String initFFMPEGExecutablePath() {
		// 首先判断是否启用了在线解码功能，若未启用则无需初始化ffmpeg执行路径
		if (!ConfigureReader.instance().isEnableFFMPEG()) {
			enableFFmpeg = false;
			return null;
		}
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
					lu.writeException(e);
					enableFFmpeg = false;
					return null;
				}
				// 已经有了？那么它应该准备好了
			}
		} else {
			// 否则，使用内置的ffmpeg文件。
			// 临时文件中是否已经拷贝好了ffmpeg可执行文件了？
			ffmpegFile = new File(dirFolder, "ffmpeg-" + arch + "-" + Version.getVersion() + suffix);
			if (!ffmpegFile.exists()) {
				// 没有？那将自带的、对应操作系统的ffmpeg文件拷贝到临时目录中，如果没有对应自带的ffmpeg，那么会抛出异常
				// 如果抛出异常，那么直接结束构造
				try {
					copyFile("ffmpeg-" + arch + suffix, ffmpegFile);
				} catch (NullPointerException e) {
					Printer.instance.print("警告：未能找到适合此操作系统的ffmpeg引擎可执行文件，视频播放的在线解码功能将不可用。");
					lu.writeException(e);
					enableFFmpeg = false;
					return null;
				}
			}
			// 已经有了？那么它应该准备好了
		}
		// 对于类Unix系统而言，还要确保临时目录授予可运行权限，以便jave运行时调用ffmpeg
		if (!isWindows) {
			if (!ffmpegFile.canExecute()) {
				try {
					Runtime.getRuntime().exec(new String[] { "/bin/chmod", "755", ffmpegFile.getAbsolutePath() });
				} catch (IOException e) {
					// 授予权限失败的话……好像也没啥好办法
					lu.writeException(e);
					enableFFmpeg = false;
					return null;
				}
			}
		}
		// 上述工作都做好了，就可以将ffmpeg的路径返回给jave调用了。
		// 如果到不了这里，说明初始化失败，该方法返回null，那么应该禁用jave的在线转码功能
		enableFFmpeg = true;
		return ffmpegFile.getAbsolutePath();
	}

	// 把文件从自带的jar包中拷贝出来，移入指定文件夹
	private void copyFile(String path, File dest) {
		String resourceName = "nativebin/" + path;
		try {
			InputStream is = getClass().getResourceAsStream(resourceName);
			if (is == null) {
				resourceName = "ws/schild/jave/nativebin/" + path;
				is = ClassLoader.getSystemResourceAsStream(resourceName);
			}
			if (is == null) {
				resourceName = "ws/schild/jave/nativebin/" + path;
				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
				is = classloader.getResourceAsStream(resourceName);
			}
			if (is != null) {
				copy(is, dest.getAbsolutePath());
				try {
					is.close();
				} catch (IOException ioex) {
					lu.writeException(ioex);
				}
			}
		} catch (NullPointerException ex) {
			lu.writeException(ex);
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

	public boolean isEnableFFmpeg() {
		return enableFFmpeg;
	}

	@Override
	public ProcessWrapper createExecutor() {
		return new FFMPEGProcess(getExecutablePath());
	}
}
