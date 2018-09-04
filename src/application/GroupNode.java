package application;

import org.bson.types.ObjectId;

public class GroupNode {
	ObjectId record;
	String name;
	
	GroupNode(ObjectId r,String n)
	{
		record = r;
		name = n;
	}
	
	GroupNode(ObjectId r)
	{
		record = r;
	}
	
}
