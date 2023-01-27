package connection;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import entities.Address;
import entities.Package;
import entities.PriceSetting;
import entities.Receiver;
import entities.Sender;
import entities.Transport;

public class DatabaseConnection 
{

	private String url = "jdbc:mysql://127.0.0.1:3306/package_management?useUnicode=true&characterEncoding=UTF-8";
    private Connection connection = null;

    public DatabaseConnection()
    {
        try
        {
            connection = DriverManager.getConnection(url, "root", "Anonymous2001");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public Object[][] testSelect()
    {
        Object[][] objects = new Object[1][3];
        List<Sender> senders = new ArrayList<>();
        List<Receiver> receivers = new ArrayList<>();
        int flag=0;
        List<Transport> transports = new ArrayList<>();
        Transport transport;
        Sender sender = new Sender(0, null, null, null);
        Receiver receiver = new Receiver(0, null, null, null);
        Package packageTransport = new Package(0, null, 0);
        Address sendFrom, sendTo;
        try 
        {
            Statement stm = connection.createStatement();
            ResultSet resultSet = stm.executeQuery(
            "SELECT transport.*,sender.name AS senderName, sender.phoneNumber AS senderPhoneNumber, senderAddress.id AS fromId"
            + ", senderAddress.location AS sendFrom, receiver.name AS receiverName, receiver.phoneNumber AS receiverPhoneNumber"
            + ", receiverAddress.id AS toId, receiverAddress.location AS sendTo, package.description "
            + "AS packageInfo, package.weight FROM transport LEFT JOIN customer AS sender "
            + "ON transport.sender = sender.id LEFT JOIN customer AS receiver ON transport.receiver = receiver.id "
            + "LEFT JOIN package ON transport.package = package.id LEFT JOIN address AS senderAddress "
            + "ON sender.address = senderAddress.id LEFT JOIN address AS receiverAddress ON receiver.address = "
            + "receiverAddress.id;");      

            while(resultSet.next())
            {
                transport = new Transport(null, null, 0, url, null, null, null, 0, null);
                sender = new Sender(Integer.parseInt(resultSet.getString("sender")), null, null, null);
                receiver = new Receiver(Integer.parseInt(resultSet.getString("receiver")), null, null, null);
                transport.setFee(Float.parseFloat(resultSet.getString("fee")));
                transport.setTransportType(resultSet.getString("transportType"));
                transport.setSendDate(resultSet.getString("sendDate"));
                transport.setReceiveDateEstimation(resultSet.getString("receiveDateEstimation"));
                transport.setStatus(resultSet.getString("status"));
                transport.setDistance(Float.parseFloat(resultSet.getString("distance")));
                packageTransport = new Package(Integer.parseInt(resultSet.getString("package")), null, 0);
                sender.setName(resultSet.getString("senderName"));
                sender.setPhoneNumber(resultSet.getString("senderPhoneNumber"));
                sendFrom = new Address(Integer.parseInt(resultSet.getString("fromId")), resultSet.getString("sendFrom"));
                sender.setAddress(sendFrom);
                receiver.setName(resultSet.getString("receiverName"));
                receiver.setPhoneNumber(resultSet.getString("receiverPhoneNumber"));
                sendTo = new Address(Integer.parseInt(resultSet.getString("toId")), resultSet.getString("sendTo"));
                receiver.setAddress(sendTo);
                packageTransport.setDescription(resultSet.getString("packageInfo"));
                packageTransport.setWeight(Float.parseFloat(resultSet.getString("weight")));
                transport.setSender(sender);
                transport.setReceiver(receiver);
                transport.setPackageTransport(packageTransport);
                transports.add(transport);

                for (Sender senderTemp : senders)
                {
                    if (sender.getId() == senderTemp.getId())
                    {
                        flag=1;
                        senderTemp.packageSend.add(packageTransport.getId());
                    }
                }
                if (flag==0)
                {
                    senders.add(sender);
                    sender.packageSend.add(packageTransport.getId());
                }
                flag = 0;
                for (Receiver receiverTemp : receivers)
                {
                    if (receiver.getId() == receiverTemp.getId())
                    {
                        flag=1;
                        receiverTemp.packageReceive.add(packageTransport.getId());
                    }
                }
                if (flag==0)
                {
                    receivers.add(receiver);
                    receiver.packageReceive.add(packageTransport.getId());
                }
                flag = 0;
            }     

            objects[0][0] = transports;
            objects[0][1] = senders;
            objects[0][2] = receivers;

            resultSet.close();
            stm.close();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return objects;
    }
	
    public void updatePrice()
    {
        try 
        {
            List<PriceSetting> priceSettings = readFile();
            String[] s = new String[5];
            int i=0;
            String[] priceSetting1 = priceSettings.get(0).getPriceSetting().split("[|]");
            String[] priceSetting2 = priceSettings.get(1).getPriceSetting().split("[|]");
            for (String sTemp : priceSetting1)
            {
                s[i++] = sTemp;
            }
            for (String sTemp : priceSetting2)
            {
                s[i++] = sTemp;
            }

            String query = "UPDATE transport "
            + "LEFT JOIN package "
            + "ON transport.package = package.id "
            + "SET fee = " + s[0] + "*distance+"
            + s[1] + "*weight "
            + "WHERE transport.transportType = 'road';";
        
            String query1 = "UPDATE transport "
            + "LEFT JOIN package "
            + "ON transport.package = package.id "
            + "SET fee = " + s[2] + "*distance+"
            + s[3] + "*weight+" + s[4] + " "
            + "WHERE transport.transportType = 'air';" ;

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement(query1);
            preparedStatement.executeUpdate();

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public void addTransport(Sender sender, Receiver receiver, Package packageTransport, Transport transport)
    {
        try 
        {
            String query = "SELECT * FROM address WHERE location = \"" + sender.getAddress().getLocation() + "\";";
            Statement stm = connection.createStatement();
            ResultSet resultSet = stm.executeQuery(query);
            PreparedStatement preparedStatement;

            // send address
            {
                if (!resultSet.isBeforeFirst())
                {
                    query = "INSERT INTO address (location) VALUES (\"" + sender.getAddress().getLocation() + "\")";
                    preparedStatement = connection.prepareStatement(query);
                    preparedStatement.executeUpdate();
                }
                
                query = "SELECT * FROM address WHERE location = \"" + sender.getAddress().getLocation() + "\";";
                resultSet = stm.executeQuery(query);
                while (resultSet.next()) 
                {
                    sender.getAddress().setId(Integer.parseInt(resultSet.getString("id")));   
                }
                System.out.println(sender.getAddress().getId());
            }

            // receive address
            {
                query = "SELECT * FROM address WHERE location = \"" + receiver.getAddress().getLocation() + "\";";
                stm = connection.createStatement();
                resultSet = stm.executeQuery(query);
                
                if (!resultSet.isBeforeFirst())
                {
                    query = "INSERT INTO address (location) VALUES (\"" + receiver.getAddress().getLocation() + "\")";
                    preparedStatement = connection.prepareStatement(query);
                    preparedStatement.executeUpdate();
                }
                
                query = "SELECT * FROM address WHERE location = \"" + receiver.getAddress().getLocation() + "\";";
                resultSet = stm.executeQuery(query);
                while (resultSet.next()) 
                {
                    receiver.getAddress().setId(Integer.parseInt(resultSet.getString("id")));   
                }
                System.out.println(receiver.getAddress().getId());
            }
            
            // sender
            {
                query = "SELECT * FROM customer WHERE name = \"" + sender.getName() + "\" && phoneNumber = \"" + sender.getPhoneNumber() + "\" "
                + "&& address = \"" + sender.getAddress().getId() + "\";";
                resultSet = stm.executeQuery(query);

                if (!resultSet.isBeforeFirst())
                {
                    query = "INSERT INTO customer(name,phoneNumber,address,role) VALUES "
                    + "(\"" + sender.getName() + "\",\"" + sender.getPhoneNumber() + "\","
                    + sender.getAddress().getId() + ",1);";
                    preparedStatement = connection.prepareStatement(query);
                    preparedStatement.executeUpdate();
                }
                
                query = "SELECT * FROM customer WHERE name = \"" + sender.getName() + "\" && phoneNumber = \"" + sender.getPhoneNumber() + "\" "
                + "&& address = \"" + sender.getAddress().getId() + "\";";
                resultSet = stm.executeQuery(query);
                while (resultSet.next())
                {
                    sender.setId(Integer.parseInt(resultSet.getString("id")));
                }
                System.out.println(sender.getId());
            }

            // receiver
            {
                query = "SELECT * FROM customer WHERE name = \"" + receiver.getName() + "\" && phoneNumber = \"" + receiver.getPhoneNumber() + "\" "
                + "&& address = \"" + receiver.getAddress().getId() + "\";";
                resultSet = stm.executeQuery(query);

                if (!resultSet.isBeforeFirst())
                {
                    query = "INSERT INTO customer(name,phoneNumber,address,role) VALUES "
                    + "(\"" + receiver.getName() + "\",\"" + receiver.getPhoneNumber() + "\","
                    + receiver.getAddress().getId() + ",1);";
                    preparedStatement = connection.prepareStatement(query);
                    preparedStatement.executeUpdate();
                }
                
                query = "SELECT * FROM customer WHERE name = \"" + receiver.getName() + "\" && phoneNumber = \"" + receiver.getPhoneNumber() + "\" "
                + "&& address = \"" + receiver.getAddress().getId() + "\";";
                resultSet = stm.executeQuery(query);
                while (resultSet.next())
                {
                    receiver.setId(Integer.parseInt(resultSet.getString("id")));
                }
                System.out.println(receiver.getId());
            }

            // package
            {
                query = "INSERT INTO package (description,weight) VALUES "
                + "(\"" + packageTransport.getDescription() + "\"," 
                + packageTransport.getWeight() +");";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.executeUpdate();

                query = "SELECT MAX(id) FROM package;";
                resultSet = stm.executeQuery(query);
                while (resultSet.next())
                {
                    packageTransport.setId(Integer.parseInt(resultSet.getString("MAX(id)")));
                }
                System.out.println(packageTransport.getId());
            }

            // transport
            {
                query = "INSERT INTO transport (sender,receiver,fee,transportType,sendDate,receiveDateEstimation,status,distance,package) "
                + "VALUES"
                + "(" + sender.getId() + "," + receiver.getId() + "," + transport.getFee() + ",\"" + transport.getTransportType() +"\",\"" 
                + transport.getSendDate() + "\",\"" + transport.getReceiveDateEstimation() + "\",\"" + transport.getStatus() + "\","
                + transport.getDistance() + "," + packageTransport.getId() + ");";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.executeUpdate();
            }

            resultSet.close();
            stm.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public List<PriceSetting> readFile()
	{
		try 
		{
			Gson gson = new Gson();
			Reader reader = Files.newBufferedReader(Paths.get("src\\pricesetting.json"));
			List<PriceSetting> priceSettings = gson.fromJson(reader, new TypeToken<List<PriceSetting>>() {}.getType());
			reader.close();
			return priceSettings;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return null;
	}
}
