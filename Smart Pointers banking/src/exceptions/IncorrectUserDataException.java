package exceptions;

public class IncorrectUserDataException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6670442115685276623L;
	private String message;
	public IncorrectUserDataException(String message) {
		super(message);
		this.message=message;
	}
	
	public String getMessage() {
		return message;
	}
}
