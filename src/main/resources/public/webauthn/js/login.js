async function performAuthentication() {
    const username = document.getElementById("username").value
    const starLoginResp = await fetch(
        `/auth/webauthn/assertionOptions/${providerId}`,
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
    const credentialGetJson = await starLoginResp.json()
    const credentialGetOptions = {
        publicKey: {
            ...credentialGetJson.publicKey,
            allowCredentials: credentialGetJson.publicKey.allowCredentials
                && credentialGetJson.publicKey.allowCredentials.map(credential => ({
                    ...credential,
                    id: base64URLStringToBuffer(credential.id),
                })),

            challenge: base64URLStringToBuffer(credentialGetJson.publicKey.challenge),

            // Warning: Extension inputs could also contain binary data that needs encoding
            extensions: credentialGetJson.publicKey.extensions,
        },
    }
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
    const finishLoginResp = await fetch(
        `/auth/webauthn/assertions/${providerId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(
                {
                    "assertion": authenticatorReponse
                }
            ),
        }
    );
    if (finishLoginResp.status == 200) {
        alert(await finishLoginResp.text());
        window.location.href = "/index.html"
    } else {
        alert(finishLoginResp.status, await finishLoginResp.text())
    }
}

const providerId = getProviderId()
const form = document.getElementById("loginForm")
form.addEventListener("submit", (e) => {
    e.preventDefault()
    performAuthentication()
});