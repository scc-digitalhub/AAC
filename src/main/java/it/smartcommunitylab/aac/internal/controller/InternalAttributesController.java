package it.smartcommunitylab.aac.internal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.dto.AttributesRegistrationDTO;
import it.smartcommunitylab.aac.dto.AttributesRegistrationDTO.AttributeDTO;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.internal.InternalAttributeAuthority;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.model.Realm;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@RequestMapping
public class InternalAttributesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private InternalAttributeAuthority internalAuthority;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private AttributeService attributeService;

    /**
     * Redirect to registration page
     */
    @ApiIgnore
    @RequestMapping(value = "/attrs/internal/edit/{providerId}/{setId}", method = RequestMethod.GET)
    public String registrationPage(
            @PathVariable("providerId") String providerId,
            @PathVariable("setId") String setId,
            Model model) throws NoSuchProviderException, NoSuchRealmException, NoSuchAttributeSetException {

        // resolve provider
        InternalAttributeService as = internalAuthority.getAttributeService(providerId);
        if (as == null) {
            throw new NoSuchProviderException();
        }
        model.addAttribute("providerId", providerId);

        // fetch attribute set and validate
        AttributeSet attributeSet = attributeService.getAttributeSet(setId);
        model.addAttribute("attributeSet", attributeSet);

        // convert to DTO
        AttributesRegistrationDTO reg = AttributesRegistrationDTO.from(attributeSet);

        model.addAttribute("reg", reg);

        String realm = as.getRealm();
        model.addAttribute("realm", realm);

        Realm re = realmManager.getRealm(realm);
        String displayName = re.getName();
        Map<String, String> resources = new HashMap<>();
        if (!realm.equals(SystemKeys.REALM_COMMON)) {
            re = realmManager.getRealm(realm);
            displayName = re.getName();
            CustomizationBean gcb = re.getCustomization("global");
            if (gcb != null) {
                resources.putAll(gcb.getResources());
            }
        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("actionUrl", "/attrs/internal/edit/" + providerId + "/" + setId);

        return "attributes/form";
    }

    /**
     * Update user attributes
     */
    @ApiIgnore
    @RequestMapping(value = "/attrs/internal/edit/{providerId}/{setId}", method = RequestMethod.POST)
    public String register(Model model,
            @PathVariable("providerId") String providerId,
            @PathVariable("setId") String setId,
            @ModelAttribute("reg") @Valid AttributesRegistrationDTO reg,
            BindingResult result,
            HttpServletRequest req) {

        try {
            // resolve provider
            InternalAttributeService as = internalAuthority.getAttributeService(providerId);
            if (as == null) {
                throw new NoSuchProviderException();
            }
            model.addAttribute("providerId", providerId);

            // fetch attribute set and validate
            AttributeSet attributeSet = attributeService.getAttributeSet(setId);
            model.addAttribute("attributeSet", attributeSet);

            AttributesRegistrationDTO dto = AttributesRegistrationDTO.from(attributeSet);
            // extract values sent
            // TODO support multiple values as enum
            Map<String, String> values = new HashMap<>();
            if (reg.getAttributes() != null) {
                for (AttributeDTO ad : reg.getAttributes()) {
                    if (ad.getKey() != null) {
                        values.put(ad.getKey(), ad.getValue());
                    }
                }
            }

            // inflate dto with values
            for (AttributeDTO ad : dto.getAttributes()) {
                ad.setValue(values.get(ad.getKey()));
            }
            model.addAttribute("reg", dto);

            String realm = as.getRealm();
            model.addAttribute("realm", realm);

            Realm re = realmManager.getRealm(realm);
            String displayName = re.getName();
            Map<String, String> resources = new HashMap<>();
            if (!realm.equals(SystemKeys.REALM_COMMON)) {
                re = realmManager.getRealm(realm);
                displayName = re.getName();
                CustomizationBean gcb = re.getCustomization("global");
                if (gcb != null) {
                    resources.putAll(gcb.getResources());
                }
            }

            model.addAttribute("displayName", displayName);
            model.addAttribute("customization", resources);

            // build url
            // TODO handle via urlBuilder or entryPoint
            model.addAttribute("actionUrl", "/attrs/internal/edit/" + providerId + "/" + setId);

            if (result.hasErrors()) {
                model.addAttribute("error", InvalidDataException.ERROR);
                return "attributes/form";
            }

            // save to provider

            // WRONG, should send redirect to success page to avoid double POST
            return "attributes/form";
        } catch (RegistrationException e) {
            String error = e.getError();
            model.addAttribute("error", error);
            return "attributes/form";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("error", RegistrationException.ERROR);
            return "attributes/form";
        }
    }
}
