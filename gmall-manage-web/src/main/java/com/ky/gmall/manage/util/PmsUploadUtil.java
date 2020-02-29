package com.ky.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {


    public static String uploadImage(MultipartFile multipartFile) {
        String imgUrl = "http://192.168.64.130";
        //上传图片到服务器
        //配置fdfs的全局链接地址
        String path = PmsUploadUtil.class.getResource("/tracker.conf").getPath();//获得配置文件的路径
        try {
            ClientGlobal.init(path);

            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getTrackerServer();

            byte[] bytes = multipartFile.getBytes();//获得上传的二进制对象
            String file_ext_name = multipartFile.getOriginalFilename();
            file_ext_name = file_ext_name.substring(file_ext_name.lastIndexOf(".") + 1);

            StorageClient storageClient = new StorageClient(trackerServer, null);
            String[] upload_file = storageClient.upload_file(bytes, file_ext_name, null);

            for (int i = 0; i < upload_file.length; i++) {
                imgUrl += ( "/" + upload_file[i]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return imgUrl;
    }
}
