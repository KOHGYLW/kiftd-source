package kohgylw.kiftd.ui.util;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.util.file_system_manager.pojo.Folder;

/**
 * 
 * <h2>文件列表表格</h2>
 * <p>
 * 一个基于Swing默认JTable的表格工具，用于显示文件列表。该工具支持选中行，并会以蓝色和黑色区分文件夹和文件。
 * 需要使用，请直接创建该类的实例，默认显示空表格。如需更新数据，请使用updateValues方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FilesTable extends JTable {

	private static final String[] columns = new String[] { "名称", "创建日期", "大小", "创建者" };
	private static List<Folder> folders;// 当前显示的文件夹列表
	public static final int MAX_LIST_LIMIT = Integer.MAX_VALUE;

	/**  */
	private static final long serialVersionUID = -3436472714356711024L;

	public FilesTable() {
		super(new Object[][] {}, columns);
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void updateValues(List<Folder> folders, List<Node> files) {
		Runnable doUpdate = new Runnable() {
			@Override
			public void run() {
				try {
					setModel(new TableModel() {
						@Override
						public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
						}

						@Override
						public void removeTableModelListener(TableModelListener l) {
						}

						@Override
						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return false;
						}

						@Override
						public Object getValueAt(int rowIndex, int columnIndex) {
							switch (columnIndex) {
							case 0:
								return rowIndex < folders.size() ? "/" + folders.get(rowIndex).getFolderName()
										: files.get(rowIndex - folders.size()).getFileName();
							case 1:
								return rowIndex < folders.size() ? folders.get(rowIndex).getFolderCreationDate()
										: files.get(rowIndex - folders.size()).getFileCreationDate();
							case 2:
								return rowIndex < folders.size() ? "--"
										: files.get(rowIndex - folders.size()).getFileSize();
							case 3:
								return rowIndex < folders.size() ? folders.get(rowIndex).getFolderCreator()
										: files.get(rowIndex - folders.size()).getFileCreator();
							default:
								return "--";
							}
						}

						@Override
						public int getRowCount() {
							long totalSize = folders.size() + files.size();
							return totalSize > MAX_LIST_LIMIT ? MAX_LIST_LIMIT : (int) totalSize;
						}

						@Override
						public String getColumnName(int columnIndex) {
							return columns[columnIndex];
						}

						@Override
						public int getColumnCount() {
							return columns.length;
						}

						@Override
						public Class<?> getColumnClass(int columnIndex) {
							return Object.class;
						}

						@Override
						public void addTableModelListener(TableModelListener l) {
						}
					});
					setRowFontColor();
					validate();
					FilesTable.folders = folders;
				} catch (Exception e) {
					Printer.instance.print(e.toString());
				}
			}
		};
		// 避免操作过快导致的异常
		SwingUtilities.invokeLater(doUpdate);
	}

	private void setRowFontColor() {
		DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 5132133158132959506L;

			@Override
			protected void setValue(Object value) {
				// 文件夹和文件名称染色（文件是黑色，文件夹是蓝色，以便区分）
				if (value instanceof String && ((String) value).startsWith("/")) {
					setForeground(Color.BLUE);
				} else {
					setForeground(Color.black);
				}
				setText((String) value);
			}
		};
		DefaultTableCellRenderer dtcr2 = new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 5132133158132959506L;

			@Override
			protected void setValue(Object value) {
				if (!"--".equals(value)) {
					// 对文件体积进行换算
					if (value instanceof String) {
						double convertSize;
						String unit;
						long size = Long.parseLong((String) value);
						if (size < 1024) {
							convertSize = (double) size;
							unit = "B";
						} else if (size < 1048576L) {
							convertSize = ((double) size / 1024.0);
							unit = "KB";
						} else if (size < 1073741824L) {
							convertSize = ((double) size / 1048576.0);
							unit = "MB";
						} else if (size < 1099511627776L) {
							convertSize = ((double) size / 1073741824.0);
							unit = "GB";
						} else {
							convertSize = ((double) size / 1099511627776.0);
							unit = "TB";
						}
						setText(String.format("%.1f", convertSize) + " " + unit);
						return;
					}
				}
				setText((String) value);
			}
		};
		getColumn(columns[0]).setCellRenderer(dtcr);
		getColumn(columns[2]).setCellRenderer(dtcr2);
	}

	/**
	 * 
	 * <h2>获取被双击的文件夹</h2>
	 * <p>
	 * 该功能用于进入某一文件夹。如果双击的是文件夹，则返回其对象，否则返回null。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param e java.awt.event.MouseEvent 鼠标事件
	 * @return kohgylw.kiftd.util.file_system_manager.pojo.Folder 被双击的文件夹
	 */
	public Folder getDoubleClickFolder(MouseEvent e) {
		if (e.getClickCount() == 2) {
			int row = rowAtPoint(e.getPoint());
			if (row >= 0 && row < folders.size()) {
				return folders.get(row);
			}
		}
		return null;
	}
}
