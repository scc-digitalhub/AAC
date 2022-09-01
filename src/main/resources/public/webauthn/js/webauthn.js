/*
* registration
*/
async function requestAttestationOptions(providerId, username, displayName) {
    console.log("fetch attestation options for " + providerId + " user " + username);
    const response = await fetch(`/auth/webauthn/attestationOptions/${providerId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(
                {
                    "provider": providerId,
                    "username": username,
                    "displayName": displayName
                }
            ),
        }
    );

    if (!response.ok) {
        throw Error(response.statusText);
    }

    return response.json();
}

async function webauthnRegister(form) {
    const providerId = form.elements.provider.value;
    const username = form.elements.username.value;
    var displayName = form.elements.displayName.value;

    if (!username) {
        return;
    }

    if (displayName == null || displayName.length == 0) {
        displayName = username
    }

    const msg = document.getElementById("webauthn_error_" + providerId);
    if (msg) {
        msg.style = "display:none;";
    }

    try {
        console.log("start registration");
        //fetch credential options
        const optionsAndRegistrationKey = await requestAttestationOptions(providerId, username, displayName);
        const registrationKey = optionsAndRegistrationKey.key;
        const credentialCreationOptions = optionsAndRegistrationKey.options;


        //build 
        const decodedChallenge = base64URLStringToBuffer(credentialCreationOptions.publicKey.challenge);
        const credentialCreateOptions = {
            publicKey: {
                ...credentialCreationOptions.publicKey,
                challenge: decodedChallenge,
                user: {
                    ...credentialCreationOptions.publicKey.user,
                    id: base64URLStringToBuffer(credentialCreationOptions.publicKey.user.id),
                },
                excludeCredentials: credentialCreationOptions.publicKey.excludeCredentials.map(credential => ({
                    ...credential,
                    id: base64URLStringToBuffer(credential.id),
                })),
                // Warning: Extension inputs could also contain binary data that needs encoding
                extensions: credentialCreationOptions.publicKey.extensions,
            },
        }

        console.log(credentialCreateOptions);

        //call browser to load credentials
        const publicKeyCredential = await navigator.credentials.create(credentialCreateOptions)
        console.log(publicKeyCredential);

        if (!publicKeyCredential) {
            throw Error("invalid credential");
        }

        //build registration response
        const payload = {
            type: publicKeyCredential.type,
            id: publicKeyCredential.id,
            response: {
                attestationObject: bufferToBase64URLString(publicKeyCredential.response.attestationObject),
                clientDataJSON: bufferToBase64URLString(publicKeyCredential.response.clientDataJSON),
                transports: publicKeyCredential.response.getTransports && publicKeyCredential.response.getTransports() || [],
            },
            clientExtensionResults: publicKeyCredential.getClientExtensionResults(),
        }

        console.log(payload);
        
          //update form and submit
        form.action = form.action+ '/'+ registrationKey;
        form.elements.attestation.value = JSON.stringify(payload);
        form.submit();
    } catch (e) {
        console.log(e);
        if (msg) {
            msg.innerHTML = e;
            msg.style = "display: block;";
        }
    }

    console.log("end");
    return false;
}

/*
* login
*/

async function requestAssertionOptions(providerId, username) {
    console.log("fetch assertion options for " + providerId + " user " + username);
    const response = await fetch(`/auth/webauthn/assertionOptions/${providerId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(
                {
                    "username": username
                }
            ),
        }
    );

    if (!response.ok) {
        throw Error(response.statusText);
    }

    return response.json();
}



async function webauthnLogin(form) {
    const providerId = form.elements.provider.value;
    const username = form.elements.username.value;
    if (!username) {
        return;
    }

    const msg = document.getElementById("webauthn_error_" + providerId);
    if (msg) {
        msg.style = "display:none;";
    }

    try {
        //fetch options and key
        const assertionRequestAndKey = await requestAssertionOptions(providerId, username);
        console.log(assertionRequestAndKey);

        const { key, assertionRequest } = assertionRequestAndKey;


        //build credentials options
        const credentialGetOptions = {
            publicKey: {
                ...assertionRequest.publicKey,
                allowCredentials: assertionRequest.publicKey.allowCredentials
                    && assertionRequest.publicKey.allowCredentials.map(credential => ({
                        ...credential,
                        id: base64URLStringToBuffer(credential.id),
                    })),

                challenge: base64URLStringToBuffer(assertionRequest.publicKey.challenge),

                // Warning: Extension inputs could also contain binary data that needs encoding
                extensions: assertionRequest.publicKey.extensions,
            },
        }

        console.log(credentialGetOptions)

        //fetch credentials from browser    
        const publicKeyCredential = await navigator.credentials.get(credentialGetOptions)
        const authenticatorReponse = {
            type: publicKeyCredential.type,
            id: publicKeyCredential.id,
            response: {
                authenticatorData: bufferToBase64URLString(publicKeyCredential.response.authenticatorData),
                clientDataJSON: bufferToBase64URLString(publicKeyCredential.response.clientDataJSON),
                signature: bufferToBase64URLString(publicKeyCredential.response.signature),
                userHandle: publicKeyCredential.response.userHandle && bufferToBase64URLString(publicKeyCredential.response.userHandle),
            },
            // Warning: Client extension results could also contain binary data that needs encoding
            clientExtensionResults: publicKeyCredential.getClientExtensionResults(),
        }

        //update form and submit
        form.elements.key.value = key;
        form.elements.assertion.value = JSON.stringify(authenticatorReponse);
        form.submit();
    } catch (e) {
        console.log(e);
        if (msg) {
            msg.innerHTML = e;
            msg.style = "display: block;";
        }
    }

    console.log("end");
}


/*
* static utils
*/
function base64URLStringToBuffer(base64URLString) {//https://git.io/J1zgG
    const base64 = base64URLString.replace(/-/g, '+').replace(/_/g, '/');
    const padLength = (4 - (base64.length % 4)) % 4;
    const padded = base64.padEnd(base64.length + padLength, '=');
    const binary = window.atob(padded);
    const buffer = new ArrayBuffer(binary.length);
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return buffer;
}

function bufferToBase64URLString(buffer) {//https://git.io/J1zgb
    const bytes = new Uint8Array(buffer);
    let str = '';
    for (const charCode of bytes) {
        str += String.fromCharCode(charCode);
    }
    const base64String = window.btoa(str);
    return base64String.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}


