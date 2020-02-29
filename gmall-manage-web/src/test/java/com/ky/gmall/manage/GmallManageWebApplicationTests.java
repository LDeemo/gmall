package com.ky.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GmallManageWebApplicationTests {

    @Test
    void contextLoads() throws IOException, MyException {
        //配置fdfs的全局链接地址
        String path = GmallManageWebApplicationTests.class.getResource("/tracker.conf").getPath();//获得配置文件的路径

        ClientGlobal.init(path);

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer trackerServer = trackerClient.getTrackerServer();
        StorageClient storageClient = new StorageClient(trackerServer, null);
        String orginalFilename="E:/picture/ghs/fuliblxyz.jpg";
        String[] upload_file = storageClient.upload_file(orginalFilename, "jpg", null);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < upload_file.length; i++) {
            sb.append("/"+upload_file[i]);
            System.out.println("s = " + upload_file[i]);
        }
        System.out.println("http://192.168.64.130"+sb.toString());

    }

}
