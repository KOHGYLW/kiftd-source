package kohgylw.kiftd.ui.module;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.ui.pojo.FileSystemPath;
import kohgylw.kiftd.ui.util.PathsTable;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;

/**
 * 
 * <h2>管理文件系统路径窗口</h2>
 * <p>
 * 该窗口用于提供用户管理文件系统路径的界面功能，包括修改主文件系统路径和新增、删除、修改扩展存储区的功能。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FileSystemPathViewer extends KiftdDynamicWindow {

	protected static JDialog window;// 窗体对象
	private JButton addBtn;// 添加扩展存储路径按钮
	private JButton changeBtn;// 修改按钮
	private JButton removeBtn;// 删除按钮
	private PathsTable pathsTable;// 文件列表对象
	private int maxExtendStoresNum;// 最大扩展存储区数目

	private static FileSystemPathViewer fsv;// 该窗口的唯一实例
	private static List<FileSystemPath> paths;// 当前显示的视图
	private static ExecutorService worker;// 操作线程池
	private CharsetEncoder encoder;// ISO-8859-1编码器

	// 错误提示信息
	private static final String INVALID_PATH_ALTER = "错误：该路径中含有程序无法识别的字符，请使用其他路径（推荐使用纯英文路径）。";

	// 资源加载
	private FileSystemPathViewer() {
		encoder = Charset.forName("ISO-8859-1").newEncoder();
		setUIFont();
		worker = Executors.newSingleThreadExecutor();
		(window = new JDialog(SettingWindow.window, "管理文件系统路径")).setModal(true);
		window.setSize(600, 240);
		window.setDefaultCloseOperation(1);
		window.setLocation(200, 200);
		window.setResizable(false);
		paths = new ArrayList<>();
		Container c = window.getContentPane();
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		addBtn = new JButton("新建 扩展存储区[Add]");
		changeBtn = new JButton("修改路径[Change]");
		removeBtn = new JButton("移除路径[Remove]");
		addBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		changeBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		changeBtn.setEnabled(false);
		removeBtn.setPreferredSize(new Dimension((int) (105 * proportion), (int) (35 * proportion)));
		removeBtn.setEnabled(false);
		toolBar.add(addBtn);
		toolBar.addSeparator();
		toolBar.add(changeBtn);
		toolBar.add(removeBtn);
		toolBar.addSeparator();
		c.add(toolBar, BorderLayout.NORTH);
		// 各个工具栏按钮的功能实现
		maxExtendStoresNum = SettingWindow.st == null ? 0 : SettingWindow.st.getMaxExtendStoresNum();
		addBtn.addActionListener((e) -> {
			disableAllButtons();
			if (SettingWindow.extendStores.size() < maxExtendStoresNum) {
				JFileChooser addExtendStoresChooer = new JFileChooser();
				addExtendStoresChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				addExtendStoresChooer.setPreferredSize(fileChooerSize);
				addExtendStoresChooer.setDialogTitle("请选择存储路径...");
				if (addExtendStoresChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					File newExtendStores = addExtendStoresChooer.getSelectedFile();
					if (newExtendStores.isDirectory() && newExtendStores.canRead() && newExtendStores.canWrite()) {
						if (SettingWindow.extendStores.parallelStream()
								.anyMatch(f -> f.getPath().equals(newExtendStores))) {
							JOptionPane.showMessageDialog(window, "错误：该路径已被其他扩展存储区占用。", "错误",
									JOptionPane.WARNING_MESSAGE);
						} else {
							String pathName = newExtendStores.getAbsolutePath();
							if (encoder.canEncode(pathName) && pathName.indexOf("\\:") < 0
									&& pathName.indexOf("\\\\") < 0) {
								Short[] indexs = SettingWindow.extendStores.parallelStream().map((s) -> s.getIndex())
										.toArray(Short[]::new);
								short index = 1;
								while (Arrays.binarySearch(indexs, index) >= 0) {
									index++;
								}
								FileSystemPath nfsp = new FileSystemPath();
								nfsp.setIndex(index);
								nfsp.setType(FileSystemPath.EXTEND_STORES_NAME);
								nfsp.setPath(addExtendStoresChooer.getSelectedFile());
								SettingWindow.extendStores.add(nfsp);
							} else {
								JOptionPane.showMessageDialog(window, INVALID_PATH_ALTER, "错误",
										JOptionPane.WARNING_MESSAGE);
							}
						}
					} else {
						JOptionPane.showMessageDialog(window, "错误：该路径不可用，必须选择可读写的文件夹。", "错误",
								JOptionPane.WARNING_MESSAGE);
					}
				}
			}
			enableAllButtons();
			refresh();
		});
		changeBtn.addActionListener((e) -> {
			disableAllButtons();
			if (JOptionPane.showConfirmDialog(window,
					"确认要修改该存储区路径么？警告：如需保留该存储区内的全部数据，应先将该存储区原路径指定的文件夹移动到新位置，再将移动后的文件夹设置为该存储区的新路径。否则，该存储区内的数据将全部丢失。",
					"修改路径", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				short index = pathsTable.getSelectFileSystemIndex();
				if (index == 0) {
					JFileChooser mainFileSystemPathChooer = new JFileChooser();
					mainFileSystemPathChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					mainFileSystemPathChooer.setPreferredSize(fileChooerSize);
					if (SettingWindow.st != null) {
						File fileSystemPath = new File(SettingWindow.st.getFileSystemPath());
						if (fileSystemPath.isDirectory()) {
							mainFileSystemPathChooer.setCurrentDirectory(fileSystemPath);
						}
					}
					mainFileSystemPathChooer.setDialogTitle("请选择主文件系统存储路径");
					if (mainFileSystemPathChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						File selectPath = mainFileSystemPathChooer.getSelectedFile();
						if (selectPath.isDirectory() && selectPath.canWrite() && selectPath.canRead()) {
							if (!SettingWindow.extendStores.parallelStream()
									.anyMatch(f -> f.getPath().equals(selectPath))) {
								String pathName = selectPath.getAbsolutePath();
								if (new File(ConfigureReader.instance().getInitFileSystemPath()).equals(selectPath)
										|| (encoder.canEncode(pathName) && pathName.indexOf("\\:") < 0
												&& pathName.indexOf("\\\\") < 0)) {
									SettingWindow.chooserPath = mainFileSystemPathChooer.getSelectedFile();
								} else {
									JOptionPane.showMessageDialog(window, INVALID_PATH_ALTER, "错误",
											JOptionPane.WARNING_MESSAGE);
								}
							} else {
								JOptionPane.showMessageDialog(window, "错误：该路径已被某个扩展存储区占用。", "错误",
										JOptionPane.WARNING_MESSAGE);
							}
						} else {
							JOptionPane.showMessageDialog(window, "错误：该路径不可用，必须选择可读写的文件夹。", "错误",
									JOptionPane.WARNING_MESSAGE);
						}
					}
				} else {
					JFileChooser mainFileSystemPathChooer = new JFileChooser();
					mainFileSystemPathChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					mainFileSystemPathChooer.setPreferredSize(fileChooerSize);
					FileSystemPath fsp = null;
					for (int i = 0; i < SettingWindow.extendStores.size(); i++) {
						if (SettingWindow.extendStores.get(i).getIndex() == index) {
							fsp = SettingWindow.extendStores.get(i);
							mainFileSystemPathChooer.setCurrentDirectory(fsp.getPath());
							mainFileSystemPathChooer.setDialogTitle("请选择扩展存储区路径");
							if (mainFileSystemPathChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
								disableAllButtons();
								File selectPath = mainFileSystemPathChooer.getSelectedFile();
								if (selectPath.isDirectory() && selectPath.canWrite() && selectPath.canRead()) {
									if (fsp.getPath().equals(selectPath) || !SettingWindow.extendStores.parallelStream()
											.anyMatch(f -> f.getPath().equals(selectPath))) {
										String pathName = selectPath.getAbsolutePath();
										if (encoder.canEncode(pathName) && pathName.indexOf("\\:") < 0
												&& pathName.indexOf("\\\\") < 0) {
											fsp.setPath(mainFileSystemPathChooer.getSelectedFile());
										} else {
											JOptionPane.showMessageDialog(window, INVALID_PATH_ALTER, "错误",
													JOptionPane.WARNING_MESSAGE);
										}
									} else {
										JOptionPane.showMessageDialog(window, "错误：该路径已被其他扩展存储区占用。", "错误",
												JOptionPane.WARNING_MESSAGE);
									}
								} else {
									JOptionPane.showMessageDialog(window, "错误：该路径不可用，必须选择可读写的文件夹。", "错误",
											JOptionPane.WARNING_MESSAGE);
								}
							}
							break;
						}
					}
				}
			}
			enableAllButtons();
			refresh();
		});
		removeBtn.addActionListener((e) -> {
			disableAllButtons();
			if (JOptionPane.showConfirmDialog(window, "确认要移除该扩展存储区么？警告：移除后，该存储区内原先存放的数据将丢失，且设置生效后不可恢复。", "移除扩展存储区",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				short index = pathsTable.getSelectFileSystemIndex();
				for (int i = 0; i < SettingWindow.extendStores.size(); i++) {
					if (SettingWindow.extendStores.get(i).getIndex() == index) {
						final int removeItemIndex = i;// 待删除的扩展存储区索引号
						// 检查该存储区内是否存有数据
						try {
							long total = FileSystemManager.getInstance().getTotalOfNodesAtExtendStore(index);
							if (total > 0) {
								// 如果已存有数据，则询问是否需要移出
								switch (JOptionPane.showConfirmDialog(window,
										"是否立即将该扩展存储区内的数据全部移出以便留档？如果您确定要移除该扩展存储区，推荐执行该操作。注意：该操作即时生效，无论是否应用新设置均无法回退。",
										"移出", JOptionPane.YES_NO_CANCEL_OPTION)) {
								case 0:
									// 是：先执行移出操作
									JFileChooser transferDirChooer = new JFileChooser();
									transferDirChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
									transferDirChooer.setPreferredSize(fileChooerSize);
									transferDirChooer.setDialogTitle("请选择移出数据的保存路径...");
									if (transferDirChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
										File transferDir = transferDirChooer.getSelectedFile();
										worker.execute(() -> {
											FSProgressDialog fsd = FSProgressDialog.getNewInstance(window);
											Thread t = new Thread(() -> {
												fsd.show();
											});
											t.start();
											try {
												boolean r = FileSystemManager.getInstance().transferExtendStore(index,
														transferDir);
												SwingUtilities.invokeLater(() -> {
													fsd.close();
													if (r) {
														// 若所有数据移出成功，则移除指定存储区并结束流程
														SettingWindow.extendStores.remove(removeItemIndex);
														refresh();// 这里单独刷新一次是因为此部分逻辑可能会稍后才执行
													} else {
														// 若出现错误，则进行提示
														JOptionPane.showMessageDialog(window,
																"移出文件时失败，该操作已被中断，未能移出全部数据。", "错误",
																JOptionPane.ERROR_MESSAGE);
													}
												});
											} catch (FileAlreadyExistsException e1) {
												SwingUtilities.invokeLater(() -> {
													fsd.close();
													JOptionPane.showMessageDialog(window,
															"目标文件夹内存在同名文件，建议选择一个空文件夹作为移出数据的保存路径。", "错误",
															JOptionPane.ERROR_MESSAGE);
												});
											} catch (Exception e1) {
												SwingUtilities.invokeLater(() -> {
													fsd.close();
													JOptionPane.showMessageDialog(window,
															"出现意外错误，无法统计扩展存储区数据，请重启应用后重试。", "错误",
															JOptionPane.ERROR_MESSAGE);
												});

											}
										});
									}
									break;
								case 1:
									// 否：直接移除指定存储区
									SettingWindow.extendStores.remove(i);
									break;
								case 2:
								default:
									break;
								}
							} else {
								SettingWindow.extendStores.remove(i);
							}
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(window, "出现意外错误，无法统计扩展存储区数据，请重启应用后重试。", "错误",
									JOptionPane.ERROR_MESSAGE);
						}
						break;
					}
				}
			}
			enableAllButtons();
			refresh();

		});
		// 生成文件列表
		pathsTable = new PathsTable();
		pathsTable.setRowHeight((int) (16 * proportion));
		pathsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane mianPane = new JScrollPane(pathsTable);
		pathsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = pathsTable.getSelectFileSystemIndex();
				if (index < 0) {
					changeBtn.setEnabled(false);
					removeBtn.setEnabled(false);
				} else {
					if (index == 0) {
						removeBtn.setEnabled(false);
					} else {
						removeBtn.setEnabled(true);
					}
					changeBtn.setEnabled(true);
				}
			}
		});
		c.add(mianPane);
		modifyComponentSize(window);
	}

	// 刷新列表
	private void refresh() {
		paths.clear();
		FileSystemPath mainfsp = new FileSystemPath();
		mainfsp.setType(FileSystemPath.MAIN_FILE_SYSTEM_NAME);
		mainfsp.setPath(SettingWindow.chooserPath);
		mainfsp.setIndex((short) 0);// 主文件系统路径的编号必须是0！
		paths.add(mainfsp);
		if (SettingWindow.extendStores != null) {
			paths.addAll(SettingWindow.extendStores);
		}
		pathsTable.updateValues(paths);
	}

	/**
	 * 
	 * <h2>开启文件浏览窗口并显示ROOT视图</h2>
	 * <p>
	 * 该方法将获取ROOT文件视图并开启显示窗口，在此之前还会自动初始化文件节点。该过程可能耗时，因此应给予提示。
	 * 当一个文件窗口已经打开时，再次调用此方法仅会显示唯一的文件窗口（不会再打开一个新的文件窗口）。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void show() {
		disableAllButtons();
		refresh();
		if (paths == null || paths.size() == 0) {
			Printer.instance.print("错误：无法获取文件系统设置，请手动检查配置文件并重启应用。");
		} else {
			enableAllButtons();
			window.setVisible(true);
		}
	}

	/**
	 * 
	 * <h2>获取文件管理器的唯一实例</h2>
	 * <p>
	 * 该方法能够获得文件管理器的唯一实例，在第一次打开时会进行较耗时的资源加载工作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.ui.module.FSViewer 该视图的唯一实例，程序中只会存在一个该窗口。
	 */
	public static FileSystemPathViewer getInstance() {
		if (fsv == null) {
			fsv = new FileSystemPathViewer();
		}
		return fsv;
	}

	// 锁定全部按钮避免重复操作
	private void disableAllButtons() {
		addBtn.setEnabled(false);
		changeBtn.setEnabled(false);
		removeBtn.setEnabled(false);
	}

	// 解锁可用按钮
	private void enableAllButtons() {
		// 针对一些常规按钮的解锁
		if (SettingWindow.extendStores.size() < maxExtendStoresNum) {
			addBtn.setEnabled(true);
		} else {
			addBtn.setEnabled(false);
		}
		// 针对“新增”和“移除”两个按钮的解锁
		short index = pathsTable.getSelectFileSystemIndex();
		if (index < 0) {
			changeBtn.setEnabled(false);
			removeBtn.setEnabled(false);
		} else {
			if (index == 0) {
				removeBtn.setEnabled(false);
			} else {
				removeBtn.setEnabled(true);
			}
			changeBtn.setEnabled(true);
		}
	}

}
