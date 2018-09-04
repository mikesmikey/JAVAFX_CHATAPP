package application;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public class AdminController {

	@FXML Button loadAllUserButton;
	@FXML ListView<String> userList;
	
	@FXML
	private void loadAllUserButtonAction()
	{
		userList.getItems().clear();
		
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		try (MongoCursor<Document> cursor = table.find().iterator()) 
		{
			while (cursor.hasNext())
				userList.getItems().add(cursor.next().getString("username"));
		}
	}

	@FXML TextField adminSearchField;
	@FXML Button adminSearchButton;
	
	@FXML
	private void adminSearchEnter()
	{
		adminSearchField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!adminSearchField.getText().equals(""))
					{
	            		adminSearchButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void adminSearchButtonAction()
	{
		userList.getItems().clear();
		
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		if (!adminSearchField.getText().trim().isEmpty())
		{
			Document regex = new Document().append("$regex", ".*"+adminSearchField.getText()+".*");
			Document searchquery = new Document().append("username", regex);
			
			try (MongoCursor<Document> cursor = table.find(searchquery).iterator()) 
			{
				while (cursor.hasNext())
					userList.getItems().add(cursor.next().getString("username"));
			}
		}
	}
	
	@FXML Button adminForceLogoutAllButton;
	
	@FXML
	private void adminForceLogoutAllButtonAction()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		try (MongoCursor<Document> cursor = table.find().iterator()) 
		{
			while (cursor.hasNext())
				Main.oppubsub.publish(cursor.next().getString("username"), "systemmessage ADMIN forcelogout");
		}
		
	}
	
	@FXML Button adminSetZeroButtonn;
	
	@FXML
	private void adminSetZeroButtonAction()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		MongoCollection<Document> recordtable = db.getCollection("records");
		
		try (MongoCursor<Document> cursor = table.find().iterator()) 
		{
			while (cursor.hasNext())
				Main.oppubsub.publish(cursor.next().getString("username"), "systemmessage ADMIN forcelogout");
		}
		
		table.drop();
		recordtable.drop();
	}
	
	@FXML Button adminDeleteAllUserButton;
	
	@FXML
	private void adminDeleteAllUserButtonAction()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		try (MongoCursor<Document> cursor = table.find().iterator()) 
		{
			while (cursor.hasNext())
				Main.oppubsub.publish(cursor.next().getString("username"), "systemmessage ADMIN forcelogout");
		}
		
		table.drop();
	}
	
	@FXML Label adminViewNameLabel;
	@FXML Label adminViewUsernameLabel;
	@FXML Label adminViewMOTDLabel;
	@FXML Label adminViewSuperUsrLabel;
	@FXML Label adminViewStatusLabel;
	@FXML ImageView adminImageView;
	
	@FXML 
	public void OnClickOnList() 
	{
		setAdminButton.setVisible(false);
		adminForceLogoutButton.setVisible(false);
		adminRemoveAccountButton.setVisible(false);
		
		adminViewNameLabel.setText("Name : ");
		adminViewUsernameLabel.setText("Username : ");
		adminViewMOTDLabel.setText("MOTD : ");
		adminViewSuperUsrLabel.setText("Admin : ");
		adminViewStatusLabel.setText("Status : ");
		adminViewStatusLabel.setTextFill(Color.web("#000000"));
		
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		Document searchquery = new Document().append("username", userList.getSelectionModel().getSelectedItem());
		Document viewdata = table.find(searchquery).first();
		
		if (viewdata != null)
		{
			adminImageView.setImage(SampleController.loadAvatar(viewdata.getObjectId("avatar_image_id")));
			
			adminViewNameLabel.setText(adminViewNameLabel.getText()+viewdata.getString("name"));
			adminViewUsernameLabel.setText(adminViewUsernameLabel.getText()+viewdata.getString("username"));
			adminViewMOTDLabel.setText(adminViewMOTDLabel.getText()+viewdata.getString("motd"));
			
			if (userList.getSelectionModel().getSelectedItem().equals("kraiwich") || (viewdata.getInteger("admin") != null && viewdata.getInteger("admin") == 1 ))
			{
				adminViewSuperUsrLabel.setText(adminViewSuperUsrLabel.getText()+"Yes");
				setAdminButton.setText("ปลดจากแอดมิน");
			}
			else
			{
				adminViewSuperUsrLabel.setText(adminViewSuperUsrLabel.getText()+"No");
				setAdminButton.setText("แต่งตั้งเป็นแอดมิน");
			}
			
			if (viewdata.getInteger("status") == 0)
			{
				adminViewStatusLabel.setText(adminViewStatusLabel.getText()+"Offline");
				adminViewStatusLabel.setTextFill(Color.web("#ff0000"));
			}
			else if (viewdata.getInteger("status") == 1)
			{
				adminViewStatusLabel.setText(adminViewStatusLabel.getText()+"Online");
				adminViewStatusLabel.setTextFill(Color.web("#00f731"));
			}
			
			setAdminButton.setVisible(true);
			adminForceLogoutButton.setVisible(true);
			adminRemoveAccountButton.setVisible(true);
		}
	}
	
	@FXML Button setAdminButton;
	
	@FXML
	private void setAdminButtonAction()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		Document searchquery = new Document().append("username", userList.getSelectionModel().getSelectedItem());
		Document viewdata = table.find(searchquery).first();
		
		if (viewdata.getInteger("admin") != null && viewdata.getInteger("admin") == 1)
		{
			if (userList.getSelectionModel().getSelectedItem().equals("kraiwich"))
			{
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Admin set error.");
				alert.setHeaderText("คุณปลด kraiwich ออกจากการเป็นแอดมินไม่ได้!");
				
				alert.showAndWait();
			}
			else
			{
				Document newdata = new Document().append("admin", 0);
				table.updateOne(searchquery, new Document().append("$set", newdata));
				Main.oppubsub.publish(viewdata.getString("username"), "systemmessage ADMIN forcelogout");
			}
		}
		else
		{
			Document newdata = new Document().append("admin", 1);	
			table.updateOne(searchquery, new Document().append("$set", newdata));
			Main.oppubsub.publish(viewdata.getString("username"), "systemmessage ADMIN forcelogout");
		}
		OnClickOnList();
	}
	
	@FXML Button adminForceLogoutButton;
	
	@FXML
	private void adminForceLogoutButtonAction()
	{
		Main.oppubsub.publish(userList.getSelectionModel().getSelectedItem(), "systemmessage ADMIN forcelogout");
		OnClickOnList();
	}
	
	@FXML Button adminRemoveAccountButton;
	
	@FXML
	private void adminRemoveAccountButtonAction()
	{
		Main.oppubsub.publish(userList.getSelectionModel().getSelectedItem(), "systemmessage ADMIN forcelogout");
		
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		Document searchquery = new Document().append("username", userList.getSelectionModel().getSelectedItem());
		table.findOneAndDelete(searchquery);
		userList.getItems().remove(userList.getSelectionModel().getSelectedItem());

		OnClickOnList();
	}
}
