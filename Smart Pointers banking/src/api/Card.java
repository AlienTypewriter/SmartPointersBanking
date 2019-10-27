package api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import exceptions.IncorrectUserDataException;

public class Card {
	private String number;
	public Card(String number, String pin) throws IncorrectUserDataException {
		try {
			Server.getConnection().beginRequest();
			Statement st = Server.getConnection().createStatement();
			st.execute("SELECT active,pin FROM Cards WHERE card_number="+number);
			ResultSet res = st.getResultSet();
			if (!res.getBoolean("active")) {
				throw new IncorrectUserDataException("This card is deactivated or does not belong to this bank");
			} else if (!res.getString("pin").equals(pin)) {
				throw new IncorrectUserDataException("The pin code entered is incorrect");				
			}
			Server.getConnection().endRequest();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.number = number;
	}
	
	public void transfer(String cardNumber, double amount) throws IncorrectUserDataException {
		try {
			Server.getConnection().beginRequest();
			Statement st = Server.getConnection().createStatement();
			st.execute("SELECT balance FROM Cards WHERE card_number="+number);
			ResultSet res = st.getResultSet();
			if (res.getDouble("balance")<amount) {
				throw new IncorrectUserDataException("You do not have enough money to complete this transfer");
			}
			st.execute("SELECT balance,active FROM Cards WHERE card_number="+cardNumber);
			res = st.getResultSet();
			if (!res.getBoolean("active")) {
				throw new IncorrectUserDataException("The entered card is deactivated or does not belong to this bank");				
			}
			st.addBatch("UPDATE Cards SET balance=balance-"+amount+" WHERE card_number="+number);
			st.addBatch("UPDATE Cards SET balance=balance+"+amount+" WHERE card_number="+cardNumber);
			st.executeBatch();
			Server.getConnection().endRequest();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void changeAmount(double amount) throws IncorrectUserDataException {
		try {
			Server.getConnection().beginRequest();
			Statement st = Server.getConnection().createStatement();
			st.execute("SELECT balance FROM Cards WHERE card_number="+number);
			ResultSet res = st.getResultSet();
			if (res.getDouble("balance")+amount<0) {
				throw new IncorrectUserDataException("You do not have enough money to complete this withdrawal");
			}
			st.execute("UPDATE Cards SET balance=balance+"+amount+" WHERE card_number="+number);
			Server.getConnection().endRequest();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
