package it.smartcommunitylab.aac.attributes.controller;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AskAtLoginAttributeFilter;
import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.dto.AttributesRegistration;
import it.smartcommunitylab.aac.internal.InternalAttributeAuthority;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/hook/attributes")
public class AskAtLoginAttributeController {

    public static final String HOOK_BASE = "/hook"; // TODO: eventually move to an appropriate class for shared hook utils
    public static final String HOOK_ACTION = "/attributes";
    public static final String HOOK_ATTRIBUTES_BASE_URI = HOOK_BASE + HOOK_ACTION;
    public static final String HOOK_ATTRIBUTES_FORM_URI = "/edit/{providerId}/{setId}";
    public static final String HOOK_ATTRIBUTES_FORM_FORMAT = "/edit/%s/%s";
    public static final String HOOK_ATTRIBUTES_CANCEL_URI = "/cancel";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private InternalAttributeAuthority internalAuthority;
    private AuthenticationHelper authHelper;
    private AttributeService attributeService;

    //    private UserService userService;

    @Autowired
    public void setInternalAuthority(InternalAttributeAuthority internalAuthority) {
        this.internalAuthority = internalAuthority;
    }

    @Autowired
    public void setAuthHelper(AuthenticationHelper authHelper) {
        this.authHelper = authHelper;
    }

    @Autowired
    public void setAttributeService(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping(HOOK_ATTRIBUTES_FORM_URI)
    public String attributeRegistrationPage(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId,
        Model model
    ) throws Exception {
        InternalAttributeService attributeProvider = internalAuthority.getProvider(providerId); // will fail is attribute provider is not internal
        if (attributeProvider == null) {
            throw new NoSuchProviderException(
                "The selected attribute set is not eligible for user modificationsThe selected attribute provider does not exists in this realm or is not eligible for user modification"
            );
        }
        if (!(attributeProvider.getConfig().getAskAtLogin() && attributeProvider.getConfig().isUsermode())) {
            throw new InvalidDataException("The selected attribute set is not eligible for user modifications");
        }

        // check that the set id is among those handled by that provider
        Set<String> setIdsAssociatedToProvider = attributeProvider.getConfig().getAttributeSets();
        if (!setIdsAssociatedToProvider.contains(setId)) {
            throw new RuntimeException("This attribute provider does not manage the given attribute set");
        }

        UserDetails currentUser = authHelper.getUserDetails();
        if (currentUser == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }
        AttributeSet setDefinition = attributeService.getAttributeSet(setId); // This fetches information from AttributeEntity, hence the value of the attribute is missing (only the definition/properties are known)
        for (Attribute att : setDefinition.getAttributes()) {
            if (att.getType().equals(AttributeType.OBJECT)) {
                throw new InvalidDataException(
                    "Not implemented exception: forms with boolean object attribute type are currently not supported"
                );
            }
        }
        UserAttributes setCurrentValues = attributeProvider.getUserAttributes(currentUser.getSubjectId(), setId); // This fetches information form InternalAttributeEntity, hence only the (key, value) pair are known
        AttributesRegistration newAttributesRegistration = evaluateRegistrationDTOFromInternalAttributes(
            setDefinition,
            setCurrentValues
        );
        newAttributesRegistration.setProvider(providerId);

        prefillBaseModel(model, attributeProvider.getRealm(), providerId, setDefinition);
        model.addAttribute("newAttributesRegistration", newAttributesRegistration);
        return "attributes/hook_form";
    }

    @PostMapping("/edit/{providerId}/{setId}")
    public String registerUpdatedAttributes(
        Model model,
        @PathVariable("providerId") String providerId,
        @PathVariable("setId") String setId,
        @ModelAttribute("newAttributesRegistration") @Valid AttributesRegistration newAttributesRegistration,
        BindingResult result,
        HttpServletRequest req
    ) throws Exception {
        UserDetails currentUser = authHelper.getUserDetails(); // fetch current user from security context
        if (currentUser == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }
        InternalAttributeService attributeProvider = internalAuthority.getProvider(providerId);

        if (!(attributeProvider.getConfig().getAskAtLogin() && attributeProvider.getConfig().isUsermode())) {
            throw new InvalidDataException("The selected attribute set is not eligible for user modifications");
        }

        // Fetch set definition (eventually updated with new set values)
        AttributeSet setToBeUpdated;
        try {
            setToBeUpdated = attributeService.getAttributeSet(setId);
        } catch (NoSuchAttributeSetException e) {
            model.addAttribute("error", "no such service");
            return "attributes/hook_form";
        }
        prefillBaseModel(model, attributeProvider.getRealm(), providerId, setToBeUpdated);

        // Update with new set using the information form the model
        // Will redirect to form filling page with an error message if a required attribute was not filled. We trust the model to obtain mandatory-ness, which might not be appropriate
        Map<String, Serializable> updatedAttributeValues = new HashMap<>();
        if (newAttributesRegistration.getAttributes() != null) {
            for (AttributesRegistration.AttributeRegistration reg : newAttributesRegistration.getAttributes()) {
                String key = reg.getKey();
                Serializable formValue = reg.getValue(); // NOTE: empty values might be parsed as 0-length string from the template engine
                boolean isValueRequired = reg.getIsRequired();
                boolean isCheckbox = reg.getType().equals("checkbox");
                if (key == null) {
                    if (isValueRequired) {
                        String errMsg = "Unexpected state: a missing key was flagged as mandatory";
                        model.addAttribute("error", errMsg);
                        return "attributes/hook_form";
                    }
                    continue;
                }
                if (isValueRequired && ((formValue == "") || (formValue == null)) && !isCheckbox) {
                    String errMsg = "Missing mandatory attribute";
                    model.addAttribute("error", errMsg);
                    return "attributes/hook_form";
                }
                if ((formValue != null) && (formValue != "")) {
                    updatedAttributeValues.put(key, reg.getValue());
                } else if (isCheckbox) {
                    // unfilled values are skipped, unless they are boolean in which case they are interpreted as false due to HTML limitations
                    updatedAttributeValues.put(key, "false");
                }
            }
        }
        // update set with new values
        ExactAttributesMapper mapper = new ExactAttributesMapper(setToBeUpdated);
        AttributeSet updatedAttributeSet = mapper.mapAttributes(updatedAttributeValues);
        if (updatedAttributeSet.getAttributes() == null || updatedAttributeSet.getAttributes().isEmpty()) {
            throw new IllegalArgumentException(
                "empty or invalid attribute set: this error might be caused if the attribute set failed to be evaluated or the given registration form was incomplete"
            );
        }

        // Check that mandatory fields were filled: this should never fail unless the registration from the model doesn't match the set definition
        for (Attribute att : updatedAttributeSet.getAttributes()) {
            if (att.getIsRequired() && (att.getValue() == null || att.getValue().equals(""))) {
                String errMsg = String.format("Attribute with key %s is mandatory but was not provided", att.getKey());
                model.addAttribute("error", errMsg);
                return "attributes/hook_form";
            }
        }
        // update the user attributes
        logger.debug(
            String.format("updating user's %s attributes: updating attribute set %s", currentUser.getSubjectId(), setId)
        );
        attributeProvider.putUserAttributes(currentUser.getSubjectId(), setId, updatedAttributeSet);

        if (req.getAttribute(AskAtLoginAttributeFilter.ATTRIBUTE_SET_STATE) != null) {
            // update previous choice taken by the user
            req.removeAttribute(AskAtLoginAttributeFilter.ATTRIBUTE_SET_STATE);
        }
        req
            .getSession()
            .setAttribute(
                AskAtLoginAttributeFilter.ATTRIBUTE_SET_STATE,
                AskAtLoginAttributeFilter.ATTRIBUTE_SET_COMPLETE
            );
        return "redirect:/";
    }

    @GetMapping(HOOK_ATTRIBUTES_CANCEL_URI)
    public String registrationAttributesCancel(
        @RequestParam(name = "providerId", required = false) String providerId,
        @RequestParam(name = "setId", required = false) String setId,
        Model model
    ) throws Exception {
        UserDetails currentUser = authHelper.getUserDetails(); // fetch current user from security context
        if (currentUser == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user"); // TODO: handle this exception properly
        }
        if (StringUtils.hasText("providerId")) {
            model.addAttribute("providerId", providerId);
        } else {
            model.addAttribute("providerId", "");
        }
        if (StringUtils.hasText("setId")) {
            model.addAttribute("setId", setId);
        } else {
            model.addAttribute("setId", "");
        }
        return "attributes/hook_cancel";
    }

    @PostMapping(HOOK_ATTRIBUTES_CANCEL_URI)
    public String registrationAttributesCancelConfirm(
        @RequestParam(required = false, defaultValue = "undo") String action,
        @ModelAttribute("providerId") String providerId,
        @ModelAttribute("setId") String setId,
        HttpServletRequest req
    ) throws Exception {
        UserDetails currentUser = authHelper.getUserDetails(); // fetch current user from security context
        if (currentUser == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }
        if (action.equals("undo")) {
            // TODO: use Uri Builder
            String redirectUrl = "/";
            if (StringUtils.hasText(providerId) && StringUtils.hasText(setId)) {
                redirectUrl = HOOK_ATTRIBUTES_BASE_URI + String.format(HOOK_ATTRIBUTES_FORM_FORMAT, providerId, setId);
            }
            return "redirect:" + redirectUrl;
        }
        // In general this flow is reached when action.equals("logout"), but we assign this as default behaviour for any value other than "continue"
        if (req.getAttribute(AskAtLoginAttributeFilter.ATTRIBUTE_SET_STATE) != null) {
            req.removeAttribute(AskAtLoginAttributeFilter.ATTRIBUTE_SET_STATE);
        }
        req
            .getSession()
            .setAttribute(
                AskAtLoginAttributeFilter.ATTRIBUTE_SET_STATE,
                AskAtLoginAttributeFilter.ATTRIBUTE_SET_CANCELED
            );
        return "redirect:/logout";
    }

    private void prefillBaseModel(Model model, String realmId, String providerId, AttributeSet setDefinition) {
        // prefill model with form information that is not associated to user inputs
        model.addAttribute("displayName", realmId);
        model.addAttribute("setName", setDefinition.getName());
        model.addAttribute("setDescription", setDefinition.getDescription());
        // TODO handle via urlBuilder
        model.addAttribute(
            "formSubmissionUrl",
            HOOK_ATTRIBUTES_BASE_URI + String.format("/edit/%s/%s", providerId, setDefinition.getIdentifier())
        );
        String formCancelUrl;
        try {
            URIBuilder cancelUrlBuilder = new URIBuilder(HOOK_ATTRIBUTES_BASE_URI + HOOK_ATTRIBUTES_CANCEL_URI);
            cancelUrlBuilder.addParameter("providerId", providerId);
            cancelUrlBuilder.addParameter("setId", setDefinition.getIdentifier());
            formCancelUrl = cancelUrlBuilder.build().toString();
        } catch (URISyntaxException e) {
            formCancelUrl = HOOK_ATTRIBUTES_BASE_URI + HOOK_ATTRIBUTES_CANCEL_URI;
        }
        model.addAttribute("formCancellationUrl", formCancelUrl);
    }

    /**
     * Creates a DTO object for the Form using the attribute set definition and prefilling it with currently known
     * values (when possible).
     * @param attributeSetDefinition The attribute set definition as obtain from AttributeEntity table.
     * @param attributeSetValues The attribute set values as obtained from InternalAttributeEntity table.
     * @return The registration, where attribute values are filled when known.
     */
    private AttributesRegistration evaluateRegistrationDTOFromInternalAttributes(
        AttributeSet attributeSetDefinition,
        UserAttributes attributeSetValues
    ) {
        // create DTO template
        AttributesRegistration newRegistration = AttributesRegistration.from(attributeSetDefinition);
        // fill DTO with currently known values
        Map<String, Serializable> knownValues = new HashMap<>();
        for (Attribute attr : attributeSetValues.getAttributes()) {
            String key = attr.getKey();
            if (key != null) {
                knownValues.put(attr.getKey(), attr.getValue());
            }
        }
        for (AttributesRegistration.AttributeRegistration reg : newRegistration.getAttributes()) {
            String key = reg.getKey();
            if ((key != null) && (knownValues.get(key) != null)) {
                reg.setValue(knownValues.get(key));
            }
        }
        return newRegistration;
    }
}
