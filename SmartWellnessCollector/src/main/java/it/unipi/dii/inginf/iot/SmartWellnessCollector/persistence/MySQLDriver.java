package it.unipi.dii.inginf.iot.SmartWellnessCollector.persistence;

import it.unipi.dii.inginf.iot.SmartWellnessCollector.model.DataSample;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQLDriver {
    private static MySQLDriver instance = null;

    private static String databaseIp;
    private static int databasePort;
    private static String databaseUsername;
    private static String databasePassword;
    private static String databaseName;

    public static MySQLDriver getInstance() {
        if(instance == null)
            instance = new MySQLDriver();

        return instance;
    }

    private MySQLDriver() {
        databaseIp = "localhost";
        databasePort = 3306;
        databaseUsername = "root";
        databasePassword = "PASSWORD";
        databaseName = "smartwellness";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp + ":" + databasePort +
                        "/" + databaseName + "?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
                databaseUsername, databasePassword);
    }


    public void insertDataSample(DataSample dataSample) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO DataSamples (node, value, manual, sensorType, timestamp) VALUES (?, ?, ?, ?, ?)")
        )
        {
            statement.setInt(1, dataSample.getNode());
            statement.setFloat(2, dataSample.getValue());
            statement.setInt(3, dataSample.getManual());
            statement.setString(4, dataSample.getSensorType());
            statement.setTimestamp(5, dataSample.getTimestamp());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }
}