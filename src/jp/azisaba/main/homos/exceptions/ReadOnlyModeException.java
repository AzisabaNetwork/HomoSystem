package jp.azisaba.main.homos.exceptions;

public class ReadOnlyModeException extends Exception {

	private static final long serialVersionUID = 1L;

	public ReadOnlyModeException(String msg){
		super(msg);
	}
}
