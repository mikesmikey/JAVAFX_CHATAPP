package application;
	
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.netty.OptimizedPubSub;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	public static MongoClient mongo = new MongoClient("localhost", 27017);
	public static Jedis jedis = new Jedis("localhost");
	
	
	//pubusb instance (nedis api)
	public static OptimizedPubSub oppubsub = OptimizedPubSub.getInstance("localhost", 6379);
	public static OptimizedPubSub chatoppubsub = OptimizedPubSub.getInstance("localhost", 6379);
	
	@Override
	public void start(Stage primaryStage) {
		try {
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("Login.fxml"));
			Scene scene = new Scene(root,250,350);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.initStyle(StageStyle.UNDECORATED);
			primaryStage.setScene(scene);
			primaryStage.show();
			
			primaryStage.setMinHeight(350);
			primaryStage.setMinWidth(250);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
