package it.smartcommunitylab.aac.model;

public class ErrorInfo {
    
    private String url;
    private String exception;
    private String className;
    private int lineNumber;
     
    public ErrorInfo(String url, String exception, String className, int lineNumber) {
        this.url = url;
        this.exception = exception;
        this.className = className;
        this.lineNumber = lineNumber;
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

}