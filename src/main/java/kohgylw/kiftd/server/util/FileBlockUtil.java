package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import javax.servlet.http.*;
import org.springframework.web.multipart.*;
import java.io.*;
import kohgylw.kiftd.server.model.*;
import java.util.*;
import org.zeroturnaround.zip.*;

@Component
public class FileBlockUtil {
	@Resource
	private NodeMapper fm;

	public String saveToFileBlocks(final HttpServletRequest request, final MultipartFile f) {
		final String fileBlocks = ConfigureReader.instance().getFileBlockPath();
		final String id = UUID.randomUUID().toString().replace("-", "");
		final String path = "file_" + id + ".block";
		final File file = new File(fileBlocks, path);
		try {
			f.transferTo(file);
			return path;
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR";
		}
	}

	public String getFileSize(final MultipartFile f) {
		final long size = f.getSize();
		final int mb = (int) (size / 1024L / 1024L);
		return "" + mb;
	}

	public boolean deleteFromFileBlocks(final String fileblocks, final String path) {
		final File file = new File(fileblocks, path);
		return file.exists() && file.isFile() && file.delete();
	}

	public File getFileFromBlocks(final String fileBlocks, final String path) {
		final File file = new File(fileBlocks, path);
		if (file.exists() && file.isFile()) {
			return file;
		}
		return null;
	}

	public void checkFileBlocks() {
		String fileblocks = ConfigureReader.instance().getFileBlockPath();
		Thread checkThread = new Thread(() -> {
			List<Node> nodes = fm.queryAll();
			for (Node node : nodes) {
				File block = new File(fileblocks, node.getFilePath());
				if (!block.exists()) {
					fm.deleteById(node.getFileId());
				}
			}
			File blocks = new File(fileblocks);
			String[] bn = blocks.list();
			for (String n : bn) {
				Node node = fm.queryByPath(n);
				if (node == null) {
					File f = new File(fileblocks, n);
					f.delete();
				}
			}
		});
		checkThread.start();
	}

	public String createZip(final List<String> idList, final String tfPath, final String fileBlocks) {
		final String zipname = "tf_" + UUID.randomUUID().toString() + ".zip";
		final File f = new File(tfPath, zipname);
		try {
			final ZipEntrySource[] zs = new ZipEntrySource[idList.size()];
			for (int i = 0; i < idList.size(); ++i) {
				final Node node = this.fm.queryById(idList.get(i));
				if (node != null) {
					zs[i] = (ZipEntrySource) new FileSource(node.getFileName(),
							new File(fileBlocks, node.getFilePath()));
				}
			}
			ZipUtil.pack(zs, f);
			return zipname;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
