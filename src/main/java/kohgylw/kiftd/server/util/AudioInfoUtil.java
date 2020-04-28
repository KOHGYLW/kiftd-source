package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.*;
import java.util.*;

import javax.annotation.Resource;

import java.io.*;

@Component
public class AudioInfoUtil {

	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;
	@Resource
	private TxtCharsetGetter tcg;

	private static final String ERROR_ARTIST = "\u7fa4\u661f";
	private static final String DEFAULT_LRC = "css/audio_default.lrc";
	private static final String DEFAULT_COVER = "css/audio_default.png";

	public AudioInfoUtil() {
	}

	public AudioInfoList transformToAudioInfoList(final List<Node> nodes, final String fileId) {
		final AudioInfoList ail = new AudioInfoList();
		final List<AudioInfo> as = new ArrayList<AudioInfo>();
		int index = 0;
		for (final Node n : nodes) {
			final String suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".") + 1);
			if (suffix.equalsIgnoreCase("mp3") || suffix.equalsIgnoreCase("ogg") || suffix.equalsIgnoreCase("wav")) {
				final AudioInfo ai = new AudioInfo();
				ai.setUrl("resourceController/getResource/" + n.getFileId());
				ai.setLrc(DEFAULT_LRC);
				ai.setArtist(ERROR_ARTIST);
				ai.setCover(DEFAULT_COVER);
				this.getAudioArtistAndName(ai, n);
				this.getLrcAndCover(ai, n, nodes);
				as.add(ai);
				if (!fileId.equals(n.getFileId())) {
					continue;
				}
				index = as.size() - 1;
			}
		}
		ail.setAs(as);
		ail.setIndex(index);
		return ail;
	}

	private void getAudioArtistAndName(final AudioInfo ai, final Node n) {
		final File f = fbu.getFileFromBlocks(n);
		ai.setName(this.getFileName(n.getFileName()).trim());
		try (final RandomAccessFile raf = new RandomAccessFile(f, "r")) {
			final byte[] buf = new byte[128];
			raf.seek(raf.length() - 128L);
			raf.read(buf);
			if ("TAG".equalsIgnoreCase(new String(buf, 0, 3))) {
				final String artist = this.transformCharsetEncoding(buf, 33, 30);
				if (artist.length() > 0) {
					ai.setArtist(artist);
					if(artist.length() > 0) {
						return;
					}
				}
			}
			final byte[] buf2 = new byte[10];
			raf.seek(0L);
			raf.read(buf2);
			if ("ID3".equalsIgnoreCase(new String(buf2, 0, 3))) {
				final int length = (buf2[6] & 0x7F) * 2097152 + (buf2[7] & 0x7F) * 16384 + (buf2[8] & 0x7F) * 128
						+ (buf2[9] & 0x7F);
				final byte[] buf3 = new byte[length];
				raf.seek(10L);
				raf.read(buf3);
				int flength;
				for (int count = 0; count < length - 1; count = count + 10 + flength) {
					final String ftitle = new String(buf3, count, 4);
					flength = buf3[count + 4] * 16777216 + buf3[count + 5] * 65536 + buf3[count + 6] * 256
							+ buf3[count + 7];
					if (ftitle.equalsIgnoreCase("TPE1") && flength != 0) {
						final String artist2 = this.transformCharsetEncoding(buf3, count + 11, flength - 1);
						if (artist2.length() > 0) {
							ai.setArtist(artist2);
						}
					}
				}
			}
		} catch (Exception ex) {
		}
	}

	private void getLrcAndCover(final AudioInfo ai, final Node n, final List<Node> ns) {
		for (final Node e : ns) {
			final String suffix = e.getFileName().substring(e.getFileName().lastIndexOf(".") + 1);
			final String nName = this.getFileName(n.getFileName());
			if (this.getFileName(e.getFileName()).equals(nName) && suffix.equalsIgnoreCase("lrc")) {
				ai.setLrc("resourceController/getLRContext/" + e.getFileId());
			}
			if ((this.getFileName(e.getFileName()).equals(nName)
					|| ai.getArtist().equals(this.getFileName(e.getFileName())))
					&& (suffix.equals("jpg") || suffix.equals("jpeg") || suffix.equals("gif") || suffix.equals("bmp")
							|| suffix.equals("png"))) {
				ai.setCover("resourceController/getResource/" + e.getFileId());
			}
		}
	}

	private String getFileName(final String originName) {
		return (originName.indexOf(".") != -1) ? originName.substring(0, originName.indexOf(".")) : originName;
	}

	private String transformCharsetEncoding(final byte[] buf, final int offset, final int length) {
		try {
			return new String(buf, offset, length, tcg.getTxtCharset(buf, offset, length)).trim();
		} catch (Exception ex) {
			lu.writeException(ex);
		}
		return "";
	}
}
