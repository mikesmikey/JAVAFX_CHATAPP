package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import redis.clients.jedis.netty.OptimizedPubSub;
import redis.clients.jedis.netty.SubListener;
import application.Main;

public class SampleController {
	
	//Messages queue
	Queue<Message> sysmessageq = new LinkedList<>();
	Queue<Message> normessageq = new LinkedList<>();	
	
	@FXML private Button CloseButton;

	@FXML
	private void closeButtonAction()
	{
		for (FriendNode i : flist)
		{
			sysmessageq.offer(new Message (i.id,"systemmessage "+username+" goneoffline"));
		}

	    Stage stage = (Stage) CloseButton.getScene().getWindow();
	    stage.close();
	}
	
	@FXML private Button minimizeButton;
	
	@FXML
	private void minimizeButtonAction()
	{
		Stage stage = (Stage) minimizeButton.getScene().getWindow();
		stage.setIconified(true);
	}
	
	@FXML private Button maximizeButton;
	
	@FXML
	private void maximizeButtonAction()
	{
		Stage stage = (Stage) maximizeButton.getScene().getWindow();
		
		if (!stage.isMaximized())
		{
			stage.setMaximized(true);
		}
		else
		{
			stage.setMaximized(false);
		}
	}
	
	@FXML private Button GetUsernameButton;
	@FXML private TextField UsernameField;
	@FXML private BorderPane loginPane;
	@FXML private Label regisUstitleLabel;
	@FXML private Label LoginTitleLabel;
	@FXML private Label loginWarnLabel;
	
	String username = null,name = null,motd = null,userinput = null;
	int status,admin;
	ObjectId avatar;
	boolean imagechange = false;

	@FXML
	private void UsernameInputSend()
	{
		UsernameField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!UsernameField.getText().equals(""))
					{
	            		GetUsernameButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void GetUsernameButtonAction()
	{
		loginWarnLabel.setVisible(false);
		
		try 
		{
			Pattern pettern = Pattern.compile("[$&+,:;=?@#|'<>.^*()%!\\/-]");
			Matcher matcher = pettern.matcher(UsernameField.getText());
			
			if (UsernameField.getText().trim().isEmpty())
			{
				loginWarnLabel.setText("คุณไม่ได้ใส่ username!");
				loginWarnLabel.setVisible(true);
			}
			else if (UsernameField.getLength() > 12 || UsernameField.getLength() < 6)
			{
				loginWarnLabel.setText("username ต้องมีความยาว 6-12 ตัวอักษร!");
				loginWarnLabel.setVisible(true);
			}
			else if (matcher.find(0))
			{
				loginWarnLabel.setText("username ต้องไม่มีอักขระพิเศษ!");
				loginWarnLabel.setVisible(true);
			}
			else
			{
				MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
				MongoCollection<Document> table = db.getCollection("users");
				
				Document document = new Document();
				document.append("username", UsernameField.getText());
				
				userinput = UsernameField.getText();
				
				try (MongoCursor<Document> cursor = table.find(document).iterator()) 
				{
					
					Document userdata = cursor.next();
					username = userdata.getString("username");
				     
				    System.out.println("load password screen");
				     
				    FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPass.fxml"));
				    loader.setController(this);
					BorderPane newpane = (BorderPane) loader.load();
						
					Scene scene = new Scene(newpane,250,350);
					scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
					Stage stage = (Stage) GetUsernameButton.getScene().getWindow();
					stage.setScene(scene);
					
					LoginTitleLabel.setText(LoginTitleLabel.getText()+" "+username);
				}
				catch (MongoTimeoutException e) 
				{
					System.out.println("mongodb timeout");
				}
				catch (NoSuchElementException e) 
				{
					System.out.println("load regis screen");
					FXMLLoader loader = new FXMLLoader(getClass().getResource("Register.fxml"));
					loader.setController(this);
					BorderPane newpane = (BorderPane) loader.load();
					
					Scene scene = new Scene(newpane,250,350);
					scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
					Stage stage = (Stage) GetUsernameButton.getScene().getWindow();
					stage.setScene(scene);
					
					regisUstitleLabel.setText(regisUstitleLabel.getText()+" "+userinput);
				}
				catch (Exception e)
				{
					System.out.println("stage 2 "+e);
				}
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
	
	}
	
	double xOffset,yOffset;
	@FXML private HBox sceneBar;
	
	@FXML
	private void barPressed(MouseEvent event)
	{
		Stage stage = (Stage) sceneBar.getScene().getWindow();
		
		xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
	}
	
	
	@FXML
	private void barMove(MouseEvent event) 
	{
		Stage stage = (Stage) sceneBar.getScene().getWindow();
		
		stage.setX(event.getScreenX() + xOffset);
		stage.setY(event.getScreenY() + yOffset);
    }
	
	@FXML private BorderPane searchPane;
	
	@FXML
	private void addfpanePressed(MouseEvent event)
	{
		Stage stage = (Stage) searchPane.getScene().getWindow();
		
		xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
	}
	
	
	@FXML
	private void addfpaneMove(MouseEvent event) 
	{
		Stage stage = (Stage) searchPane.getScene().getWindow();
		
		stage.setX(event.getScreenX() + xOffset);
		stage.setY(event.getScreenY() + yOffset);
    }
	
	@FXML private BorderPane settingPane;
	
	@FXML
	private void settingPanePressed(MouseEvent event)
	{
		Stage stage = (Stage) settingPane.getScene().getWindow();
		
		xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
	}
	
	
	@FXML
	private void settingPaneMove(MouseEvent event) 
	{
		Stage stage = (Stage) settingPane.getScene().getWindow();
		
		stage.setX(event.getScreenX() + xOffset);
		stage.setY(event.getScreenY() + yOffset);
    }

	@FXML private Button LoginButton;
	@FXML private PasswordField LoginPassField;
	
	@FXML private Label mainScreenNameLabel;
	@FXML private Label mainScreenUsernameLabel;
	@FXML private Label mainScreenMOTDLabel;
	
	@FXML private Label loginPassWarnLabel;
	
	@FXML private ImageView mainScreenAvatarImage;
	
	@FXML private Button adminButton;
	
	LinkedList<FriendNode> flist = new LinkedList<>();
	LinkedList<GroupNode> glist = new LinkedList<>();
	
	@FXML
	private void PasswordInputSend()
	{
		LoginPassField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!LoginPassField.getText().equals(""))
					{
	            		LoginButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void LoginButtonAction ()
	{
		loginPassWarnLabel.setVisible(false);
		
		try
		{
			MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
			MongoCollection<Document> table = db.getCollection("users");
			MongoCollection<Document> recordtable = db.getCollection("records");

			Document document = new Document();
			document.append("username", username);
			
			try (MongoCursor<Document> cursor = table.find(document).iterator()) 
			{
				Document userdata  = cursor.next();
				String password = userdata.getString("password");

			    if (!LoginPassField.getText().equals(password))
			    {
			    	loginPassWarnLabel.setText("password ไม่ถูกต้อง!");
			    	loginPassWarnLabel.setVisible(true);
			    }
			    else if(userdata.getInteger("status") == 1)
			    {
			    	loginPassWarnLabel.setText("user ล๊อคอินอยู่ในขณะนี้!");
			    	loginPassWarnLabel.setVisible(true);
			    }
			    else
			    {  	
			    	username = userdata.getString("username");
			    	name = userdata.getString("name");
			    	status = 1;
			    	avatar = userdata.getObjectId("avatar_image_id");
			    	
			    	if (userdata.getInteger("admin") != null && userdata.getInteger("admin") == 1)
	    				admin = 1;

			    	Document searchquery = new Document("username",username);
					Document newdata = new Document().append("$set", new Document().append("status", 1));
					
					table.updateOne(searchquery, newdata);
			    	
			    	if (userdata.getString("motd") != null)
			    	{
			    		motd = userdata.getString("motd");
			    	}

			    	System.out.println("logged in..");
			    	ArrayList<Document> friends = (ArrayList<Document>) userdata.get("friends");
			    	
			    	if (friends!=null)
			    		for (Document i : friends)
			    		{
			    			searchquery = new Document().append("username", i.getString("friend_username"));
			    			Document frienddata = table.find(searchquery).first();
			    			
			    			flist.add(new FriendNode(frienddata.getString("username"),frienddata.getString("name"),
		    						i.getInteger("friend_type"),i.getString("friend_record"),frienddata.getObjectId("avatar_image_id"),
		    						loadAvatar(frienddata.getObjectId("avatar_image_id"))));
		    				
			    			if (i.getString("friend_record") != null) 
			    				flist.getLast().record = i.getString("friend_record");
			    			if (frienddata.getString("motd") != null)
			    				flist.getLast().motd = frienddata.getString("motd");
			    		}
			    	
			    	ArrayList<Document> groups = (ArrayList<Document>) userdata.get("group_records");
			    	
			    	if (groups!=null)
			    		for (Document i : groups)
			    		{
			    			Document recordsearch = new Document();
	                		recordsearch.append("_id", i.getObjectId("record_id"));
	                		
	                		Document recorddata = recordtable.find(recordsearch).first();
			    			
	                		glist.add(new GroupNode(recorddata.getObjectId("_id"),recorddata.getString("chat_name")));	
			    		}

			    	FXMLLoader loader = new FXMLLoader(getClass().getResource("Sample.fxml"));
					loader.setController(this);
					BorderPane newpane = (BorderPane) loader.load();
					
					Scene scene = new Scene(newpane,700,500);
					scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
					Stage stage = (Stage) LoginButton.getScene().getWindow();
					stage.hide();
					stage.setScene(scene);
					
					
					mainScreenUsernameLabel.setText(username);
					mainScreenNameLabel.setText(name);
					
					if (motd != null && !motd.equals(""))
					{
						mainScreenMOTDLabel.setText(motd);
					}
					else
					{
						mainScreenMOTDLabel.setText("สามารถตั้งค่าข้อความได้ที่ปุ่มตั้งค่า");
					}
					
					if (username.equals("kraiwich") || admin == 1)
			    	{
			    		adminButton.setVisible(true);
			    	}

					mainScreenAvatarImage.setImage(loadAvatar(avatar));
	
					//subscribe to yourself..
					Main.oppubsub.registerListener(syslisten);
					Main.oppubsub.subscribe(username);
					
					Main.chatoppubsub.registerListener(norlisten);
					
					//start service
					SysMessageService(Main.oppubsub);
					normalMessageService(Main.chatoppubsub);
	
					//publish to check partner status..
					sysmessageq.offer(new Message (username,"systemmessage "+username+" checkifsomeonehere"));
					
					//start rendering node..
					for (FriendNode i : flist)
					{
						UpdateFriendUI(i);
						Main.oppubsub.subscribe(i.id);
						if (i.record != null)
							Main.chatoppubsub.subscribe(i.record);
						if (i.type != 0)		
							sysmessageq.offer(new Message (i.id,"systemmessage "+username+" checkifsomeonehere"));
					}
					
					for (GroupNode i : glist)
					{
						UpdateGroupUI(i);
						Main.chatoppubsub.subscribe(i.record.toString());
					}
					
					stage.show();
			    }
			}
			catch (MongoTimeoutException e) 
			{
				System.out.println("mongodb timeout");
			}
			catch (NoSuchElementException e) 
			{
				loginPassWarnLabel.setText("ไม่พบบัญชีนี้!");
		    	loginPassWarnLabel.setVisible(true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@FXML private Button regisConfirmButton;
	@FXML private PasswordField regisPassField;
	@FXML private PasswordField conRegisPassField;
	@FXML private Label regisWarnLabel;
	
	@FXML
	private void RegisInputSend()
	{
		regisPassField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!regisPassField.getText().equals(""))
					{
	            		regisConfirmButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void RegisConInputSend()
	{
		conRegisPassField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!conRegisPassField.getText().equals(""))
					{
	            		regisConfirmButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void regisConfirmButtonAction ()
	{
		regisWarnLabel.setVisible(false);
		
		Pattern pettern = Pattern.compile("[$&+,:;=?@#|'<>.^*()%!\\/-]");
		Matcher matcher = pettern.matcher(regisPassField.getText());
		
		if (regisPassField.getText().trim().isEmpty())
		{
			regisWarnLabel.setText("คุณไม่ได้ใส่ password!");
			regisWarnLabel.setVisible(true);
		}
		else if (regisPassField.getLength() > 12 || regisPassField.getLength() < 6)
		{
			regisWarnLabel.setText("password ต้องมีความยาว 6-12 ตัวอักษร!");
			regisWarnLabel.setVisible(true);
		}
		else if (matcher.find(0))
		{
			regisWarnLabel.setText("password ต้องไม่มีอักขระพิเศษ!");
			regisWarnLabel.setVisible(true);
		}
		else if (!regisPassField.getText().equals(conRegisPassField.getText()))
		{
			regisWarnLabel.setText("password ไม่ตรงกัน!");
			regisWarnLabel.setVisible(true);
		}
		else
		{
			System.out.println("registering");
			
			try
			{
				MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
				MongoCollection<Document> table = db.getCollection("users");
				
				Document document = new Document();
				document.append("username", userinput);
				document.append("password", regisPassField.getText());
				document.append("name", userinput);
				document.append("avatar_image_id", new ObjectId("5af107d455fe3a4a985e231e"));
				document.append("status", 1);
				
				table.insertOne(document);
				
				System.out.println("done");
				
				MongoCollection<Document> recordtable = db.getCollection("records");

				document = new Document();
				document.append("username", userinput);		
				
				try (MongoCursor<Document> cursor = table.find(document).iterator()) 
				{
					Document userdata  = cursor.next();

					username = userdata.getString("username");
			    	name = userdata.getString("name");
			    	status = 1;
			    	avatar = userdata.getObjectId("avatar_image_id");

			    	Document searchquery = new Document("username",username);
					Document newdata = new Document().append("$set", new Document().append("status", 1));
					
					table.updateOne(searchquery, newdata);
			    	
			    	if (userdata.getString("motd") != null)
			    	{
			    		motd = userdata.getString("motd");
			    	}

			    	System.out.println("logged in..");
			    	ArrayList<Document> friends = (ArrayList<Document>) userdata.get("friends");
			    	
			    	if (friends!=null)
			    		for (Document i : friends)
			    		{
			    			searchquery = new Document().append("username", i.getString("friend_username"));
			    			Document frienddata = table.find(searchquery).first();
			    			
			    			flist.add(new FriendNode(frienddata.getString("username"),frienddata.getString("name"),
		    						i.getInteger("friend_type"),i.getString("friend_record"),frienddata.getObjectId("avatar_image_id"),
		    						loadAvatar(frienddata.getObjectId("avatar_image_id"))));
		    				
			    			if (i.getString("friend_record") != null) 
			    				flist.getLast().record = i.getString("friend_record");
			    			if (frienddata.getString("motd") != null)
			    				flist.getLast().motd = frienddata.getString("motd");
			    		}
			    	
			    	ArrayList<Document> groups = (ArrayList<Document>) userdata.get("group_records");
			    	
			    	if (groups!=null)
			    		for (Document i : groups)
			    		{
			    			Document recordsearch = new Document();
	                		recordsearch.append("_id", i.getObjectId("record_id"));
	                		
	                		Document recorddata = recordtable.find(recordsearch).first();
			    			
	                		glist.add(new GroupNode(recorddata.getObjectId("_id"),recorddata.getString("chat_name")));
	                		
			    		}

			    	FXMLLoader loader = new FXMLLoader(getClass().getResource("Sample.fxml"));
					loader.setController(this);
					BorderPane newpane = (BorderPane) loader.load();
					
					Scene scene = new Scene(newpane,700,500);
					scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
					Stage stage = (Stage) regisConfirmButton.getScene().getWindow();
					stage.hide();
					stage.setScene(scene);
					
					mainScreenUsernameLabel.setText(username);
					mainScreenNameLabel.setText(name);
					
					if (motd != null && !motd.equals(""))
					{
						mainScreenMOTDLabel.setText(motd);
					}
					else
					{
						mainScreenMOTDLabel.setText("สามารถตั้งค่าข้อความได้ที่ปุ่มตั้งค่า");
					}		
					
					if (username.equals("kraiwich"))
			    	{
			    		adminButton.setVisible(true);
			    	}

					mainScreenAvatarImage.setImage(loadAvatar(avatar));
	
					//subscribe to yourself..
					Main.oppubsub.registerListener(syslisten);
					Main.oppubsub.subscribe(username);
					
					Main.chatoppubsub.registerListener(norlisten);
					
					//start service
					SysMessageService(Main.oppubsub);
					normalMessageService(Main.chatoppubsub);
	
					//publish to check partner status..
					sysmessageq.offer(new Message (username,"systemmessage "+username+" checkifsomeonehere"));
					
					//start rendering node..
					for (FriendNode i : flist)
					{
						UpdateFriendUI(i);
						Main.oppubsub.subscribe(i.id);
						if (i.record != null)
							Main.chatoppubsub.subscribe(i.record);
						if (i.type != 0)		
							sysmessageq.offer(new Message (i.id,"systemmessage "+username+" checkifsomeonehere"));
					}
					
					for (GroupNode i : glist)
					{
						UpdateGroupUI(i);
						Main.chatoppubsub.subscribe(i.record.toString());
					}
					stage.show();
				}
				catch (MongoTimeoutException e) 
				{
					System.out.println("mongodb timeout");
				}
				catch (NoSuchElementException e) 
				{
					regisWarnLabel.setText("ไม่พบบัญชีนี้!");
					regisWarnLabel.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			catch (Exception e)
			{
				System.out.println("Register exception: "+ e );
			}
		}
	}

	@FXML private Button regisBackButton;
	
	@FXML
	private void regisBackButtonAction ()
	{
		try
		{
			System.out.println("load login screen");
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
			BorderPane newpane = (BorderPane) loader.load();
			
			Scene scene = new Scene(newpane,250,350);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			Stage stage = (Stage) regisBackButton.getScene().getWindow();
			stage.setScene(scene);
		}
		catch (Exception e)
		{
			System.out.println("Register back exception: "+ e );
		}
		
	}
	
	@FXML private Button SearchButton;
	@FXML private TextField SearchTextField;
	
	@FXML private Label SearchboxNameLabel;
	@FXML private Label SearchboxUsernameLabel;
	@FXML private ImageView SearchboxImage;
	
	Document searchdata;
	
	@FXML
	private void searchEnter()
	{
		SearchTextField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!SearchTextField.getText().equals(""))
					{
	            		SearchButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void SearchButtonAction ()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		if(!SearchTextField.getText().trim().equals(""))
		{
			Document document = new Document();
			document.append("username", SearchTextField.getText());
			
			
			try (MongoCursor<Document> cursor = table.find(document).iterator()) 
			{
				searchdata = cursor.next();
				
				FXMLLoader loader = new FXMLLoader(getClass().getResource("friendadddialog.fxml"));
				loader.setController(this);
				
				BorderPane boxpane = (BorderPane) loader.load();
				Scene scene = new Scene(boxpane,300,150);
				scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
				Stage stage = new Stage();
				stage.setScene(scene);
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.initStyle(StageStyle.UNDECORATED);
				
				SearchboxNameLabel.setText(searchdata.getString("name"));
				SearchboxUsernameLabel.setText(searchdata.getString("username"));
				
				SearchboxImage.setImage(loadAvatar(searchdata.getObjectId("avatar_image_id")));
				
				stage.show();
			}
			catch (MongoTimeoutException e) 
			{
				System.out.println("mongodb timeout");
			}
			catch (NoSuchElementException e) 
			{
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("No user data!");
				alert.setHeaderText(null);
				alert.setContentText("ไม่พบข้อมูลผู้ใช้ที่ค้นหา!");

				alert.showAndWait();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			SearchTextField.setText("");
		}
	}
	
	@FXML private Button addFriendButton;
	
	@FXML
	private void addFriendButtonAction ()
	{
		if(searchdata.getString("username").equals(username)) {
			System.out.println("you can't be friend with yourself!"); return; }
		try
		{
			MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
			MongoCollection<Document> table = db.getCollection("users");
			
			Document friendpattern = new Document();
			friendpattern.append("$elemMatch", new Document ().append("friend_username", username));

			Document searchquery = new Document();
			searchquery.append("username", searchdata.getString("username"));
			searchquery.append("friends", friendpattern);
			
			Document frienddata = table.find(searchquery).first();

			if (frienddata != null)
			{
				System.out.println("duplicate or already be friend with this person!");
			}
			else
			{
				//update friend
				Document searchquery2 = new Document();
				searchquery2.append("username", searchdata.getString("username"));
				
				Document updatedoc2 = new Document();
				updatedoc2.append("friend_username", username);
				updatedoc2.append("friend_type", 0);
				
				Document updatedocfriend = new Document();
				updatedocfriend.append("friends", updatedoc2);
				
				Document updatedoc3 = new Document();
				updatedoc3.append("$push", updatedocfriend);
				
				table.updateOne(searchquery2, updatedoc3);
				
				//update you
				searchquery2 = new Document();
				searchquery2.append("username", username);
				
				updatedoc2 = new Document();
				updatedoc2.append("friend_username", searchdata.getString("username"));
				updatedoc2.append("friend_type", 2);
				
				updatedocfriend = new Document();
				updatedocfriend.append("friends", updatedoc2);
				
				updatedoc3 = new Document();
				updatedoc3.append("$push", updatedocfriend);
				
				table.updateOne(searchquery2, updatedoc3);
				
				flist.add(new FriendNode(searchdata.getString("username"),0));
				Main.oppubsub.subscribe(searchdata.getString("username"));
				sysmessageq.offer(new Message (searchdata.getString("username"),"systemmessage "+username+" addfriend"));
				
				System.out.println("success!");
			}			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@FXML private Button searchCloseButton;
	
	@FXML
	private void searchCloseButtonAction()
	{
		Stage stage = (Stage) searchCloseButton.getScene().getWindow();
		stage.close();
	}
	
	//listener
	SubListener syslisten = new SubListener() {
		@Override
		public void onChannelMessage(String channel, String message)  {
			String[] command = message.split(" ");
			if (command.length > 2)
			{
				if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("checkifsomeonehere"))
				{
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]) && i.status == 0)
						{
							i.status = 1;
							sysmessageq.offer(new Message (command[1],"systemmessage "+username+" checkifsomeonehere"));
							if (i.type != 0 || i.type != 2)
								UpdateFriendUI(i);
							System.out.println(command[1]+" online!");
						}		
					}
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("goneoffline"))
				{
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]) && i.status == 1)
						{
							i.status = 0;
							System.out.println(command[1]+" gone offline!");
							
							if (i.type != 0 || i.type != 2)
								UpdateFriendUI(i);
						}		
					}
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("addfriend"))
				{
					
					boolean found = false;
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]))
						{
							found = true;
							updateFriendData(i);
							UpdateFriendUI(i);
						}
					}
					
					if (!found)
					{
						FriendNode newnode = new FriendNode (command[1],0);
						flist.add(newnode);
						updateFriendData(newnode);
						UpdateFriendUI(newnode);
					}

					System.out.println(command[1]+" want to be your friend!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("friendaccepted"))
				{
					boolean found = false;
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]))
						{
							found = true;
							i.type = 1;
							updateFriendData(i);
							UpdateFriendUI(i);
							sysmessageq.offer(new Message (command[1],"systemmessage "+username+" friendacceptedreturn"));
						}
					}
					
					if (!found)
					{
						FriendNode newnode = new FriendNode (command[1],1);
						flist.add(newnode);
						updateFriendData(newnode);
						UpdateFriendUI(newnode);
						sysmessageq.offer(new Message (command[1],"systemmessage "+username+" friendacceptedreturn"));
					}

					System.out.println(command[1]+" accepted your friend request!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("friendacceptedreturn"))
				{
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]))
						{
							i.type = 1;
							sysmessageq.offer(new Message (command[1],"systemmessage "+username+" checkifsomeonehere"));
							updateFriendData(i);
							UpdateFriendUI(i);
						}
					}
					System.out.println(command[1]+" return accepted!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("reloadfrienddata"))
				{
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]))
						{
							updateFriendData(i);
							UpdateFriendUI(i);
						}
					}
					System.out.println(command[1]+" update request!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("updatenodedata"))
				{
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]))
						{
							updateFriendData(i);
							UpdateNodeData(i);
						}
					}
					System.out.println(command[1]+" node update request!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("deletefriendnode"))
				{
					for (FriendNode i : flist)
					{
						if (i.id.equals(command[1]))
						{	
							Tab tabtodelete = null;
							for (Tab j : chattabpane.getTabs())
							{
								if (j.getProperties().get("record").equals(i.record))
								{
									tabtodelete = j;				
								}
							}
							Deletetab(tabtodelete);
							
							for (Node j : offlineBox.getChildren())
							{
								if (j.getId().equals(i.id))
								{
									DeleteFriendNode(i,j);
									break;
								}
							}
							
							for (Node j : onlineBox.getChildren())
							{
								if (j.getId().equals(i.id))
								{
									DeleteFriendNode(i,j);
									break;
								}
							}
						}
					}
					System.out.println(command[1]+" node delete request!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("reloadgroupdata"))
				{
					boolean found = false;
					for (GroupNode i : glist)
					{
						if (i.record.toString().equals(command[1]))
						{
							updateGroupData(i);
							found = true;
							break;
						}
					}
					
					if(!found)
					{
						GroupNode newgroup = new GroupNode(new ObjectId(command[1]));
						glist.add(newgroup);
						updateGroupData(newgroup);
					}
					
					System.out.println(command[1]+" update group request!");
				}
				else if (!username.equals(command[1]) && channel.equals(username) && command[0].equals("systemmessage") && command[2].equals("forcelogout"))
				{
					System.out.println("logout now!, "+channel);
					ForceLogOut();
				}
			}	
		}

		@Override
		public void onPatternMessage(String pattern, String channel, String message) {
			// TODO Auto-generated method stub
			
		}
	};
	
	SubListener norlisten = new SubListener() {
		@Override
		public void onChannelMessage(String channel, String message)  {
			String[] stringcut = message.split("(:spliter:)");
			if (!stringcut[1].equals(username))
			{
				if(stringcut[0].equals("(n)"))
				{
					boolean gettab = TabCheck(channel,stringcut[1]);;
					if(gettab)
					{
						for (FriendNode i :flist)
						{
							if(i.record.equals(channel))
							{
								UpdateChat(channel,stringcut[2],i.avatar);
								break;
							}
						}
					}
				}
				else if(stringcut[0].equals("(g)"))
				{
					UpdateGroupChat(channel,stringcut[2],stringcut[1],false);
				}
			}			
		}

		@Override
		public void onPatternMessage(String pattern, String channel, String message) {
			// TODO Auto-generated method stub
			
		}
	};
	
	//Publish Service
	private void SysMessageService(OptimizedPubSub ps){
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
 
        scheduler.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						Platform.runLater(new Runnable() {
							@Override
							@FXML
							public void run() {
								Stage stage = (Stage) CloseButton.getScene().getWindow();
								if (stage == null || (!stage.isShowing() && sysmessageq.isEmpty()))
								{
									SetLogoutStatus();
									scheduler.shutdown();
								}			
								
								if (!sysmessageq.isEmpty())
								{
									ps.publish(sysmessageq.peek().reciever, sysmessageq.poll().message);
								}
							}
						});
					}
				}, 
                1, 
        1, TimeUnit.MILLISECONDS);  
    }
	
	private void normalMessageService(OptimizedPubSub ps){
        final ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
 
        scheduler2.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						Platform.runLater(new Runnable() {
							@Override
							@FXML
							public void run() {
								Stage stage = (Stage) CloseButton.getScene().getWindow();
								if (stage == null || (!stage.isShowing() && sysmessageq.isEmpty()))
								{
									scheduler2.shutdown();
								}
									
								
								if (!normessageq.isEmpty())
								{
									ps.publish(normessageq.peek().reciever, normessageq.poll().message);
								}
							}
						});
					}
				}, 
                1, 
        1, TimeUnit.MILLISECONDS);  
    }
	
	@FXML private VBox friendReBox;
	@FXML private VBox offlineBox;
	@FXML private VBox onlineBox;
	@FXML private TabPane chattabpane;
	
	//updatefriendlistUI
	@FXML
	private void UpdateFriendUI (FriendNode friend)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		BorderPane node = new BorderPane();
    	node.setPrefSize(221, 50);
    	node.setStyle("-fx-background-color: #762323;");
    	node.setId(friend.id);
    	node.setPadding(new Insets(5,5,5,5));
    	
    	HBox nodebox = new HBox();
    	nodebox.setSpacing(5);
    	
    	VBox namebox = new VBox();
    	namebox.setSpacing(5);
    	
    	ImageView favatar = new ImageView(); 
    	favatar.setFitWidth(40);
    	favatar.setFitHeight(40);
    	favatar.setImage(loadAvatar(friend.avatarid));
    	
    	Label flab = new Label(friend.name);
    	flab.setTextFill(Color.web("#FFFFFF"));
    	flab.setFont(new Font(14));
    	
    	namebox.getChildren().add(flab);
    	
    	Label fmotd = new Label();
    	if (friend.motd != null)
    	{
    		fmotd.setText(friend.motd);;
    	}
    	else
    	{
    		fmotd.setText("");;
    	}
    	
    	fmotd.setTextFill(Color.web("#FFFFFF"));
    	namebox.getChildren().add(fmotd);

    	nodebox.getChildren().addAll(favatar,namebox);
		
		if (friend.type == 0)
		{
			if (friend.nodeappear == false)
			{
				friend.nodeappear = true;
				
				Platform.runLater(new Runnable() {
					@Override
					@FXML
					public void run() {
						
				    	Button addbutton = new Button();
				    	addbutton.setText("รับ");
				    	
				    	addbutton.setOnAction (new EventHandler<ActionEvent>
				    	() {
							@Override
							public void handle(ActionEvent e) {
								// Set friend (you) type to 1
								Document friendpattern = new Document();
								friendpattern.append("$elemMatch", new Document ().append("friend_username", friend.id));

								Document searchquery = new Document();
								searchquery.append("username", username);
								searchquery.append("friends", friendpattern);
								
								Document updatedoc = new Document();
								updatedoc.append("friends.$.friend_type", 1);
								
								Document updatedoc2 = new Document();
								updatedoc2.append("$set", updatedoc);
								
								table.updateOne(searchquery,updatedoc2);

								//Set friend (her/him) type to 1
								Document myfriendpattern = new Document();
								myfriendpattern.append("$elemMatch", new Document ().append("friend_username", username));

								Document searchquery2 = new Document();
								searchquery2.append("username", friend.id);
								searchquery2.append("friends", myfriendpattern);
								
								if (table.find(searchquery2).first() != null)
								{
									table.updateOne(searchquery2,updatedoc2);
								}
								else
								{
									Document myfriendupdatedoc2 = new Document();
									myfriendupdatedoc2.append("friend_username", username);
									myfriendupdatedoc2.append("friend_type", 1);
									
									Document updatedocfriend = new Document();
									updatedocfriend.append("friends", myfriendupdatedoc2);
									
									Document updatedoc3 = new Document();
									updatedoc3.append("$push", updatedocfriend);
									
									table.updateOne(new Document().append("username", friend.id), updatedoc3);
								}
								
								friend.nodeappear = false;

								//Update pane
								friendReBox.getChildren().remove(node);
								friendReBox.setPrefHeight(friendReBox.getPrefHeight()-50);
								
								Main.oppubsub.subscribe(friend.id);
								updateFriendData(friend);
								UpdateFriendUI(friend);
								sysmessageq.offer(new Message (friend.id,"systemmessage "+username+" friendaccepted"));
							}	
				    	});
				    	
				    	Button rejectbutton = new Button();
				    	rejectbutton.setText("ปฏิเสธ");
				    	
				    	rejectbutton.setOnAction (new EventHandler<ActionEvent>
				    	() {
							@Override
							public void handle(ActionEvent e) {
								// TODO Auto-generated method stub
								
								//delete friend data
								Document pullpattern = new Document();
								pullpattern.append("friend_username", friend.id);

								Document searchquery = new Document();
								searchquery.append("username", username);
								
								Document updatedoc = new Document().append("friends", pullpattern);
								
								Document updatedoc2 = new Document();
								updatedoc2.append("$pull", updatedoc);
								
								table.updateOne(searchquery,updatedoc2);
								
								//delete your data
								pullpattern = new Document();
								pullpattern.append("friend_username", username);

								searchquery = new Document();
								searchquery.append("username", friend.id);
								
								updatedoc = new Document().append("friends", pullpattern);
								
								updatedoc2 = new Document();
								updatedoc2.append("$pull", updatedoc);
								
								table.updateOne(searchquery,updatedoc2);
								
								for (FriendNode i : flist)
								{
									if (i.id.equals(friend.id))
									{
										flist.remove(i);
									}
								}
								
								friendReBox.getChildren().remove(node);
								friendReBox.setPrefHeight(friendReBox.getPrefHeight()-50);
							}	
				    	});
				    	
				    	HBox buttonbox = new HBox();
				    	buttonbox.setAlignment(Pos.CENTER_RIGHT);
				    	
				    	buttonbox.setSpacing(3);
				    	
				    	buttonbox.getChildren().add(addbutton);
				    	buttonbox.getChildren().add(rejectbutton);
				    	
				    	node.setOnMouseEntered(new EventHandler<MouseEvent>
				    	() {
				    	        @Override
				    	        public void handle(MouseEvent t) {
				    	        	node.setStyle("-fx-background-color:#a52e2e;");

							    	BorderPane.setAlignment(buttonbox,Pos.CENTER_RIGHT);
							    	node.setRight(buttonbox);
				    	        }
				    	});
				    	
				    	node.setOnMouseExited(new EventHandler<MouseEvent>
				    	() {
				    	        @Override
				    	        public void handle(MouseEvent t) {
				    	        	node.setStyle("-fx-background-color:#762323;");
				    	        	node.getChildren().remove(buttonbox);
				    	        }
				    	});

				    	node.setCenter(nodebox);

				    	friendReBox.getChildren().add(node);
				    	friendReBox.setPrefHeight(friendReBox.getPrefHeight()+50);
					}
				});
			}
		}
		else if (friend.type == 1)
		{
			Platform.runLater(new Runnable() {
				@Override
				@FXML
				public void run() {
					ContextMenu menu = new ContextMenu();
					Menu groupmenu = new Menu("เพิ่มลงในกลุ่ม");								
					MenuItem deletefriend = new MenuItem("ลบเพื่อน");
					
					menu.setOnHidden(new EventHandler<WindowEvent>()
					{
						@Override
						public void handle(WindowEvent arg0) {
							// TODO Auto-generated method stub
							groupmenu.getItems().clear();
						}
					});
					
					deletefriend.setOnAction(new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(ActionEvent arg0) {
							// TODO Auto-generated method stub
							//delete friend data
							Document pullpattern = new Document();
							pullpattern.append("friend_username", friend.id);

							Document searchquery = new Document();
							searchquery.append("username", username);
							
							Document updatedoc = new Document().append("friends", pullpattern);
							
							Document updatedoc2 = new Document();
							updatedoc2.append("$pull", updatedoc);
							
							table.updateOne(searchquery,updatedoc2);
							
							//delete your data
							pullpattern = new Document();
							pullpattern.append("friend_username", username);

							searchquery = new Document();
							searchquery.append("username", friend.id);
							
							updatedoc = new Document().append("friends", pullpattern);
							
							updatedoc2 = new Document();
							updatedoc2.append("$pull", updatedoc);
							
							table.updateOne(searchquery,updatedoc2);
							
							Tab tabtodelete = null;
							for (Tab i : chattabpane.getTabs())
							{
								if (i.getProperties().get("record").equals(friend.record))
								{
									tabtodelete = i;
								}
							}
							Deletetab(tabtodelete);
							
							DeleteFriendNode(friend,node);
		
							sysmessageq.offer(new Message(friend.id,"systemmessage "+username+" deletefriendnode"));
						}					
					});		
					
					menu.getItems().addAll(groupmenu,deletefriend);
					
			    	node.setOnMouseEntered(new EventHandler<MouseEvent>
			    	() {
			    	        @Override
			    	        public void handle(MouseEvent t) {
			    	        	node.setStyle("-fx-background-color:#a52e2e;");
			    	        }
			    	});
			    	
			    	node.setOnMouseExited(new EventHandler<MouseEvent>
			    	() {
			    	        @Override
			    	        public void handle(MouseEvent t) {
			    	        	node.setStyle("-fx-background-color:#762323;");
			    	        }
			    	});
			    	
			    	node.setOnMouseClicked(new EventHandler<MouseEvent>()
			    	{
						@Override
						public void handle(MouseEvent event) {
							// TODO Auto-generated method stub
							 if(event.getButton().equals(MouseButton.PRIMARY))
							 {
						           if(event.getClickCount() == 2)
						           {
						        	   boolean found = false;
						            	for (Tab i : chattabpane.getTabs())
						            	{
						            		if (i.getProperties().get("record").equals(friend.record))
						            		{
						            			found = true;
						            			chattabpane.getSelectionModel().select(i);
						            		}
						            	}    
						            	
						            	if (!found)
						            	{
						            		for (FriendNode i : flist)
						        			{
						        				if (i.id.equals(friend.id))
						        				{
						        					if (i.record == null)
						        					{
						        						MongoCollection<Document> recordtable = db.getCollection("records");
								        				//new record
								        				Document newrecord = new Document();
								        				recordtable.insertOne(newrecord);
								        				
								        				Object objectId = newrecord.get("_id");

								        				//update friend record in my list
														Document friendpattern = new Document();
									        			friendpattern.append("$elemMatch", new Document ().append("friend_username", friend.id));

									        			Document searchquery = new Document();
									        			searchquery.append("username", username);
									        			searchquery.append("friends", friendpattern);
									        			
									        			Document friendrecord = new Document();
								        				friendrecord.append("friends.$.friend_record",objectId.toString());
														
														Document updatedoc = new Document();
														updatedoc.append("$set", friendrecord);
														
														table.updateOne(searchquery,updatedoc);
														
														i.record = objectId.toString();
														
														Main.chatoppubsub.subscribe(i.record);
														
														System.out.println("friend record updated (you)");
														
								        				//update friend record in friend list
														friendpattern = new Document();
									        			friendpattern.append("$elemMatch", new Document ().append("friend_username", username));
									        			
														searchquery = new Document();
														searchquery.append("username", friend.id);
									        			searchquery.append("friends", friendpattern);
									        										        			
									        			updatedoc = new Document();
														updatedoc.append("$set", friendrecord);
														
														table.updateOne(searchquery,updatedoc);
														
														sysmessageq.offer(new Message(i.id,"systemmessage "+username+" reloadfrienddata"));

										                System.out.println("friend record updated (friend)");
						        					}
						        					//insert tab
						        					InsertFriendTab(i,null);
						        				}
						        			}
						            	}
						           }
							 }
							 else if (event.getButton().equals(MouseButton.SECONDARY))
							 {
								 MongoCollection<Document> recordtable = db.getCollection("records");
								 
								 menu.show(node, event.getScreenX(), event.getScreenY());
								 
								 for (GroupNode i : glist)
								 {
									 Document searchquery = new Document().append("_id", i.record);
									 
									 Document recorddata = recordtable.find(searchquery).first();
									 
									 ArrayList<Document> persons = (ArrayList<Document>) recorddata.get("persons");
									 
									 boolean found = false;
									 
									 for (Document j : persons)
									 {
										 if (j.getString("person_usr").equals(friend.id))
										 {
											 found = true;
											 break;
										 }
									 }
									 
									 if (!found)
									 {
										 MenuItem invitetogroup = new MenuItem(i.name);
										 
										 invitetogroup.setOnAction(new EventHandler<ActionEvent> ()
										 {
											@Override
											public void handle(ActionEvent arg0) {
												
												// TODO Auto-generated method stub
												
												//update your group record
												Object objectId = i.record;
												
												Document searchquery = new Document();
												searchquery.append("username", friend.id);
												
												Document recordid = new Document();
												recordid.append("record_id", objectId);
												
												Document groups = new Document();
												groups.append("group_records", recordid);
												
												Document updatedoc = new Document();
												updatedoc.append("$push", groups);
												
												table.updateOne(searchquery, updatedoc);
												
												//updaterecord
												searchquery = new Document();
												searchquery.append("_id", objectId);
												
												Document personid = new Document();
												personid.append("person_usr", friend.id);
												
												Document persons = new Document();
												persons.append("persons", personid);
												
												updatedoc = new Document();
												updatedoc.append("$push", persons);
												
												recordtable.updateOne(searchquery, updatedoc);
												
												sysmessageq.offer(new Message(friend.id,"systemmessage "+i.record.toString()+" reloadgroupdata"));
											}
													 
										 });
										 groupmenu.getItems().add(invitetogroup);
									 }
								 }
							 }
						}		
			    	});
			    	
			    	node.setCenter(nodebox);
					
					if (friend.nodeappear == false)
					{
						friend.nodeappear = true;
						
						if (friend.status == 0)
						{	
					    	offlineBox.getChildren().add(node);
					    	offlineBox.setPrefHeight(offlineBox.getPrefHeight()+50);
					    	
						}
						else if (friend.status == 1)
						{						
					    	onlineBox.getChildren().add(node);
					    	onlineBox.setPrefHeight(onlineBox.getPrefHeight()+50);
						}
					}
					else
					{
						if (friend.status == 0)
						{
							for (Node i : onlineBox.getChildren())
							{
								if (i.getId().equals(friend.id))
								{
									Platform.runLater(new Runnable() {
										@Override
										@FXML
										public void run() {
											offlineBox.getChildren().add(i);
											offlineBox.setPrefHeight(offlineBox.getPrefHeight()+50);
											
											onlineBox.getChildren().remove(i);
											onlineBox.setPrefHeight(onlineBox.getPrefHeight()-50);
										}
									});
								}
							}
						}
						else if (friend.status == 1)
						{
							for (Node i : offlineBox.getChildren())
							{
								if (i.getId().equals(friend.id))
								{
									Platform.runLater(new Runnable() {
										@Override
										@FXML
										public void run() {
											onlineBox.getChildren().add(i);
											onlineBox.setPrefHeight(onlineBox.getPrefHeight()+50);
											
											offlineBox.getChildren().remove(i);
											offlineBox.setPrefHeight(offlineBox.getPrefHeight()-50);
										}
									});
								}
							}
						}
					}
				}
			});			
		}
	}
	
	public void UpdateChat(String record,String word,Image avatar)
	{
		for (Tab i : chattabpane.getTabs())
		{
			if (i.getProperties().get("record").equals(record))
			{
				Label text = new Label (word);
				text.setWrapText(true);
				text.setPadding( new Insets(10,10,10,10));
				text.setTextFill(Color.web("#000000"));
				text.setStyle("-fx-background-color:#ffffff;");
            	
				ImageView img = new ImageView(avatar);
				img.setFitWidth(35);
				img.setFitHeight(35);
				img.setCache(true);
				img.setSmooth(true);

            	HBox chatnode = new HBox();
            	chatnode.setSpacing(10);
            	VBox.setVgrow(chatnode, Priority.ALWAYS);
            	chatnode.setPadding( new Insets(10,10,10,10));

            	chatnode.getChildren().add(img);
            	chatnode.getChildren().add(text);
            	chatnode.setAlignment(Pos.TOP_LEFT);
            	
            	Platform.runLater(new Runnable() {
					@Override
					@FXML
					public void run() {
						
						BorderPane paneintab = (BorderPane) i.getContent();
		            	
		            	ScrollPane scrollpane = (ScrollPane) paneintab.getChildren().get(0);
		            	
		            	BorderPane chatpane = (BorderPane) scrollpane.getContent();
		            	
		            	VBox chatvbox = (VBox) chatpane.getChildren().get(0);
		            	
		            	chatvbox.getChildren().add(chatnode);
					}
            	});
			}
		}
	}
	
	public void UpdateOldChat(String record,String word,String sender)
	{
		for (Tab i : chattabpane.getTabs())
		{
			if (i.getProperties().get("record").equals(record))
			{
				Label text = new Label (word);
				text.setWrapText(true);
				text.setPadding( new Insets(10,10,10,10));
				text.setTextFill(Color.web("#000000"));
				text.setStyle("-fx-background-color:#ffffff;");
            	
            	HBox chatnode = new HBox();
            	chatnode.setSpacing(10);
            	VBox.setVgrow(chatnode, Priority.ALWAYS);
            	chatnode.setPadding( new Insets(10,10,10,10));
            	
            	if (!username.equals(sender))
            	{
            		for (FriendNode j : flist)
            		{
            			if(j.id.equals(sender))
            			{
            				ImageView img = new ImageView(j.avatar);
            				img.setFitWidth(35);
            				img.setFitHeight(35);
            				img.setCache(true);
            				img.setSmooth(true);
            				
            				chatnode.getChildren().add(img);
            			}
            		}
            		chatnode.setAlignment(Pos.TOP_LEFT);
            	}
            	else
            	{
            		chatnode.setAlignment(Pos.TOP_RIGHT);
            	}
            	
            	chatnode.getChildren().add(text);
            	
            	Platform.runLater(new Runnable() {
					@Override
					@FXML
					public void run() {
						
						BorderPane paneintab = (BorderPane) i.getContent();
		            	
		            	ScrollPane scrollpane = (ScrollPane) paneintab.getChildren().get(0);
		            	
		            	BorderPane chatpane = (BorderPane) scrollpane.getContent();
		            	
		            	VBox chatvbox = (VBox) chatpane.getChildren().get(0);
		            	
		            	chatvbox.getChildren().add(0,chatnode);	
					}
            	});
			}
		}
	}

	private void updateFriendData(FriendNode friend)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		Document searchquery = new Document();
		searchquery.append("username", username);
		
		MongoCursor<Document> cursor = table.find(searchquery).iterator();
		ArrayList<Document> friends = (ArrayList<Document>) cursor.next().get("friends");
		
		for (FriendNode i : flist)
		{
			if (friend.id.equals(i.id))
			{
				for (Document j : friends)
				{
					if (j.getString("friend_username").equals(friend.id))	
					{
						if (j.getString("friend_record") != null)		
						{
							i.record = j.getString("friend_record");
							Main.chatoppubsub.subscribe(i.record);
							System.out.println("record updated!");
						}
						i.type = j.getInteger("friend_type");
					}	
				}
				
				searchquery = new Document().append("username", friend.id);
				Document frienddata = table.find(searchquery).first();
				
				i.avatarid = frienddata.getObjectId("avatar_image_id");
				i.name = frienddata.getString("name");
				i.motd = frienddata.getString("motd");
				i.avatar = loadAvatar(frienddata.getObjectId("avatar_image_id"));
			}
		}

	}
	
	static Image loadAvatar(ObjectId obj)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		GridFSBucket gridFSFilesBucket = GridFSBuckets.create(db, "image");
		
		try
		{
			File temp = File.createTempFile("avatar", ".tmp"); 
	    	FileOutputStream streamToDownloadTo = new FileOutputStream(temp);
	    	gridFSFilesBucket.downloadToStream(obj, streamToDownloadTo);
	        streamToDownloadTo.close();
	        
	        temp.deleteOnExit();
	        
	        BufferedImage avatar = ImageIO.read(temp);
	        Image avatarimg = SwingFXUtils.toFXImage(avatar, null);
	        return avatarimg;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;	
	}
	
	@FXML private Button settingButton;
	@FXML private ImageView settingAvatarView;

	@FXML
	private void settingButtonAction ()
	{
		try
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Setting.fxml"));
			loader.setController(this);
			BorderPane settingpane = (BorderPane) loader.load();
			Scene scene = new Scene(settingpane,560,330);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			Stage stage = new Stage();
			stage.setScene(scene);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initStyle(StageStyle.UNDECORATED);
			stage.show();
			
			settingAvatarView.setImage(loadAvatar(avatar));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@FXML private StackPane settingCloseButton;
	
	@FXML
	private void settingCloseButtonAction ()
	{
		Stage stage = (Stage) settingCloseButton.getScene().getWindow();
		stage.close();
	}
	
	@FXML private Button settingSaveButton;
	@FXML private TextField settingNameField;
	@FXML private TextField settingMOTDField;
	
	@FXML
	private void settingSaveButtonAction ()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		Document searchquery = new Document();
		searchquery.append("username", username);
		

		if(!settingNameField.getText().equals(name))
		{
			if (settingNameField.getLength() > 20)
			{
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("ความยาวเกิน!");
				alert.setHeaderText(null);
				alert.setContentText("ความยาวไม่ควรเกิน 20 ตัวอักษร!");

				alert.showAndWait();
			}
			else if (settingNameField.getText().equals(""))
			{
				Document newdata = new Document().append("$set", new Document().append("name", username));
				name = username;
				
				table.updateOne(searchquery, newdata);
			}
			else
			{
				Document newdata = new Document().append("$set", new Document().append("name", settingNameField.getText()));
				name = settingNameField.getText();
				
				table.updateOne(searchquery, newdata);
			}	
		}
		
		if(!settingMOTDField.getText().equals(motd))
		{
			if (settingMOTDField.getLength() > 20)
			{
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("ความยาวเกิน!");
				alert.setHeaderText(null);
				alert.setContentText("ความยาวไม่ควรเกิน 20 ตัวอักษร!");

				alert.showAndWait();
			}
			else
			{
				Document newdata = new Document().append("$set", new Document().append("motd", settingMOTDField.getText()));
				table.updateOne(searchquery, newdata);
				
				if (settingMOTDField.getText().equals(""))
					mainScreenMOTDLabel.setText("สามารถตั้งค่าข้อความได้ที่ปุ่มตั้งค่า");
				else
					mainScreenMOTDLabel.setText(settingMOTDField.getText());
				
				motd = settingMOTDField.getText();
			}
		}
		
		if (imagechange)
		{
			try 
			{	
				String path = settingAvatarView.getImage().getUrl();
				InputStream streamToUploadFrom = new FileInputStream(path.split("(file:/)")[1]);
				GridFSBucket gridFSFilesBucket = GridFSBuckets.create(db, "image");
				GridFSUploadOptions options = new GridFSUploadOptions()
						.chunkSizeBytes(358400)
						.metadata(new Document("type", "image"));
				ObjectId fileId = gridFSFilesBucket.uploadFromStream("default-image", streamToUploadFrom, options);
				
				Document newdata = new Document().append("$set", new Document().append("avatar_image_id", fileId));
				
				table.updateOne(searchquery, newdata);
				avatar = fileId;
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
			imagechange = false;
		}
		
		for (FriendNode i : flist)
		{
			sysmessageq.offer(new Message(i.id,"systemmessage "+username+" updatenodedata"));
		}
		
		updateMyData();
	}
	
	@FXML Button selectImageButton;
	
	@FXML
	private void selectImageButtonAction()
	{
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.png", "*.bmp", "*.jpg", ".jpeg", ".gif"));
		File selectfield = fc.showOpenDialog(null);
		
		if (selectfield!=null)
		{
			Image newimg = new Image(selectfield.toURI().toString(),150,150,false,false);
			
			settingAvatarView.setImage(newimg);
		}	
		
		imagechange = true;
	}
	
	@FXML
	private void updateMyData ()
	{
		mainScreenNameLabel.setText(name);
		
		if (motd != null && !motd.equals(""))
		{
			mainScreenMOTDLabel.setText(motd);
		}
		else
		{
			mainScreenMOTDLabel.setText("สามารถตั้งค่าข้อความได้ที่ปุ่มตั้งค่า");
		}
		
		mainScreenAvatarImage.setImage(loadAvatar(avatar));
	}
	
	@FXML
	private void UpdateNodeData(FriendNode friend)
	{
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				if (friend.type == 0)
				{
					for (Node i : friendReBox.getChildren())
					{
						if (i.getId().equals(friend.id))
						{
							if (i instanceof BorderPane)
							{
								HBox nodebox = (HBox) ((BorderPane) i).getChildren().get(0);
								VBox chatnodebox = (VBox) nodebox.getChildren().get(1);
								Label name = (Label) chatnodebox.getChildren().get(0);
								name.setText(friend.name);
								
								Label motd = (Label) chatnodebox.getChildren().get(1);
								motd.setText(friend.motd);
								
								ImageView imgbox = (ImageView)  nodebox.getChildren().get(0);
								imgbox.setImage(loadAvatar(friend.avatarid));
								imgbox.setSmooth(true);
								imgbox.setCache(true);
							}

						}
					}
				}
				else if (friend.type == 1)
				{
					for (Node i : offlineBox.getChildren())
					{
						if (i.getId().equals(friend.id))
						{
							if (i instanceof BorderPane)
							{
								HBox nodebox = (HBox) ((BorderPane) i).getChildren().get(0);
								VBox chatnodebox = (VBox) nodebox.getChildren().get(1);
								Label name = (Label) chatnodebox.getChildren().get(0);
								name.setText(friend.name);
								
								Label motd = (Label) chatnodebox.getChildren().get(1);
								motd.setText(friend.motd);
								
								ImageView imgbox = (ImageView)  nodebox.getChildren().get(0);
								imgbox.setImage(loadAvatar(friend.avatarid));
								imgbox.setSmooth(true);
								imgbox.setCache(true);
							}						
						}
					}
					
					for (Node i : onlineBox.getChildren())
					{
						if (i.getId().equals(friend.id))
						{
							if (i instanceof BorderPane)
							{
								HBox nodebox = (HBox) ((BorderPane) i).getChildren().get(0);
								VBox chatnodebox = (VBox) nodebox.getChildren().get(1);
								Label name = (Label) chatnodebox.getChildren().get(0);
								name.setText(friend.name);
								
								Label motd = (Label) chatnodebox.getChildren().get(1);
								motd.setText(friend.motd);
								
								ImageView imgbox = (ImageView)  nodebox.getChildren().get(0);
								imgbox.setImage(loadAvatar(friend.avatarid));
								imgbox.setSmooth(true);
								imgbox.setCache(true);
							}
						}
					}
				}
			}
		});	
	}
	
	public void InsertFriendTab(FriendNode friend,String message)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> recordtable = db.getCollection("records");
		
		Document searchquery = new Document().append("_id", new ObjectId(friend.record));	
		Document oldchatdata = recordtable.find(searchquery).first();
			
		ArrayList<Document> chatnodes = (ArrayList<Document>) oldchatdata.get("chat_records");
		
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				
				Tab chattab = new Tab();
		        chattab.setText(friend.name+" ("+friend.id+")");
		        chattab.getProperties().put("record", friend.record);
		        
		        BorderPane paneintab = new BorderPane();
		        paneintab.setStyle("-fx-background-color:#303030;");
		        ScrollPane chatviewpane = new ScrollPane();
		        
		        VBox.setVgrow(chatviewpane, Priority.ALWAYS);
		        BorderPane.setMargin(chatviewpane, new Insets(0,0,10,0));
		        chatviewpane.setFitToWidth(true);
		        
		        BorderPane borderpaneinchatviewpane = new BorderPane();
		        borderpaneinchatviewpane.setStyle("-fx-background-color:#303030;");
		        chatviewpane.setContent(borderpaneinchatviewpane);

		        VBox vboxinscroll = new VBox();
		        
		        vboxinscroll.heightProperty().addListener(observable -> {
		        	if(chatviewpane.vvalueProperty().get() > 0.7)
		        		chatviewpane.setVvalue(1D);
		        });
		     
		        borderpaneinchatviewpane.setCenter(vboxinscroll);  
 
		        chatviewpane.vvalueProperty().addListener(observable -> {
		        	if(chatviewpane.vvalueProperty().get() == 0.0)
		        	{
		        		System.out.println("top of the tab");
		        		
		        		if(chatnodes != null)
						{
							if(chatnodes.size()>5)
							{
								for (int i=0;i<=5;i++)
								{
									UpdateOldChat(friend.record, chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"));
									chatnodes.remove(chatnodes.size()-1);
								}
							}
							else
							{
								while(!chatnodes.isEmpty())
								{
									UpdateOldChat(friend.record, chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"));
									chatnodes.remove(chatnodes.size()-1);
								}
							}
							
						}
		        	}
		        });
		        
		        HBox chatinsertpanel = new HBox();
		        chatinsertpanel.setSpacing(5);
		        chatinsertpanel.setPadding(new Insets(5,5,5,5));
		        
		        TextField chatfield = new TextField();
		        HBox.setHgrow(chatfield, Priority.ALWAYS);
		        
		        chatfield.setOnKeyPressed(new EventHandler<KeyEvent>()
		        {
		            @Override
		            public void handle(KeyEvent ke)
		            {
		                if (ke.getCode().equals(KeyCode.ENTER))
		                {
		                	if (!chatfield.getText().equals(""))
							{
								Label text = new Label (chatfield.getText());
								text.setWrapText(true);
								text.setPadding( new Insets(10,10,10,10));
								text.setTextFill(Color.web("#000000"));
								text.setStyle("-fx-background-color:#ffffff;");
			                	
			                	HBox chatnode = new HBox();
			                	VBox.setVgrow(chatnode, Priority.ALWAYS);
			                	chatnode.setPadding( new Insets(10,10,10,10));
			                	
			                	chatnode.getChildren().add(text);
			                	chatnode.setAlignment(Pos.CENTER_RIGHT);
			                	
			                	try											                	
			                	{
			                		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
			                		MongoCollection<Document> recordtable = db.getCollection("records");
			                		
			                		boolean found = false;
			                		for (FriendNode i : flist)
			                		{
			                			if (i.id.equals(friend.id) && i.record != null)
			                			{
			                				found = true;
				                			break;
			                			}
			                		}
			                		
			                		if(found)
			                		{
			                			normessageq.offer(new Message(friend.record,"(n):spliter:"+username+":spliter:"+text.getText()));
			                			
			                			Document recordsearch = new Document();
				                		recordsearch.append("_id", new ObjectId(friend.record));
				                		
				                		Document record = new Document();
				                		record.append("chat_record", text.getText());
				                		record.append("chat_sendusr", username);
				                		
				                		Document chatrecord = new Document();
				                		chatrecord.append("chat_records", record);
				        				
				        				Document updatechatrecord = new Document();
				        				updatechatrecord.append("$push", chatrecord);
				        				
				        				recordtable.updateOne(recordsearch, updatechatrecord);

				                		Platform.runLater(new Runnable() {
						    				@Override
						    				@FXML
						    				public void run() {
						    					vboxinscroll.getChildren().add(chatnode);
						    				}
				                		});
			                		}
			                		else
			                		{
			                			System.out.println("you are not friend with this person anymore!");
			                		}
			                	}
			                	catch (Exception e)
			                	{
			                		e.printStackTrace();
			                	}									                	
							}
		                	chatfield.setText("");
		                	chatviewpane.setVvalue(1D);
		                }
		            }
		        });
		        
		        Button sendbutton = new Button();
		        sendbutton.setText("ส่ง");
		          
		        chatinsertpanel.getChildren().add(chatfield);
		        chatinsertpanel.getChildren().add(sendbutton);

		        paneintab.setCenter(chatviewpane);
		        paneintab.setBottom(chatinsertpanel);
		        
		        chattab.setContent(paneintab);
		        
		        chattabpane.getTabs().add(chattab); 
		        chattabpane.getSelectionModel().select(chattab);
		        
		        sendbutton.setOnAction (new EventHandler<ActionEvent>
		    	() {
					@Override
					public void handle(ActionEvent arg0) {
						if (!chatfield.getText().equals(""))
						{
							Label text = new Label (chatfield.getText());
							text.setWrapText(true);
							text.setPadding( new Insets(10,10,10,10));
							text.setTextFill(Color.web("#000000"));
							text.setStyle("-fx-background-color:#ffffff;");
		                	
		                	HBox chatnode = new HBox();
		                	VBox.setVgrow(chatnode, Priority.ALWAYS);
		                	chatnode.setPadding( new Insets(10,10,10,10));
		                	
		                	chatnode.getChildren().add(text);
		                	chatnode.setAlignment(Pos.CENTER_RIGHT);
		                	
		                	try											                	
		                	{
		                		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		                		MongoCollection<Document> recordtable = db.getCollection("records");
		                		
		                		boolean found = false;
		                		for (FriendNode i : flist)
		                		{
		                			if (i.id.equals(friend.id) && i.record != null)
		                			{
		                				found = true;
			                			break;
		                			}
		                		}
		                		
		                		if(found)
		                		{
		                			normessageq.offer(new Message(friend.record,"(n):spliter:"+username+":spliter:"+text.getText()));
		                			
		                			Document recordsearch = new Document();
			                		recordsearch.append("_id", new ObjectId(friend.record));
			                		
			                		Document record = new Document();
			                		record.append("chat_record", text.getText());
			                		record.append("chat_sendusr", username);
			                		
			                		Document chatrecord = new Document();
			                		chatrecord.append("chat_records", record);
			        				
			        				Document updatechatrecord = new Document();
			        				updatechatrecord.append("$push", chatrecord);
			        				
			        				recordtable.updateOne(recordsearch, updatechatrecord);

			                		Platform.runLater(new Runnable() {
					    				@Override
					    				@FXML
					    				public void run() {
					    					vboxinscroll.getChildren().add(chatnode);
					    				}
			                		});
		                		}
		                		else
		                		{
		                			System.out.println("you are not friend with this person anymore!");
		                		}
		                	}
		                	catch (Exception e)
		                	{
		                		e.printStackTrace();
		                	}									                	
						}
						chatfield.setText("");
						chatviewpane.setVvalue(1D);
					}						                	
		        });
	
				if(chatnodes != null)
				{
					if(chatnodes.size()>20)
					{
						for (int i=0;i<20;i++)
						{
							UpdateOldChat(friend.record, chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"));
							chatnodes.remove(chatnodes.size()-1);
						}
					}
					else
					{
						while(!chatnodes.isEmpty())
						{
							UpdateOldChat(friend.record, chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"));
							chatnodes.remove(chatnodes.size()-1);
						}
					}
					
				}
				chatviewpane.setVvalue(1D);
			}
		});
	}
	
	public void InsertGroupTab(GroupNode group)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> recordtable = db.getCollection("records");
		
		Document searchquery = new Document().append("_id", group.record);	
		Document oldchatdata = recordtable.find(searchquery).first();
			
		ArrayList<Document> chatnodes = (ArrayList<Document>) oldchatdata.get("chat_records");
		
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				
				Tab chattab = new Tab();
		        chattab.setText(group.name);
		        chattab.getProperties().put("record", group.record.toString());
		        
		        BorderPane paneintab = new BorderPane();
		        paneintab.setStyle("-fx-background-color:#303030;");

		        ScrollPane chatviewpane = new ScrollPane();
		        VBox.setVgrow(chatviewpane, Priority.ALWAYS);
		        BorderPane.setMargin(chatviewpane, new Insets(0,0,10,0));
		        chatviewpane.setFitToWidth(true);
		        
		        BorderPane borderpaneinchatviewpane = new BorderPane();
		        borderpaneinchatviewpane.setStyle("-fx-background-color:#303030;");
		        
		        chatviewpane.setContent(borderpaneinchatviewpane);

		        VBox vboxinscroll = new VBox();
		        
		        vboxinscroll.heightProperty().addListener(observable -> {
		        	if(chatviewpane.vvalueProperty().get() > 0.8)
		        		chatviewpane.setVvalue(1D);
		        });
		     
		        borderpaneinchatviewpane.setCenter(vboxinscroll);  
 
		        chatviewpane.vvalueProperty().addListener(observable -> {
		        	if(chatviewpane.vvalueProperty().get() == 0.0)
		        	{
		        		System.out.println("top of the tab");
		        		
		        		if(chatnodes != null)
						{
							if(chatnodes.size()>5)
							{
								for (int i=0;i<=5;i++)
								{
									UpdateGroupChat(group.record.toString(), chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"),true);
									chatnodes.remove(chatnodes.size()-1);
								}
							}
							else
							{
								while(!chatnodes.isEmpty())
								{
									UpdateGroupChat(group.record.toString(), chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"),true);
									chatnodes.remove(chatnodes.size()-1);
								}
							}
							
						}
		        	}
		        });
		        
		        HBox chatinsertpanel = new HBox();
		        chatinsertpanel.setSpacing(5);
		        
		        TextField chatfield = new TextField();
		        HBox.setHgrow(chatfield, Priority.ALWAYS);
		        
		        chatfield.setOnKeyPressed(new EventHandler<KeyEvent>()
		        {
		            @Override
		            public void handle(KeyEvent ke)
		            {
		                if (ke.getCode().equals(KeyCode.ENTER))
		                {
		                	if (!chatfield.getText().equals(""))
							{
								Label text = new Label (chatfield.getText());
								text.setWrapText(true);
								text.setPadding( new Insets(10,10,10,10));
								text.setTextFill(Color.web("#000000"));
								text.setStyle("-fx-background-color:#ffffff;");
			                	
			                	HBox chatnode = new HBox();
			                	VBox.setVgrow(chatnode, Priority.ALWAYS);
			                	chatnode.setPadding( new Insets(10,10,10,10));
			                	
			                	chatnode.getChildren().add(text);
			                	chatnode.setAlignment(Pos.CENTER_RIGHT);
			                	
			                	try											                	
			                	{
			                		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
			                		MongoCollection<Document> recordtable = db.getCollection("records");
			                		
			                		boolean found = false;
			                		for (GroupNode i : glist)
			                		{
			                			if (i.record.toString().equals(chattab.getProperties().get("record")))
			                			{
			                				found = true;
				                			break;
			                			}
			                		}
			                		
			                		if(found)
			                		{
			                			normessageq.offer(new Message(group.record.toString(),"(g):spliter:"+username+":spliter:"+text.getText()));
			                			
			                			Document recordsearch = new Document();
				                		recordsearch.append("_id", group.record);
				                		
				                		Document record = new Document();
				                		record.append("chat_record", text.getText());
				                		record.append("chat_sendusr", username);
				                		
				                		Document chatrecord = new Document();
				                		chatrecord.append("chat_records", record);
				        				
				        				Document updatechatrecord = new Document();
				        				updatechatrecord.append("$push", chatrecord);
				        				
				        				recordtable.updateOne(recordsearch, updatechatrecord);

				                		Platform.runLater(new Runnable() {
						    				@Override
						    				@FXML
						    				public void run() {
						    					vboxinscroll.getChildren().add(chatnode);
						    				}
				                		});
			                		}
			                		else
			                		{
			                			System.out.println("you are not in this group anymore!");
			                		}
			                	}
			                	catch (Exception e)
			                	{
			                		e.printStackTrace();
			                	}									                	
							}
							chatfield.setText("");
							chatviewpane.setVvalue(1D);
		                }
		            }
		        });
		        
		        Button sendbutton = new Button();
		        sendbutton.setText("ส่ง");
		          
		        chatinsertpanel.getChildren().add(chatfield);
		        chatinsertpanel.getChildren().add(sendbutton);

		        paneintab.setCenter(chatviewpane);
		        paneintab.setBottom(chatinsertpanel);
		        
		        chattab.setContent(paneintab);
		        
		        chattabpane.getTabs().add(chattab); 
		        
		        chattabpane.getSelectionModel().select(chattab);
		        
		        sendbutton.setOnAction (new EventHandler<ActionEvent>
		    	() {
					@Override
					public void handle(ActionEvent arg0) {
						if (!chatfield.getText().equals(""))
						{
							Label text = new Label (chatfield.getText());
							text.setWrapText(true);
							text.setPadding( new Insets(10,10,10,10));
							text.setTextFill(Color.web("#000000"));
							text.setStyle("-fx-background-color:#ffffff;");
		                	
		                	HBox chatnode = new HBox();
		                	VBox.setVgrow(chatnode, Priority.ALWAYS);
		                	chatnode.setPadding( new Insets(10,10,10,10));
		                	
		                	chatnode.getChildren().add(text);
		                	chatnode.setAlignment(Pos.CENTER_RIGHT);
		                	
		                	try											                	
		                	{
		                		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		                		MongoCollection<Document> recordtable = db.getCollection("records");
		                		
		                		boolean found = false;
		                		for (GroupNode i : glist)
		                		{
		                			if (i.record.toString().equals(chattab.getProperties().get("record")))
		                			{
		                				found = true;
			                			break;
		                			}
		                		}
		                		
		                		if(found)
		                		{
		                			normessageq.offer(new Message(group.record.toString(),"(g):spliter:"+username+":spliter:"+text.getText()));
		                			
		                			Document recordsearch = new Document();
			                		recordsearch.append("_id", group.record);
			                		
			                		Document record = new Document();
			                		record.append("chat_record", text.getText());
			                		record.append("chat_sendusr", username);
			                		
			                		Document chatrecord = new Document();
			                		chatrecord.append("chat_records", record);
			        				
			        				Document updatechatrecord = new Document();
			        				updatechatrecord.append("$push", chatrecord);
			        				
			        				recordtable.updateOne(recordsearch, updatechatrecord);

			                		Platform.runLater(new Runnable() {
					    				@Override
					    				@FXML
					    				public void run() {
					    					vboxinscroll.getChildren().add(chatnode);
					    				}
			                		});
		                		}
		                		else
		                		{
		                			System.out.println("you are not in this group anymore!");
		                		}
		                	}
		                	catch (Exception e)
		                	{
		                		e.printStackTrace();
		                	}									                	
						}
						chatfield.setText("");
						chatviewpane.setVvalue(1D);
					}	
		        });
	
				if(chatnodes != null)
				{
					if(chatnodes.size()>20)
					{
						for (int i=0;i<20;i++)
						{
							UpdateGroupChat(group.record.toString(), chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"),true);
							chatnodes.remove(chatnodes.size()-1);
						}
					}
					else
					{
						while(!chatnodes.isEmpty())
						{
							UpdateGroupChat(group.record.toString(), chatnodes.get(chatnodes.size()-1).getString("chat_record"), chatnodes.get(chatnodes.size()-1).getString("chat_sendusr"),true);
							chatnodes.remove(chatnodes.size()-1);
						}
					}
					
				}
				chatviewpane.setVvalue(1D);
			}
		});
	}
	
	private boolean TabCheck (String record,String message)
	{
		boolean found = false;
				
		for (Tab i : chattabpane.getTabs())
		{
			if (i.getProperties().get("record").equals(record))
			{
				found = true;
				return true;
			}		
		}

		if (!found)
		{
			for (FriendNode i : flist)
			{
				if (i.record.equals(record))
				{
					InsertFriendTab(i,message);
				}
			}
		}
		
		return false;
	}
	
	private void Deletetab(Tab tab)
	{
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				if (tab != null)
				{
					chattabpane.getTabs().remove(tab);
				}
			}
		});
			
	}
	
	private void DeleteFriendNode(FriendNode friend, Node node)
	{
		flist.remove(friend);	
		
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				
				
				VBox parent = (VBox) node.getParent();
				parent.getChildren().remove(node);
				parent.setPrefHeight(parent.getPrefHeight()-50);

			}
		});
	}
	
	@FXML Button createGroupButton;
	@FXML TextField createGroupField;
	
	@FXML
	private void createGroupEnter()
	{
		createGroupField.setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent ke)
	        {
	            if (ke.getCode().equals(KeyCode.ENTER))
	            {
	            	if (!createGroupField.getText().equals(""))
					{
	            		createGroupButtonAction();
					}
	            }
	        }
	    });
	}
	
	@FXML
	private void createGroupButtonAction()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		MongoCollection<Document> recordtable = db.getCollection("records");
		
		if (createGroupField.getText() != null)
		{
			//new record
			Document newrecord = new Document();
			newrecord.append("chat_name", createGroupField.getText());
			
			List<Document> persons = new ArrayList<>();
			persons.add(new Document().append("person_usr", username));
			
			newrecord.append("persons", persons);
			
			recordtable.insertOne(newrecord);
			
			Object objectId = newrecord.get("_id");
			
			//update your group record
			Document searchquery = new Document();
			searchquery.append("username", username);
			
			Document recordid = new Document();
			recordid.append("record_id", objectId);
			
			Document groups = new Document();
			groups.append("group_records", recordid);
			
			Document updatedoc = new Document();
			updatedoc.append("$push", groups);
			
			table.updateOne(searchquery, updatedoc);
			
			GroupNode newgroup = new GroupNode((ObjectId) objectId,createGroupField.getText());
			glist.add(newgroup);
			Main.chatoppubsub.subscribe(newgroup.record.toString());
			UpdateGroupUI(newgroup);
			createGroupField.setText("");
		}
	}
	
	@FXML VBox groupVBox;
	
	@FXML
	private void UpdateGroupUI(GroupNode group)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> recordtable = db.getCollection("records");
		MongoCollection<Document> table = db.getCollection("users");
		
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				BorderPane node = new BorderPane();
		    	node.setPrefSize(221, 50);
		    	node.setStyle("-fx-background-color: #762323;");
		    	node.setId(group.record.toString());
		    	node.setPadding(new Insets(5,5,5,5));
		    	
		    	Label glab = new Label(group.name);
		    	glab.setTextFill(Color.web("#FFFFFF"));
		    	glab.setFont(new Font(16));
		    	glab.setAlignment(Pos.CENTER);
		    	
		    	node.setLeft(glab);
		    	
		    	ContextMenu menu = new ContextMenu();							
				MenuItem groupexit = new MenuItem("ออกจากกลุ่ม");
				
				menu.getItems().add(groupexit);
				
				groupexit.setOnAction(new EventHandler<ActionEvent>()
				{
					@Override
					public void handle(ActionEvent arg0) {
						Document searchquery = new Document();
						searchquery.append("username", username);
						
						Document pullpattern = new Document();
						pullpattern.append("record_id", group.record);
						
						Document updatedoc = new Document().append("group_records", pullpattern);
						
						Document updatedoc2 = new Document();
						updatedoc2.append("$pull", updatedoc);
						
						table.updateOne(searchquery, updatedoc2);	
						
						searchquery = new Document();
						searchquery.append("_id", group.record);
						
						Document recorddata = recordtable.find(searchquery).first();
						
						ArrayList<Document> persons = (ArrayList<Document>) recorddata.get("persons");
						
						if (persons.size() == 1)
						{
							recordtable.findOneAndDelete(searchquery);
						}
						else
						{
							pullpattern = new Document();
							pullpattern.append("person_usr", username);
							
							updatedoc = new Document().append("persons", pullpattern);
							
							updatedoc2 = new Document();
							updatedoc2.append("$pull", updatedoc);
							
							recordtable.updateOne(searchquery, updatedoc2);	
						}
						
						DeleteGroupNode(group,node);
					}
				});
		    	
		    	node.setOnMouseEntered(new EventHandler<MouseEvent>
		    	() {
		    	        @Override
		    	        public void handle(MouseEvent t) {
		    	        	node.setStyle("-fx-background-color:#a52e2e;");
		    	        }
		    	});
		    	
		    	node.setOnMouseExited(new EventHandler<MouseEvent>
		    	() {
		    	        @Override
		    	        public void handle(MouseEvent t) {
		    	        	node.setStyle("-fx-background-color:#762323;");
		    	        }
		    	});
		    	
		    	node.setOnMouseClicked(new EventHandler<MouseEvent>()
		    	{
					@Override
					public void handle(MouseEvent event) {
						// TODO Auto-generated method stub
						 if(event.getButton().equals(MouseButton.PRIMARY))
						 {
					           if(event.getClickCount() == 2)
					           {
					        	   boolean found = false;
					            	for (Tab i : chattabpane.getTabs())
					            	{
					            		if (i.getProperties().get("record").equals(group.record.toString()))
					            		{
					            			found = true;
					            			chattabpane.getSelectionModel().select(i);
					            		}
					            	}    
					            	
					            	if (!found)
					            	{
					            		InsertGroupTab(group);
					            	}
					           }
						 }
						 else if (event.getButton().equals(MouseButton.SECONDARY))
						 {
							 menu.show(node, event.getScreenX(), event.getScreenY());
						 }
					}		
		    	});
	
		    	groupVBox.getChildren().add(node);
		    	groupVBox.setPrefHeight(groupVBox.getPrefHeight()+50);
			}
		});
	}
	
	public void UpdateGroupChat(String record,String word,String sender,boolean old)
	{
		
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		for (Tab i : chattabpane.getTabs())
		{
			if (i.getProperties().get("record").equals(record))
			{
				Label text = new Label (word);
				text.setWrapText(true);
				text.setPadding( new Insets(10,10,10,10));
				text.setTextFill(Color.web("#000000"));
				text.setStyle("-fx-background-color:#ffffff;");
            	
				VBox chatnode = new VBox();
            	HBox chatsection = new HBox();
            	chatsection.setSpacing(10);
            	VBox.setVgrow(chatsection, Priority.ALWAYS);
            	chatsection.setPadding( new Insets(5,10,10,10));
            	
            	
            	if (!username.equals(sender))
            	{
            		Document searchquery = new Document().append("username", sender); 		
            		Document persondata = table.find(searchquery).first();
            		
            		Label nametext = new Label (persondata.getString("name"));
            		nametext.setPadding( new Insets(0,0,0,10));
            		nametext.setTextFill(Color.web("#ffffff"));
            		chatnode.getChildren().add(nametext);

            		ImageView img = new ImageView(loadAvatar(persondata.getObjectId("avatar_image_id")));
    				img.setFitWidth(35);
    				img.setFitHeight(35);
    				img.setCache(true);
    				img.setSmooth(true);
    				
    				chatsection.getChildren().add(img);
    				chatsection.setAlignment(Pos.TOP_LEFT);
            	}
            	else
            	{
            		chatsection.setAlignment(Pos.TOP_RIGHT);
            	}
            	
            	chatsection.getChildren().add(text);
            	chatnode.getChildren().add(chatsection);
            	
            	Platform.runLater(new Runnable() {
					@Override
					@FXML
					public void run() {
						
						BorderPane paneintab = (BorderPane) i.getContent();
		            	
		            	ScrollPane scrollpane = (ScrollPane) paneintab.getChildren().get(0);
		            	
		            	BorderPane chatpane = (BorderPane) scrollpane.getContent();
		            	
		            	VBox chatvbox = (VBox) chatpane.getChildren().get(0);
		            	
		            	if(old)
		            		chatvbox.getChildren().add(0,chatnode);
		            	else
		            		chatvbox.getChildren().add(chatnode);
					}	
            	});
			}
		}
	}
	
	private void updateGroupData(GroupNode group)
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		MongoCollection<Document> recordtable = db.getCollection("records");
		
		Document searchquery = new Document();
		searchquery.append("username", username);
		
		MongoCursor<Document> cursor = table.find(searchquery).iterator();
		ArrayList<Document> groups = (ArrayList<Document>) cursor.next().get("group_records");
		
		for (GroupNode i : glist)
		{
			if (group.record.toString().equals(i.record.toString()))
			{
				if (i.name != null)
				{
					for (Document j : groups)
					{
						i.name = j.getString("chat_name");
					}
				}
				else
				{
					searchquery = new Document().append("_id", group.record);
					Document recorddata = recordtable.find(searchquery).first();
					
					i.name = recorddata.getString("chat_name");
					
					Main.chatoppubsub.subscribe(i.record.toString());
					UpdateGroupUI(i);
				}
				
			}
		}
	}
	
	private void DeleteGroupNode(GroupNode group, Node node)
	{
		glist.remove(group);	
		
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				VBox parent = (VBox) node.getParent();
				parent.getChildren().remove(node);
				parent.setPrefHeight(parent.getPrefHeight()-50);
			}
		});
	}
	
	private void SetLogoutStatus()
	{
		MongoDatabase db = Main.mongo.getDatabase("Javafx_chat_data");
		MongoCollection<Document> table = db.getCollection("users");
		
		Document searchquery = new Document("username",username);
		Document newdata = new Document().append("$set", new Document().append("status", 0));
		
		table.updateOne(searchquery, newdata);
	}
	
	@FXML 
	private void adminButtonAction ()
	{
		try 
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Admin.fxml"));
			BorderPane adminPane;
			adminPane = (BorderPane) loader.load();
			
			Scene scene = new Scene(adminPane,750,500);
			Stage stage = new Stage();
			stage.setScene(scene);
			stage.initOwner(adminButton.getScene().getWindow());
			stage.initStyle(StageStyle.DECORATED);
			stage.show();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void ForceLogOut()
	{
		SetLogoutStatus();
		
		Platform.runLater(new Runnable() {
			@Override
			@FXML
			public void run() {
				try 
				{
					Stage stage = (Stage) CloseButton.getScene().getWindow();
					stage.close();
					
					FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
					BorderPane newpane;
					newpane = (BorderPane) loader.load();
					stage = new Stage();
					Scene scene = new Scene(newpane,250,350);
					scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
					stage.initStyle(StageStyle.UNDECORATED);
					stage.setScene(scene);
					stage.show();
				} 
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
}
