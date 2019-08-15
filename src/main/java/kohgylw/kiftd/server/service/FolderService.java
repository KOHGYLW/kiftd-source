package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface FolderService
{
    String newFolder(final HttpServletRequest request);
    
    String deleteFolder(final HttpServletRequest request);
    
    String renameFolder(final HttpServletRequest request);
    
    String deleteFolderByName(final HttpServletRequest request);
    
    /**
     * 
     * <h2>上传文件夹前置操作：创建一个新名称的文件夹</h2>
     * <p>
     * 该操作用于实现上传同名文件夹时的“保留两者”，执行该方法将创建一个带编号的文件夹从而方便上传。
     * 执行后，将返回一个结果json对象，其中包括result和newName两个属性。result的值可为：success 成功，error 失败。
     * 若创建成功，则可从newName中获取这个新建的文件夹名称，并在上传文件夹时使用该名称作为newFolderName属性的值。
     * </p>
     * @author 青阳龙野(kohgylw)
     * @param request javax.servlet.http.HttpServletRequest 请求对象
     * @return java.lang.String 返回的结果json对象
     */
    String createNewFolderByName(final HttpServletRequest request);
}
