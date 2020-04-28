package kohgylw.kiftd.ui.module;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.exception.FilesTotalOutOfLimitException;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.util.FileNodeUtil;
import kohgylw.kiftd.ui.util.FilesTable;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;
import kohgylw.kiftd.util.file_system_manager.pojo.Folder;
import kohgylw.kiftd.util.file_system_manager.pojo.FolderView;

/**
 * 
 * <h2>文件管理窗口</h2>
 * <p>
 * 该窗口是文件导入管理工具的图形化操作页面，应由主界面上的“文件”按钮开启。
 * 其中除了包含页面内容外，也封装了一些文件管理必要的操作过程，与文件管理器工具直接对接。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FSViewer extends KiftdDynamicWindow {

	protected static JDialog window;// 窗体对象
	private JButton homeBtn;// 主目录
	private JButton backToParentFolder;// 返回上一级
	private JButton importBtn;// 导入按钮
	private JButton exportBtn;// 导出按钮
	private JButton deleteBtn;// 删除按钮
	private JButton refreshBtn;// 刷新按钮
	private FilesTable filesTable;// 文件列表对象

	private static FSViewer fsv;// 该窗口的唯一实例
	private static FolderView currentView;// 当前显示的视图
	private static ExecutorService worker;// 操作线程池

	// 资源加载
	private FSViewer() throws SQLException {
		setUIFont();
		worker = Executors.newSingleThreadExecutor();
		window = new JDialog(ServerUIModule.window, "kiftd-ROOT");
		window.setSize(750, 450);
		window.setDefaultCloseOperation(1);
		window.setLocation(150, 150);
		window.setResizable(true);
		Container c = window.getContentPane();
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		// TODO 自动生成的 catch 块
		homeBtn = new JButton("根目录[/Root]");
		backToParentFolder = new JButton("上一级[^]");
		importBtn = new JButton("导入[<-]");
		exportBtn = new JButton("导出[->]");
		deleteBtn = new JButton("删除[X]");
		refreshBtn = new JButton("刷新[*]");
		homeBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		homeBtn.setEnabled(false);
		backToParentFolder.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		backToParentFolder.setEnabled(false);
		importBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		exportBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		exportBtn.setEnabled(false);
		deleteBtn.setPreferredSize(new Dimension((int) (105 * proportion), (int) (35 * proportion)));
		deleteBtn.setEnabled(false);
		refreshBtn.setPreferredSize(new Dimension((int) (105 * proportion), (int) (35 * proportion)));
		toolBar.add(homeBtn);
		toolBar.add(backToParentFolder);
		toolBar.addSeparator();
		toolBar.add(importBtn);
		toolBar.addSeparator();
		toolBar.add(exportBtn);
		toolBar.add(deleteBtn);
		toolBar.addSeparator();
		toolBar.add(refreshBtn);
		c.add(toolBar, BorderLayout.NORTH);
		// 各个工具栏按钮的功能实现
		homeBtn.addActionListener((e) -> {
			disableAllButtons();
			worker.execute(() -> {
				try {
					getFolderView("root");
				} catch (Exception e1) {
					// TODO 自动生成的 catch 块
					JOptionPane.showMessageDialog(window, "出现意外错误：无法读取文件列表，请重试或重启应用。", "错误", JOptionPane.ERROR_MESSAGE);
				}
				enableAllButtons();
			});
		});
		backToParentFolder.addActionListener((e) -> {
			disableAllButtons();
			worker.execute(() -> {
				try {
					getFolderView(currentView.getCurrent().getFolderParent());
				} catch (Exception e1) {
					// TODO 自动生成的 catch 块
					JOptionPane.showMessageDialog(window, "出现意外错误：无法读取文件列表，请重试或重启应用。", "错误", JOptionPane.ERROR_MESSAGE);
				}
				enableAllButtons();
			});
		});
		importBtn.addActionListener((e) -> {
			disableAllButtons();
			JFileChooser importChooer = new JFileChooser();
			importChooer.setMultiSelectionEnabled(true);
			importChooer.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			importChooer.setPreferredSize(fileChooerSize);
			importChooer.setDialogTitle("请选择...");
			if (importChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				worker.execute(() -> {
					doImport(importChooer.getSelectedFiles());
					enableAllButtons();
				});
			} else {
				enableAllButtons();
			}
		});
		exportBtn.addActionListener((e) -> {
			JFileChooser exportChooer = new JFileChooser();
			exportChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			exportChooer.setPreferredSize(fileChooerSize);
			exportChooer.setDialogTitle("导出到...");
			if (exportChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				disableAllButtons();
				worker.execute(() -> {
					File path = exportChooer.getSelectedFile();
					int[] selected = filesTable.getSelectedRows();
					List<String> selectedNodes = new ArrayList<>();
					List<String> selectedFolders = new ArrayList<>();
					int borderIndex = currentView.getFolders().size();
					for (int i : selected) {
						if (i < borderIndex) {
							selectedFolders.add(currentView.getFolders().get(i).getFolderId());
						} else {
							selectedNodes.add(currentView.getFiles().get(i - borderIndex).getFileId());
						}
					}
					String[] folders = selectedFolders.toArray(new String[0]);
					String[] nodes = selectedNodes.toArray(new String[0]);
					int exi = 0;
					try {
						exi = FileSystemManager.getInstance().hasExistsFilesOrFolders(folders, nodes, path);
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(window, "出现意外错误，无法导出文件，请重试。", "错误", JOptionPane.ERROR_MESSAGE);
						refresh();
						enableAllButtons();
						return;
					}
					String type = null;
					if (exi > 0) {
						switch (JOptionPane.showConfirmDialog(window,
								"该路径存在" + exi + "个同名文件或文件夹，您希望覆盖它们么？（“是”覆盖，“否”保留两者，“取消”终止导入）", "导入",
								JOptionPane.YES_NO_CANCEL_OPTION)) {
						case JOptionPane.YES_OPTION:
							type = FileSystemManager.COVER;
							break;
						case JOptionPane.NO_OPTION:
							type = FileSystemManager.BOTH;
							break;
						case JOptionPane.CANCEL_OPTION:

						default:
							type = "CANCEL";
							enableAllButtons();
							return;
						}
					}
					FSProgressDialog fsd = FSProgressDialog.getNewInstance();
					Thread deleteListenerDialog = new Thread(() -> {
						fsd.show();
					});
					deleteListenerDialog.start();
					try {
						FileSystemManager.getInstance().exportTo(folders, nodes, path, type);
						fsd.close();
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						fsd.close();
						JOptionPane.showMessageDialog(window, "导出文件时失败，该操作已被中断，未能全部导出。", "错误",
								JOptionPane.ERROR_MESSAGE);
					}
					refresh();
					enableAllButtons();
				});
			}
		});
		deleteBtn.addActionListener((e) -> {
			disableAllButtons();
			if (JOptionPane.showConfirmDialog(window, "确认要删除这些文件么？警告：该操作无法恢复。", "删除",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				int[] selected = filesTable.getSelectedRows();
				worker.execute(() -> {
					Runnable doDeleteThread = new Runnable() {
						@Override
						public void run() {
							// TODO 自动生成的方法存根
							List<String> selectedNodes = new ArrayList<>();
							List<String> selectedFolders = new ArrayList<>();
							int borderIndex = currentView.getFolders().size();
							for (int i : selected) {
								if (i < borderIndex) {
									selectedFolders.add(currentView.getFolders().get(i).getFolderId());
								} else {
									selectedNodes.add(currentView.getFiles().get(i - borderIndex).getFileId());
								}
							}
							FSProgressDialog fsd = FSProgressDialog.getNewInstance();
							Thread fsProgressDialogThread = new Thread(() -> {
								fsd.show();
							});
							fsProgressDialogThread.start();
							try {
								FileSystemManager.getInstance().delete(selectedFolders.toArray(new String[0]),
										selectedNodes.toArray(new String[0]));
								fsd.close();
							} catch (Exception e1) {
								// TODO 自动生成的 catch 块
								fsd.close();
								JOptionPane.showMessageDialog(window, "删除文件时失败，该操作已被中断，未能全部删除。", "错误",
										JOptionPane.ERROR_MESSAGE);
								refresh();
							}
							refresh();
							enableAllButtons();
						}
					};
					SwingUtilities.invokeLater(doDeleteThread);
				});
			} else {
				enableAllButtons();
			}
		});
		refreshBtn.addActionListener((e) -> {
			disableAllButtons();
			worker.execute(() -> {
				refresh();
				enableAllButtons();
			});
		});
		// 生成文件列表
		filesTable = new FilesTable();
		filesTable.setRowHeight((int) (16 * proportion));
		JScrollPane mianPane = new JScrollPane(filesTable);
		filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO 自动生成的方法存根
				if (filesTable.getSelectedRows().length > 0) {
					exportBtn.setEnabled(true);
					deleteBtn.setEnabled(true);
				} else {
					exportBtn.setEnabled(false);
					deleteBtn.setEnabled(false);
				}
			}
		});
		// 文件列表的双击监听（进入文件夹）
		filesTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO 自动生成的方法存根
				disableAllButtons();
				worker.execute(() -> {
					Folder f = filesTable.getDoubleClickFolder(e);
					if (f != null) {
						try {
							getFolderView(f.getFolderId());
						} catch (Exception e1) {
							// TODO 自动生成的 catch 块
						}
					}
					enableAllButtons();
				});
			}
		});
		// 文件列表的拖拽监听
		DropTargetListener dtl = new DropTargetListener() {

			@Override
			public void dropActionChanged(DropTargetDragEvent dtde) {
				// TODO 自动生成的方法存根

			}

			@Override
			public void drop(DropTargetDropEvent dtde) {
				// TODO 自动生成的方法存根
				if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					try {
						Object dropTarget = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						@SuppressWarnings("unchecked")
						List<File> files = (List<File>) dropTarget;
						dtde.dropComplete(true);
						worker.execute(() -> {
							disableAllButtons();
							doImport(files.toArray(new File[0]));
							enableAllButtons();
						});
					} catch (Exception e) {
						// TODO 自动生成的 catch 块
						worker.execute(() -> {
							Runnable refreshThread = new Runnable() {
								@Override
								public void run() {
									// TODO 自动生成的方法存根
									refresh();
								}

							};
							SwingUtilities.invokeLater(refreshThread);
						});
					}
				}
			}

			@Override
			public void dragOver(DropTargetDragEvent dtde) {
				// TODO 自动生成的方法存根

			}

			@Override
			public void dragExit(DropTargetEvent dte) {
				// TODO 自动生成的方法存根

			}

			@Override
			public void dragEnter(DropTargetDragEvent dtde) {
				// TODO 自动生成的方法存根

			}
		};
		window.setDropTarget(new DropTarget(window, DnDConstants.ACTION_COPY_OR_MOVE, dtl));
		c.add(mianPane);
		modifyComponentSize(window);
	}

	// 刷新文件列表
	private void refresh() {
		try {
			getFolderView(currentView.getCurrent().getFolderId());
		} catch (Exception e1) {
			// TODO 自动生成的 catch 块
			JOptionPane.showMessageDialog(window, "无法刷新文件列表，请重试或返回根目录。", "错误", JOptionPane.ERROR_MESSAGE);
		}
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
		FileNodeUtil.initNodeTableToDataBase();
		try {
			if (currentView == null) {
				getFolderView("root");
			} else {
				refresh();
			}
			enableAllButtons();
			window.setVisible(true);
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			Printer.instance.print("错误：无法打开文件系统，该文件系统可能正在被另一个kiftd占用。");
		}
	}

	// 显示指定文件夹ID的文件列表视图（每次调用均会显示最新的列表）
	private void getFolderView(String folderId) throws Exception {
		try {
			currentView = FileSystemManager.getInstance().getFolderView(folderId);
			long maxTotalNum = currentView.getFiles().size() + currentView.getFolders().size();
			if (maxTotalNum > FilesTable.MAX_LIST_LIMIT) {
				JOptionPane.showMessageDialog(window,
						"文件夹列表的长度已超过最大限值（" + FilesTable.MAX_LIST_LIMIT + "），只能显示前" + FilesTable.MAX_LIST_LIMIT + "行。",
						"警告", JOptionPane.WARNING_MESSAGE);
			}
			if (currentView != null && currentView.getCurrent() != null) {
				filesTable.updateValues(currentView.getFolders(), currentView.getFiles());
				window.setTitle("kiftd-" + currentView.getCurrent().getFolderName());
			} else {
				// 浏览一个不存在的文件夹时自动返回根目录
				getFolderView("root");
			}
		} catch (Exception e) {
			throw e;
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
	public static FSViewer getInstance() throws SQLException {
		if (fsv == null) {
			fsv = new FSViewer();
		}
		return fsv;
	}

	// 执行导入任务
	private void doImport(File[] files) {
		int exi = 0;
		String folderId = currentView.getCurrent().getFolderId();
		try {
			exi = FileSystemManager.getInstance().hasExistsFilesOrFolders(files, folderId);
		} catch (SQLException e1) {
			// TODO 自动生成的 catch 块
			JOptionPane.showMessageDialog(window, "出现意外错误，无法导入文件，请刷新或重启应用后重试。", "错误", JOptionPane.ERROR_MESSAGE);
			refresh();
			return;
		}
		String type = null;
		if (exi > 0) {
			switch (JOptionPane.showConfirmDialog(window, "该路径存在" + exi + "个同名文件或文件夹，您希望覆盖它们么？（“是”覆盖，“否”保留两者，“取消”终止导入）",
					"导入", JOptionPane.YES_NO_CANCEL_OPTION)) {
			case 0:
				type = FileSystemManager.COVER;
				break;
			case 1:
				type = FileSystemManager.BOTH;
				break;
			case 2:

			default:
				type = "CANCEL";
				return;
			}
		}
		// 打开进度提示会话框
		FSProgressDialog fsd = FSProgressDialog.getNewInstance();
		Thread fspt = new Thread(() -> {
			fsd.show();
		});
		fspt.start();
		try {
			FileSystemManager.getInstance().importFrom(files, folderId, type);
		} catch (FoldersTotalOutOfLimitException e1) {
			JOptionPane.showMessageDialog(window, "导入失败，该文件夹内的文件夹数目已达上限，无法导入更多文件夹。", "错误", JOptionPane.ERROR_MESSAGE);
		} catch (FilesTotalOutOfLimitException e2) {
			JOptionPane.showMessageDialog(window, "导入失败，该文件夹内的文件数目已达上限，无法导入更多文件。", "错误", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e3) {
			JOptionPane.showMessageDialog(window, "导入失败，无法完成导入，该操作已被中断。", "错误", JOptionPane.ERROR_MESSAGE);
		}
		fsd.close();
		refresh();
	}

	// 锁定全部按钮避免重复操作
	private void disableAllButtons() {
		homeBtn.setEnabled(false);
		backToParentFolder.setEnabled(false);
		importBtn.setEnabled(false);
		exportBtn.setEnabled(false);
		deleteBtn.setEnabled(false);
		refreshBtn.setEnabled(false);
	}

	// 解锁可用按钮
	private void enableAllButtons() {
		// 针对一些常规按钮的解锁
		refreshBtn.setEnabled(true);
		importBtn.setEnabled(true);
		// 针对“导出”和“删除”两个按钮的解锁
		if (filesTable.getSelectedRows().length > 0) {
			exportBtn.setEnabled(true);
			deleteBtn.setEnabled(true);
		}
		// 针对“上一级”和“根目录”按钮的解锁
		if (currentView != null && !"null".equals(currentView.getCurrent().getFolderParent())) {
			backToParentFolder.setEnabled(true);
			homeBtn.setEnabled(true);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		worker.shutdown();
	}

}
