async function performRegistration() {
    const username = document.getElementById("username").value
    var displayName = document.getElementById("displayName").value
    if (displayName == null || displayName.length == 0) {
        displayName = username
    }
    const startRegistrationResp = await fetch(
        `/auth/webauthn/attestationOptions/${providerId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(
                {
                    "username": username,
                    "displayName": displayName
                }
            ),
        }
    );
    if (startRegistrationResp.status != 200) {
        alert("Could not register this username");
        return;
    }
    const credentialCreateJson = await startRegistrationResp.json()
    const decodedChallenge = base64URLStringToBuffer(credentialCreateJson.publicKey.challenge);
    const credentialCreateOptions = {
        publicKey: {
            ...credentialCreateJson.publicKey,
            challenge: decodedChallenge,
            user: {
                ...credentialCreateJson.publicKey.user,
                id: base64URLStringToBuffer(credentialCreateJson.publicKey.user.id),
            },
            excludeCredentials: credentialCreateJson.publicKey.excludeCredentials.map(credential => ({
                ...credential,
                id: base64URLStringToBuffer(credential.id),
            })),
            // Warning: Extension inputs could also contain binary data that needs encoding
            extensions: credentialCreateJson.publicKey.extensions,
        },
    }

    const publicKeyCredential = await navigator.credentials.create(credentialCreateOptions)
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
    const finalResult = await fetch(
        `/auth/webauthn/assertions/${providerId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                "attestation": payload,
            }),
        }
    );
    if (finalResult.status == 200) {
        alert("Account created successfully")
        window.location.href = "/index.html"
    } else {
        alert("Error while creating the account")
    }
}

const providerId = getProviderId()
const form = document.getElementById("registrationForm")
form.addEventListener("submit", (e) => {
    e.preventDefault()
    performRegistration()
});
