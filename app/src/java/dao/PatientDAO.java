/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

/**
 *
 * @author yu.fu.2015
 */
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import model.Patient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.RESTHandler;

public class PatientDAO {

    public static boolean addPatient(Patient p) {

        boolean insertSuccess = false;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            //Opens a connection
            conn = ConnectionManager.getConnection();

            //Statement to insert information into the database: user_id, password, name, school, edollar
            pstmt = conn.prepareStatement("INSERT INTO sabai.patients "
                    + "(village_prefix, name, image, contactNo, gender, travelling_time_to_village, date_of_birth, face_encodings) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

//            System.out.println("imageLength" + fgImage.length);
            //Sets the objects retrieved from the getter methods into the variables
            pstmt.setString(1, p.getVillage());
            pstmt.setString(2, p.getName());
            pstmt.setString(3, p.getPhotoImage());
            pstmt.setString(4, p.getContactNo());
            pstmt.setString(5, p.getGender());
            pstmt.setInt(6, p.getTravellingTimeToClinic());
            pstmt.setString(7, p.getDateOfBirth());
            pstmt.setString(8, p.getFaceEncoding().toString());
            //Executes the update and stores data into database
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("we died");
                throw new SQLException("Creating user failed, no rows affected.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    p.setPatientId(generatedKeys.getInt(1));
                    insertSuccess = true;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            //Catches any possible SQL exception
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ConnectionManager.close(conn, pstmt, rs);
        }
        System.out.println("user should be in");
        return insertSuccess;
    }

    public static boolean updatePatientDetails(int patientId, String village, String name, String image, String contactNo, int travellingTime, String dateOfBirth) {

        boolean updateSuccess = false;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            //Opens a connection
            conn = ConnectionManager.getConnection();

            //Statement to insert information into the database: user_id, password, name, school, edollar
            pstmt = conn.prepareStatement("UPDATE patients SET name = ?, image = ?, contactNo = ?, travelling_time_to_village = ?, date_of_birth = ? WHERE id = ? && village_prefix = ?");

//            System.out.println("imageLength" + fgImage.length);
            //Sets the objects retrieved from the getter methods into the variables
            pstmt.setString(1, name);
            pstmt.setString(2, image);
            pstmt.setString(3, contactNo);
            pstmt.setInt(4, travellingTime);
            pstmt.setString(5, dateOfBirth);
            pstmt.setInt(6, patientId);
            pstmt.setString(7, village);

            //Executes the update and stores data into database
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            } else {
                updateSuccess = true;
            }

            //Catches any possible SQL exception
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ConnectionManager.close(conn, pstmt, rs);
        }

        return updateSuccess;
    }

    public static void updateImage(Patient p) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            //Opens a connection
            conn = ConnectionManager.getConnection();

            pstmt = conn.prepareStatement("UPDATE patients SET image = ? WHERE id = ?;");

//            System.out.println("imageLength" + fgImage.length);
            //Sets the objects retrieved from the getter methods into the variables
            pstmt.setString(1, p.getPhotoImage());
            pstmt.setInt(2, p.getPatientId());

            //Executes the update and stores data into database
            pstmt.executeUpdate();

            //Catches any possible SQL exception
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ConnectionManager.close(conn, pstmt, rs);
        }
    }

    public static JSONObject getJSONObject(String jsonString) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(jsonString);
    }

    public static Patient getPatientByFace(JSONObject faceEncoding) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnectionManager.getConnection();
            stmt = conn.prepareStatement("select * from patients where face_encodings is not null");
            rs = stmt.executeQuery();
            while (rs.next()) {
                String face_encodings = rs.getString("face_encodings");
                System.out.println("database faces");
                System.out.println(face_encodings);
                JSONObject faceJSON = getJSONObject(face_encodings);
                JSONObject compareImages = new JSONObject();
                compareImages.put("first_encoding", faceEncoding.get("encoding"));
                compareImages.put("second_encoding", faceJSON.get("encoding"));
                String postResult = null;
                System.out.println(compareImages);
                try {
                    postResult = RESTHandler.sendPostRequest(RESTHandler.facialURL + "compareimages", compareImages, null);
                } catch (IOException e) {
                    continue;
                }

                JSONObject postJSON = getJSONObject(postResult);

                if (postJSON.get("match").equals("true")) {
                    String village = rs.getString("village_prefix");
                    int patientId = rs.getInt("id");
                    String name = rs.getString("name");
                    String contactNo = rs.getString("contactNo");
                    String gender = rs.getString("gender");
                    String dateOfBirth = rs.getString("date_of_birth");
                    int travellingTimeToClinic = rs.getInt("travelling_time_to_village");
                    int parentId = rs.getInt("parent");
                    String allergy = rs.getString("drug_allergy");
                    String encoding = rs.getString("face_encodings");
                    JSONObject encodedObj = faceJSON;
                    Patient p = new Patient(village, patientId, name, contactNo, gender, dateOfBirth, travellingTimeToClinic, parentId, allergy, encodedObj);
                    p.setPhotoImage(rs.getString("image"));
                    return p;
                }
                //return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Patient getPatientByPatientID(String pVillage, int pNo) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnectionManager.getConnection();
            stmt = conn.prepareStatement("select * from patients where id = ? and village_prefix = ?");
            stmt.setInt(1, pNo);
            stmt.setString(2, pVillage);
            rs = stmt.executeQuery();

            ArrayList<Integer> allergies = new ArrayList<>();
            if (rs.next()) {
                String village = rs.getString("village_prefix");
                int patientId = rs.getInt("id");
                String name = rs.getString("name");
                String contactNo = rs.getString("contactNo");
                String gender = rs.getString("gender");
                String dateOfBirth = rs.getString("date_of_birth");
                int travellingTimeToClinic = rs.getInt("travelling_time_to_village");
                int parentId = rs.getInt("parent");
                String allergy = rs.getString("drug_allergy");
                String encoding = rs.getString("face_encodings");
                JSONObject encodedObj;
                if (encoding == null || encoding.length() == 0) {
                    encodedObj = null;
                } else {
                    encodedObj = getJSONObject(encoding);
                }

//                allergies.add(allergy);
//                while (rs.next()) {
//                    allergy = rs.getInt(7);
//                    allergies.add(allergy);
//                }
                Patient p = new Patient(village, patientId, name, contactNo, gender, dateOfBirth, travellingTimeToClinic, parentId, allergy, encodedObj);
                p.setPhotoImage(rs.getString("image"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            ConnectionManager.close(conn, stmt, rs);
        }
        return null;
    }

    Connection conn;
    ResultSet rs;
    PreparedStatement stmt;

    public static Patient getPatientByPatientID(String patientID) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnectionManager.getConnection();
            stmt = conn.prepareStatement("select * from patients where id = ?");
            stmt.setString(1, patientID);
            rs = stmt.executeQuery();

            ArrayList<Integer> allergies = new ArrayList<>();
            if (rs.next()) {
                String village = rs.getString("village_prefix");
                int patientId = rs.getInt("id");
                String name = rs.getString("name");
                String gender = rs.getString("gender");
                String dateOfBirth = rs.getString("date_of_birth");
                int parentId = rs.getInt("parent");
                String allergy = rs.getString("drug_allergy");
                String encoding = rs.getString("face_encodings");
                JSONObject encodedObj;
                if (encoding == null || encoding.length() == 0) {
                    encodedObj = null;
                } else {
                    encodedObj = getJSONObject(encoding);
                }
//                allergies.add(allergy);
//                while (rs.next()) {
//                    allergy = rs.getInt(7);
//                    allergies.add(allergy);
//                }
                Patient p = new Patient(village, patientId, name, gender, dateOfBirth, parentId, allergy, encodedObj);
                p.setPhotoImage(rs.getString("image"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            ConnectionManager.close(conn, stmt, rs);
        }
        return null;
    }

    public static Patient getPatientByPatientID(int patientID) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnectionManager.getConnection();
            stmt = conn.prepareStatement("select * from patients where id = ?");
            stmt.setInt(1, patientID);
            rs = stmt.executeQuery();

            ArrayList<Integer> allergies = new ArrayList<>();
            if (rs.next()) {
                String village = rs.getString("village_prefix");
                int patientId = rs.getInt("id");
                String name = rs.getString("name");
                String gender = rs.getString("gender");
                String dateOfBirth = rs.getString("date_of_birth");
                int parentId = rs.getInt("parent");
                String allergy = rs.getString("drug_allergy");
                String encoding = rs.getString("face_encodings");
                JSONObject encodedObj;
                if (encoding == null || encoding.length() == 0) {
                    encodedObj = null;
                } else {
                    encodedObj = getJSONObject(encoding);
                }
//                allergies.add(allergy);
//                while (rs.next()) {
//                    allergy = rs.getInt(7);
//                    allergies.add(allergy);
//                }
                Patient p = new Patient(village, patientId, name, gender, dateOfBirth, parentId, allergy, encodedObj);
                p.setPhotoImage(rs.getString("image"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            ConnectionManager.close(conn, stmt, rs);
        }
        return null;
    }

}
