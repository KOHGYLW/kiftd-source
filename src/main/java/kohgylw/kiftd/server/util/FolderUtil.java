package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import javax.annotation.*;
import kohgylw.kiftd.server.mapper.*;
import kohgylw.kiftd.server.model.*;
import java.util.*;

@Component
public class FolderUtil
{
    @Resource
    private FolderMapper fm;
    @Resource
    private NodeMapper fim;
    @Resource
    private FileBlockUtil fbu;
    
    public List<Folder> getParentList(final String fid) {
        Folder f = this.fm.queryById(fid);
        final List<Folder> folderList = new ArrayList<Folder>();
        if (f != null) {
            while (!f.getFolderParent().equals("null")) {
                f = this.fm.queryById(f.getFolderParent());
                folderList.add(f);
            }
        }
        Collections.reverse(folderList);
        return folderList;
    }
    
    public int deleteAllChildFolder(final String folderId) {
        final String fileblocks = ConfigureReader.instance().getFileBlockPath();
        final Thread deleteChildFolderThread = new Thread(() -> this.iterationDeleteFolder(fileblocks, folderId));
        deleteChildFolderThread.start();
        return this.fm.deleteById(folderId);
    }
    
    private void iterationDeleteFolder(final String fileblocks, final String folderId) {
        final List<Folder> cf = (List<Folder>)this.fm.queryByParentId(folderId);
        if (cf.size() > 0) {
            for (final Folder f : cf) {
                this.iterationDeleteFolder(fileblocks, f.getFolderId());
            }
        }
        final List<Node> files = (List<Node>)this.fim.queryByParentFolderId(folderId);
        if (files.size() > 0) {
            this.fim.deleteByParentFolderId(folderId);
            for (final Node f2 : files) {
                this.fbu.deleteFromFileBlocks(f2);
            }
        }
        this.fm.deleteById(folderId);
    }
}
