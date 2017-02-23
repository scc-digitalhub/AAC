/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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
 ******************************************************************************/

package it.smartcommunitylab.aac.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author raman
 *
 */
@Component
class GlobalDefaultExceptionHandler implements HandlerExceptionResolver, Ordered {
    public static final String DEFAULT_ERROR_VIEW = "error";

	    public int getOrder() {
	        return Integer.MIN_VALUE;
	     }

	    public ModelAndView resolveException(
	        HttpServletRequest aReq, HttpServletResponse aRes,
	        Object aHandler, Exception anExc
	    ) {
	    	// hack. should be done better in configuration	
	    	if ("/oauth/token".equals(aReq.getServletPath())) {
	    		return null;
	    	}
	        // Otherwise setup and send the user to a default error-view.
	        ModelAndView mav = new ModelAndView();
	        mav.addObject("exception", anExc);
	        mav.addObject("url", aReq.getRequestURL());
	        mav.setViewName(DEFAULT_ERROR_VIEW);
	        return mav;
	 	    }
	}
