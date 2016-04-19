/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.vigaun.IoT;

import static info.vigaun.IoT.IoTServer.logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.glassfish.grizzly.http.util.FastHttpDateFormat.getCurrentDate;
import org.json.simple.JSONObject;

/**
 *
 * @author Herbert
 */
public class database {
    
    private static Connection conn = null;
    
    public static void insertController(int id,String name,String ip, String location) throws ClassNotFoundException {
        try {
            Connection conn = connectToDatabase();
            Statement s = conn.createStatement();             
            /**
             * Abfrage und Test ob die ID schon vorhanden ist
             * Nach Neustart wird die ID des Controller wieder abgefragt.
             * Die ID des Controllers darf aber nur einmal vorhanden sein. Sollte dieser eingetragen sein, dann wird der Vorgang abgebrochen.
             */
            s.execute("SELECT controller_id from controller");
            ResultSet as = s.getResultSet();
            while (as.next()) {
                /**
                 * Überprüft ob der Controller schon registriert ist.
                 * Wenn Ja, dann wird ein Update der IP und Onlinestatus gemacht
                 */
                if(as.getInt("controller_id") == id){
                    updateDatabase(ip,"ja", id);
                    return; 
                }
            }
            /**
             * Vorbereitung des Insert. Muss bei Variablen sein
             */
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO controller "
                                                 + "(controller_id, name,location,ip, online) "
                                                 + "VALUES (?, ?, ?,?,?)");
            pstmt.setInt(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3,location);
            pstmt.setString(4,ip);
            pstmt.setString(5,"ja");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.info("Fehler: "+e.toString());
        }
        
    }
    /**
     * Updates die datenbank bei neuverbindung des Controllers
     * @param conn aktive Datenbankverbindung
     * @param ip neue IP-Adresse
     * @throws SQLException 
     */
    public static void updateDatabase( String ip, String active, int id) throws SQLException, ClassNotFoundException{
    Connection conn = connectToDatabase();
    Statement s = conn.createStatement(); 
    String updateTableSQL = "UPDATE controller"
                        + " SET ip = ?,online = ?"
                        + "WHERE controller_id = ?";
    PreparedStatement pstupd;
        try {
            pstupd = conn.prepareStatement(updateTableSQL);
            pstupd.setString(1,ip);
            pstupd.setString(2,active);
            pstupd.setInt(3,id);
            pstupd.executeUpdate();
        } 
        catch (SQLException e) {
            logger.info("Fehler: "+e.toString());
        }
    }
   /**
    * Fügt die vorhandenen Sensoren in die Datenbank ein.
    * Es wird geprüft ob schon Einträge vorhanden sind.
    * @param sensor Name des Sensors
    * @param c_id   Controller_ID
    * @param g_id   Group_ID (Sensor)
    * @throws SQLException 
    */
   public static void insertGroups(String sensor, int c_id,int g_id) throws SQLException, ClassNotFoundException{
       Connection conn = connectToDatabase();
       Statement s = conn.createStatement();
       String insertTable = "INSERT INTO sensor"
             + "(sensor_id,name,controller_id) VALUES"
             + " (?,?,?)";
       
       PreparedStatement pstupd;
       
        s.execute("SELECT sensor_id FROM sensor");
        ResultSet as = s.getResultSet();
            while (as.next()) {
                /**
                 * Überprüft ob die Sensoren schon registriert ist.
                 * Wenn Ja, dann wird ein Update gemacht
                 */
                if(as.getInt("sensor_id") == g_id){
                    insertTable = "UPDATE sensor "
                            + "SET sensor_id = ?,name = ?,controller_id = ? "
                            + "WHERE sensor_id = ?";
                    try {
                        pstupd = conn.prepareStatement(insertTable);
                        pstupd.setInt(1,g_id);
                        pstupd.setString(2,sensor);
                        pstupd.setInt(3,c_id);
                        pstupd.setInt(4,g_id);
                        pstupd.executeUpdate();
                        return;
                    } 
                    catch (SQLException e) {
                        logger.info("Fehler: "+e.toString());
                    }
                }
            }
       
        try {
            pstupd = conn.prepareStatement(insertTable);
            pstupd.setInt(1,g_id);
            pstupd.setString(2,sensor);
            pstupd.setInt(3,c_id);
            pstupd.executeUpdate();
        } 
        catch (SQLException e) {
            logger.info("Fehler: "+e.toString());
        }
       
   }
    
   public static void  insertValues(double values,int group_id) throws SQLException, ClassNotFoundException{
       
       Date date = new Date();
       SimpleDateFormat da = new SimpleDateFormat("dd.MM.yyyy");
       SimpleDateFormat ti = new SimpleDateFormat("hh:mm:ss");      
       Connection conn = connectToDatabase();
       Statement s = conn.createStatement();
       String insertTable = "INSERT INTO messurevalue"
               + "(mdate,mtime,value,sensor_id) VALUES"
               + "(?,?,?,?)";
       PreparedStatement pstupd;
        try {
            pstupd = conn.prepareStatement(insertTable);
            pstupd.setDate(1,getCurrentDate());
            pstupd.setString(2,getCurrentTime());
            pstupd.setDouble(3,values);
            pstupd.setInt(4,group_id);
            pstupd.executeUpdate();
        } 
        catch (SQLException e) {
           logger.info("Fehler: "+e.toString());
        }  
   }
   private static java.sql.Date getCurrentDate() {
             java.util.Date today = new java.util.Date();
    return new java.sql.Date(today.getTime());
}
   public static String getCurrentTime() {
        String time_now = "hh:mm:ss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(time_now);
   return sdf.format(cal.getTime());

}

   
   
   
   
   
   
   /**
    * Holt den aktuellen Wert des gewälten Sensors aus der Datenbank
    * @param group_id Sensor Gruppe
    * @throws SQLException Fehler bei der Abfrage
    */
   public static String getFromDataBase(int group_id) throws SQLException, ClassNotFoundException{
       JSONObject obj = new JSONObject();
       Connection conn = connectToDatabase();
       Statement s = conn.createStatement();
       String queryTable = "SELECT name,mdate,mtime,value FROM messurevalue m JOIN sensor g "
               + "ON m.sensor_id = g.sensor_id "
               + "WHERE m.sensor_id = "+group_id
               + " ORDER by VALUES_ID DESC "
               + "LIMIT 1";

       ResultSet rs = s.executeQuery(queryTable);
      while (rs.next()) {
                obj.put("name", rs.getString("name"));
                obj.put("date", rs.getDate("mdate"));
                obj.put("time", rs.getString("mtime"));
                obj.put("value",rs.getDouble("value"));
                logger.info(obj.toString());
          return obj.toString();
      }
       return "Error";
   }
   
   
   
    /**
     * Herstellung einer Datenbankverbindung
     * @return Connection Variable
     * @throws java.lang.ClassNotFoundException keine Datenbank gefunden
     * @throws java.sql.SQLException falsche Abfragesyntax
     */
    public static Connection connectToDatabase() throws ClassNotFoundException, SQLException{
     
    if(conn != null){
        return conn;
    }
            
  // JDBC Treibernameand  und Datenbak URL
   String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
   String DB_URL = "jdbc:mysql://localhost/sensor";

   //  Database credentials
   String USER = "root";
   String PASS = "raspberry";
   //String PASS = "";
    
    try {
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(DB_URL,USER,PASS); //Datenbank sensor
        //conn = DriverManager.getConnection("jdbc:sqlite:/home/pi/database/sensor.db"); //Datenbank sensor
    } 
    catch ( Exception e ) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
        return conn;
    }
}
