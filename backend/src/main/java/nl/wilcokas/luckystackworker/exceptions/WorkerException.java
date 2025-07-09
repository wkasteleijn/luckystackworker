package nl.wilcokas.luckystackworker.exceptions;

public class WorkerException extends RuntimeException {

  private static final long serialVersionUID = 3213850465285480686L;

  public WorkerException(String message) {
    super(message);
  }
}
