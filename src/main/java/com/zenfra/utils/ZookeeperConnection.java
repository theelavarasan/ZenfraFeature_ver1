package com.zenfra.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.google.common.io.Files;
import com.zenfra.model.ZKConstants;


public class ZookeeperConnection {

	// declare zookeeper instance to access ZooKeeper ensemble
	
	private ZooKeeper zoo;
	final CountDownLatch connectedSignal = new CountDownLatch(1);
	// Method to connect zookeeper ensemble.
	public ZooKeeper connect(String host) throws IOException, InterruptedException {

		zoo = new ZooKeeper(host, 5000, new Watcher() {

			public void process(WatchedEvent we) {

				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});

		connectedSignal.await();
		return zoo;
	}

	// Method to disconnect from zookeeper server
	public void close() throws InterruptedException {
		zoo.close();
	}

	public Map<String, String> getZKData() throws IOException, KeeperException, InterruptedException {
		   
		   String zkPropertiesFilePath = ZKConstants.ZK_PROPERTIES_FILE_PATH;
		   File zkPropertiesFile = new File(zkPropertiesFilePath);
		   InputStream input = new FileInputStream(zkPropertiesFile);
		   Properties properties = new Properties();
		   properties.load(input);
		   String zookeeperServer = properties.getProperty(ZKConstants.SERVER);
		   String defaultPath = properties.getProperty(ZKConstants.DEFAULT_PATH);
		   String repoPath = properties.getProperty(ZKConstants.ZCAMUNDA);
		   ZookeeperConnection conn = new ZookeeperConnection();
		   ZooKeeper zk = conn.connect(zookeeperServer);
		  String nodePath = (defaultPath + repoPath);
		   String nodePath1 = defaultPath + "/common";
		  List<String> children = zk.getChildren(nodePath, new Watcher(){

				@Override
				public void process(WatchedEvent event) {
					if(event.getType()==Event.EventType.NodeChildrenChanged){
						System.out.println("node children updated");
					}
				}}); 
		   
		   List<String> children1 = zk.getChildren(nodePath1, new Watcher(){

				@Override
				public void process(WatchedEvent event) {
					if(event.getType()==Event.EventType.NodeChildrenChanged){
						System.out.println("node children updated");
					}
				}});
			
			Map<String, String> zkData = new HashMap<String, String>();
			for(int i = 0; i  < children.size(); i++) {
				byte[] b = zk.getData(nodePath + "/" + children.get(i), true, new Stat());
				zkData.put(children.get(i), new String(b, ZKConstants.STRING_FORMAT));
			} 
			
			for(int i = 0; i  < children1.size(); i++) {
				byte[] b = zk.getData(nodePath1 + "/" + children1.get(i), true, new Stat());
				zkData.put(children1.get(i), new String(b, ZKConstants.STRING_FORMAT));
			}
			
			//System.out.println("zkData: " + zkData);
			
			return zkData;
		}
	
	public void copyQueryFiles() {
		
		String path = "./properties/ReportQueries/";
		
		try {
			//String path = "./properties/ReportQueries/" + devicesArray.get(i).toString().toLowerCase().replace("-", "") + "ServerClickReport.properties";
			ClassLoader classLoader = getClass().getClassLoader();
			URL resources = classLoader.getResource(path);
			System.out.println("!!!!! File: " + resources.getFile());
			System.out.println("!!!!! Path: " + resources.getPath());
			File file = new File(resources.getFile());
			File[] fileList = file.listFiles();
			System.out.println(fileList.length);
			for(int i = 0; i < fileList.length; i++) {
				if(fileList[i].getName().contains("ServerClickReport")) {
					File queryFile = new File(fileList[i].getAbsolutePath());
					File dest = new File("/opt/config/" + fileList[i].getName());
					if(queryFile.exists()) {
						Files.copy(queryFile, dest);
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	   
}
