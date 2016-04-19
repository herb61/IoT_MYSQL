
/**
 * @author Herbert Pichler
 * @version 1.1
 * @date 06.04.2016
 */
package info.vigaun.IoT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import org.glassfish.tyrus.server.Server;
import org.json.simple.JSONObject;
//logger
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
/**
 * Diese Klasse  startet den WesocketServer
 */
public class IoTServer {
    /**
     * logger Variable für das Logfile wird in der Datei sensor.log abgespeichert
     * Pattern gibt an Datum Uhrzeit Logfilter Meldung
     */
    static Logger logger = Logger.getRootLogger();
    static String filename = "sensor.log";
    static String pattern = "%d{MM.dd.yyyy\tHH:mm:ss}\t%p\t%m %n";
    static String[] sensorname = new String[10];
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException {
        

        /**
         * Fileappender erstellen und dem Logger hinzufügen
         */
        Appender fileAppender = null;
        try {
            fileAppender = new FileAppender(new PatternLayout(pattern),
                    filename, true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        logger.addAppender(fileAppender);
        logger.info("Server wurde gestartet");
        readSensorName();
        runServer();


    }
 
    /**
     * Methode startet den Server
     */
    public static void runServer() throws ClassNotFoundException {
        Server server = new Server("localhost", 33334, "/websockets",null, IoTEndpoint.class);
        try {
            server.start();
            runMenue();
            } 
        catch (DeploymentException | IOException | InterruptedException | SQLException e) {
            e.getStackTrace();
            logger.info("Fehler: "+e.toString());
            } 
        finally {
            server.stop();
        }
    } 
    
/**
 * Diese Methode zeigt ein einfaches Menü. Alle wichtigen funktionen implementiert
 * @throws IOException falsche Eingabe
 * @throws InterruptedException falsche
 */    
private static void runMenue() throws IOException, InterruptedException, SQLException, ClassNotFoundException{

while(true){
Scanner s = new Scanner(System.in);
int value = 0;
// Display menue
System.out.println("=========================================");
System.out.println("|   MENÜ AUSWAHL                        |");
System.out.println("=========================================");
System.out.println("| Auswahl:                              |");
System.out.println("|        1. Update: Alle Werte          |");
System.out.println("|        2. Update: Temperatur          |");
System.out.println("|        3. Update: Luftfeuchtigkeit    |");
System.out.println("|        4. Exit                        |");
System.out.println("|        5. Insert                      |");
System.out.println("|        6. Abfrage                     |");
System.out.println("=========================================");
System.out.println("\n Wählen Sie eine Option aus:");

//read an integer
try{
    value = s.nextInt();
}
catch (InputMismatchException e){
    System.err.println("Input error - kein gültiger Ganzzahlenwert");
            }
// Switch construct
switch (value) {
case 1:
  System.out.println("Option 1 ausgewählt: Update: Alle Werte"); 
    if(IoTEndpoint.session1 == null){
        System.out.println("Server nicht verbunden");
        Thread.sleep(2000);
        break;
  }
  else{
    //IoTEndpoint.session1.getAsyncRemote().sendText(createJSON("5"));
    IoTEndpoint.sendAll("5");
    Thread.sleep(2000);
    break;
  }
case 2:
  System.out.println("Option 2 ausgewählt: Update: Luftfeuchtigkeit"); 
  System.out.println("Bitte warten");
    if(IoTEndpoint.session1 == null){
        System.out.println("Server nicht verbunden");
        break;
  }
    else{
      IoTEndpoint.session1.getAsyncRemote().sendText(createJSON("2"));
      Thread.sleep(2000);
      break;
  }
case 3:
  System.out.println("Option 3 ausgewählt Update: Temperatur"); 
    if(IoTEndpoint.session1 == null){
        System.out.println("Server nicht verbunden");
        break;
  }
    else{
      IoTEndpoint.session1.getAsyncRemote().sendText(createJSON("3"));
      Thread.sleep(2000);
      break; 
    }
case 4:
   System.out.println("Beenden");
   logger.info("Server wurde beendet");
   setOnlineStatus();
   System.exit(1);
case 5:
   System.out.println("Insert");
   logger.info("Datenbankeintrag");
   database.insertValues(23.56, 6);
   break;
   case 6:
      database.getFromDataBase(6);
      Thread.sleep(2000);
      break; 
default:
  System.out.println("Falsche Auswahl!");
  Thread.sleep(2000);
  break; 
  }
 }     
}
/**
 * Beim Beenden des Programmes wird der Onlinestatus auf "nein" geändert
 * Zusätzlich wird die IP gelöscht
 */
private static void setOnlineStatus() throws ClassNotFoundException{
    
    for (Session s: IoTEndpoint.sessions){
    try {
      database.updateDatabase(" ","nein",Integer.parseInt((String)s.getUserProperties().get("ID")));
     } 
    catch (SQLException ex) {
       java.util.logging.Logger.getLogger(IoTEndpoint.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         }
    }
}


/**
 * Erzeugt den benötigten JSON String
 * @param value Zeichenkette für den Client mögliche Werte 1-5
 * @return JSON String
 */
 private static String createJSON(String value){
        
     JSONObject obj = new JSONObject();
        obj.put("request", value);
     return obj.toString();   
    }  
 
 private static void readSensorName(){
     
    int i = 1;
      try {
        BufferedReader br = new BufferedReader (new FileReader("sensorname.txt") );
        while( (sensorname[i] = br.readLine()) != null ) { 
        // liest zeilenweise aus Datei  
          i++;
        }
        br.close();
      }
      catch (IOException e) {
        logger.info("Fehler: "+e.toString());
      }
 }
}

