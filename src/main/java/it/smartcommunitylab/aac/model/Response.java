/**
 *    Copyright 2012-2013 Trento RISE
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package it.smartcommunitylab.aac.model;

/**
 * ServiceDescriptor response descriptor: response code (OK/ERROR) contained data and optional error message
 * @author raman
 *
 */
public class Response {

	public enum RESPONSE {OK,ERROR};
	
	private Object data;
	private RESPONSE responseCode;
	private String errorMessage;
	
	public static Response ok(Object data) {
		Response res = new Response();
		res.setResponseCode(RESPONSE.OK);
		res.setData(data);
		return res;
	}
	
	public static Response error(String message) {
		Response res = new Response();
		res.setResponseCode(RESPONSE.ERROR);
		res.setErrorMessage(message);
		return res;
	}

	
	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}
	/**
	 * @param data the data to set
	 */
	public void setData(Object data) {
		this.data = data;
	}
	
	/**
	 * @return the responseCode
	 */
	public String getResponseCode() {
		return responseCode.toString();
	}
	/**
	 * @param responseCode the responseCode to set
	 */
	public void setResponseCode(RESPONSE responseCode) {
		this.responseCode = responseCode;
	}
	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
