/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.util.HtmlUtils;

@Component(FormPostView.VIEWNAME)
public class FormPostView extends AbstractView {

    public static final String VIEWNAME = "oauthFormPostView";

    @Override
    protected void renderMergedOutputModel(
        Map<String, Object> model,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        String actionUri = (String) model.get("redirectUri");

        Map<String, String> data = new HashMap<>();
        // we send only registered parameters
        data.put("code", (String) model.get("code"));
        data.put("access_token", (String) model.get("access_token"));
        data.put("refresh_token", (String) model.get("refresh_token"));
        data.put("id_token", (String) model.get("id_token"));
        data.put("expires_in", (String) model.get("expires_in"));
        data.put("token_type", (String) model.get("token_type"));
        data.put("scope", (String) model.get("scope"));
        data.put("state", (String) model.get("state"));
        data.put("nonce", (String) model.get("nonce"));

        data.put("error", (String) model.get("error"));
        data.put("error_description", (String) model.get("error_description"));
        data.put("error_uri", (String) model.get("error_uri"));

        String html = createPostFormData(actionUri, data);

        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.getWriter().write(html);
    }

    private String createPostFormData(String actionUri, Map<String, String> data) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html xmlns:th=\"http://www.thymeleaf.org\">\n");
        html.append("<head>\n");
        html.append("   <meta charset=\"utf-8\" />\n");
        html.append("   <meta http-equiv=\"Cache-Control\" content=\"no-cache, no-store, must-revalidate\" />\n");
        html.append("   <meta http-equiv=\"Pragma\" content=\"no-cache\" />\n");
        html.append("   <meta http-equiv=\"Expires\" content=\"-1\" />\n");
        html.append("   <title>OAuth2 Authorization</title>\n");
        html.append("</head>\n");
        html.append("<body onload=\"document.forms[0].submit()\">\n");
        html.append("   <noscript>\n");
        html.append("      <p>\n");
        html.append("         <strong>Note:</strong> Since your browser does not support JavaScript,\n");
        html.append("         you must press the Continue button once to proceed.\n");
        html.append("      </p>\n");
        html.append("   </noscript>\n");
        html.append("   <form action=\"");
        html.append(actionUri);
        html.append("\" method=\"post\">\n");
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (StringUtils.hasText(e.getValue())) {
                html.append("         <input type=\"hidden\" name=\"").append(e.getKey()).append("\" value=\"");
                html.append(HtmlUtils.htmlEscape(e.getValue())).append("\">\n");
            }
        }
        html.append("      <noscript>\n");
        html.append("         <div>\n");
        html.append("            <input type=\"submit\" value=\"Continue\"/>\n");
        html.append("         </div>\n");
        html.append("      </noscript>\n");
        html.append("   </form>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }
}
