package nl.wilcokas.planetherapy.worker;

public class WorkerException extends RuntimeException {

	private static final long serialVersionUID = 3213850465285480686L;
	private String message;

	public WorkerException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

}
