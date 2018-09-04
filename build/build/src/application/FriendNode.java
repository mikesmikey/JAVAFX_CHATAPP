package application;

import org.bson.types.ObjectId;

import javafx.scene.image.Image;

public class FriendNode {
	String id;
	String name;
	String motd;
	ObjectId avatarid;
	int type;
	int status=0;
	boolean nodeappear = false;
	String record = null;
	double onlinetime=0;
	Image avatar;
	
	FriendNode(String i,int t)
	{
		id = i;
		type = t;
	}
	
	FriendNode(String i,String n,int t,String r,ObjectId a,Image img)
	{
		id = i;
		name = n;
		type = t;
		record = r;
		avatarid = a;
		avatar = img;
	}
}
