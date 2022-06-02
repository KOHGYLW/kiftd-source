package kohgylw.kiftd.server.pojo;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigureReader;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.progress.EncoderProgressListener;

/**
 * 
 * <h2>视频转码信息</h2>
 * <p>
 * 其中存放了视频的转码信息。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class VideoTranscodeThread {

	private String md5;
	private String progress;
	private Encoder encoder;
	private String outputFileName;

	public VideoTranscodeThread(File f, EncodingAttributes ea,ProcessLocator fl) throws Exception {
		// 首先计算MD5值
		md5 = DigestUtils.md5Hex(new FileInputStream(f));
		progress = "0.0";
		MultimediaObject mo = new MultimediaObject(f,fl);
		encoder = new Encoder(fl);
		Thread t = new Thread(() -> {
			try {
				outputFileName="video_"+UUID.randomUUID().toString()+".mp4";
				encoder.encode(mo, new File(ConfigureReader.instance().getTemporaryfilePath(), outputFileName),
						ea, new EncoderProgressListener() {
							public void progress(int arg0) {
								progress = (arg0 / 10.00) + "";
							}

							public void message(String arg0) {
							}

							public void sourceInfo(MultimediaInfo info) {
							}
						});
				progress = "FIN";
			} catch (Exception e) {
				Printer.instance.print("警告：在线转码功能出现意外错误。详细信息："+e.getMessage());
			}
		});
		t.start();
	}

	public String getMd5() {
		return md5;
	}

	public String getProgress() {
		return progress;
	}

	public String getOutputFileName() {
		return outputFileName;
	}
	
	/**
	 * 
	 * <h2>终止当前转码过程</h2>
	 * <p>执行该方法将中断正在进行的转码，并删除原有的输出文件。</p>
	 * @author 青阳龙野(kohgylw)
	 */
	public void abort() {
		if(encoder!=null) {
			encoder.abortEncoding();
		}
		File f=new File(ConfigureReader.instance().getTemporaryfilePath(),outputFileName);
		if(f.exists()) {
			f.delete();
		}
	}

}
